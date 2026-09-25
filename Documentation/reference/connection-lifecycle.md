---
title: Connection lifecycle
description: What the JVM client does when it connects, checks compatibility, registers artifacts, loses the kernel and reconnects, and where each failure shows up.
---

Most connection problems look the same from the outside: a call that throws,
or a call that never returns. This page lists what the Kotlin and Java client
does at each stage of a connection, and what you see when a stage fails. All
of it applies equally to `ChronicleClient`, `BlockingChronicleClient` and the
Spring Boot starter, which is built on them. It describes
`io.cratis:chronicle` 6.4.0.

## Creating the client

`ChronicleClient(options)` and `BlockingChronicleClient.connect(options)` dial
the kernel before they return. Dialing:

1. Resolves the address. A `chronicle+srv://` connection string is looked up
   in DNS; a list of hosts is narrowed to one by the `loadBalancer` policy.
2. Fetches an access token from the kernel's `/connect/token` endpoint with the
   connection string's user name and password (the development credentials
   when none are given). A failed token request is written to standard error
   but does not fail the constructor; the failure shows up later. With an
   `apiKey` option it skips this step; see
   [Configuration](configuration.md#tls-and-authentication) for why that
   currently means sending no credentials.
3. Runs a compatibility check: the client sends its client type (`Kotlin`),
   its version and the contract descriptors it was built with, and the kernel
   answers whether it can serve them. The check has a 10-second deadline.

If the address cannot be reached or the compatibility check fails, the
constructor throws and no operation is sent. There is no lazy connect.

- **Nothing listening, or the wrong host or port:**
  `io.grpc.StatusException: UNAVAILABLE`.
- **`skipTlsValidation=false` and a certificate the JVM does not trust:**
  `io.grpc.StatusException: UNAVAILABLE`, with
  `Failed to fetch OAuth2 token: … PKIX path building failed` on stderr.
- **The kernel rejects the client's contracts:** `IllegalStateException` with
  the message
  `Chronicle server <version> is incompatible: <reasons>. No operations were sent.`

Credentials are not checked at this point: with wrong credentials, or when the
token request fails for another reason, the client is created without error
and then never manages to connect. See
[Bound the first call](#bound-the-first-call).

In a Spring Boot application the client is a bean, so an unreachable kernel at
startup fails the application context with `APPLICATION FAILED TO START`.

## Compatibility

There is no published compatibility matrix between client and kernel versions.
The compatibility check at connect time is what enforces it: an incompatible
pairing fails before anything is sent.

What has been checked for this page: client `io.cratis:chronicle` 6.4.0
against kernel image `cratis/chronicle:19.4.8-development`. The client
connects, appends, and runs reactors and reducers, and reducer read models
can be read back after a delay.

Client 6.4.0 depends on `io.cratis:chronicle-contracts` 19.4.0. That is a
build dependency, not a statement about which kernel versions work. Treat any
pairing not listed above as unverified until the compatibility check and your
own tests pass against it.

## Staying connected

Once connected, the client holds a connection stream open. The kernel sends a
keep-alive every second and the client answers each one. If no keep-alive
arrives for 5 seconds, or the stream fails, the client treats the connection as
lost and reconnects:

- It waits with exponential backoff, starting at about 1 second and capped at
  30 seconds, with jitter so a fleet of clients does not return all at once.
- It retries forever, until you dispose the client.
- Every attempt dials from scratch: DNS and SRV are resolved again, a host is
  picked again, a new token is fetched and the compatibility check runs again.
- Every reconnect uses a new connection id. The kernel drops a client it stops
  hearing from, together with that client's observers.
- After each reconnect, the client registers every discovered artifact again
  and each reactor and reducer re-establishes its observation. A kernel that
  restarted and forgot everything is told it all again.

While the connection is down, reactors and reducers receive nothing, and calls
that need the kernel fail or wait. Appends that fail throw a gRPC
`StatusException`.

The client writes connection problems to standard error, not to a logging
framework. Look for lines such as:

```text
[Chronicle] Connection lost: UNAVAILABLE: io exception
[Chronicle] Connection lost: UNAUTHENTICATED: HTTP status code 401
[Chronicle] Failed to fetch OAuth2 token: Token request failed with status 401
```

## Registration and the first append

With automatic registration on (the default), the client registers every
discovered artifact each time a connection is established. See
[Artifact Registration](../guides/artifact-registration.md) for the order.

The first append on an event store waits until the first registration pass has
run, so it cannot reach a kernel that does not know its event type yet.
`awaitRegistration()` waits for the same thing, so you can put that wait at
startup instead of on the first request.

Neither guarantees that registration succeeded. A pass that throws still lets
the waiting calls through, so an append fails with a clear error instead of
hanging. The failure is written to standard error as
`[EventStore] Automatic registration of artifacts failed: <message>`, and the
pass is tried again on the next reconnect.

## Bound the first call

The registration wait has no time limit of its own. If the client never
establishes a connection, because the credentials are wrong or the kernel went
away right after the compatibility check, the first append and
`awaitRegistration()` wait indefinitely while the client keeps retrying. Put a
limit on that wait at startup and fail with a message that points at the
cause:

<!-- validate: body needs=store -->

```kotlin
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

try {
    withTimeout(30_000) { store.awaitRegistration() }
} catch (timeout: TimeoutCancellationException) {
    throw IllegalStateException(
        "Chronicle was not ready within 30 seconds; check stderr for the cause",
        timeout
    )
}
```

Java has no cancellable wait, so run the blocking call on another thread and
stop waiting for it:

<!-- validate: body needs=blockingStore -->

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

try {
    CompletableFuture.runAsync(store::awaitRegistration).get(30, TimeUnit.SECONDS);
} catch (TimeoutException timeout) {
    throw new IllegalStateException(
        "Chronicle was not ready within 30 seconds; check stderr for the cause",
        timeout);
}
```

The thread running `awaitRegistration` stays blocked until the client
connects; disposing the client does not release it. That is acceptable at
startup, where the usual response to the timeout is to stop the process.

The Spring Boot starter bounds this wait for you with
`cratis.chronicle.registration-timeout`; see
[Spring Boot](../guides/spring-boot.md#startup).

## Reading after writing

A successful append means the event is stored in the event log. Reactors,
reducers and projections process it afterwards, and a materialized read model
is written to its store after that. A read immediately after the append can
therefore return `null` or the previous state. See
[step 8 of Get started](../get-started/index.md#8-query-a-read-model-by-key)
for a bounded wait, and the shared
[read models](/chronicle/read-models/) documentation for designing around it.

## Disposing

Call `dispose()` (or `close()`, or use try-with-resources on
`BlockingChronicleClient`) when the application stops. It stops reconnecting,
closes the connection, and gives in-flight calls up to 5 seconds to finish.
Reactors and reducers that were observing report
`failed: UNAVAILABLE: Channel shutdownNow invoked` on standard error as they
stop; during shutdown that line is expected.

## Troubleshooting

**The constructor throws `UNAVAILABLE`.** The kernel is not reachable, or the
TLS settings do not match it. Check `curl -sk https://<host>:<port>/health`,
and check `disableTls` and `skipTlsValidation` in
[Configuration](configuration.md).

**The constructor throws `… is incompatible …`.** The client and kernel
versions cannot work together. Upgrade one of them; the message lists what
does not match.

**The first append or `awaitRegistration()` never returns.** No connection is
ever established, most often because the credentials are wrong. Look for
`UNAUTHENTICATED` on stderr, and bound the wait as shown above.

**Reactors stopped firing.** The connection was lost and is not yet back. Look
for `Connection lost` on stderr; the client reconnects on its own once the
kernel is reachable.

**`Automatic registration of artifacts failed` on stderr.** An artifact is
invalid or could not be created. Fix the artifact named in the message; see
[Artifact Registration](../guides/artifact-registration.md#when-registration-fails).

**A read model is `null` right after appending.** It has not caught up yet.
That is expected; see [Reading after writing](#reading-after-writing).
