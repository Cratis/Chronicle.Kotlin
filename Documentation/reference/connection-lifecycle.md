---
title: Connection lifecycle
description: What the JVM client does when it connects, checks compatibility, registers artifacts, loses the kernel and reconnects, and where each failure shows up.
---

Most connection problems look the same from the outside: a call that throws,
or a call that never returns. This page lists what the Kotlin and Java client
does at each stage of a connection, and what you see when a stage fails.
`ChronicleClient` exposes suspending operations; `BlockingChronicleClient`
wraps them for Java. The Spring Boot starter uses these clients and bounds its
startup registration wait as described below.

## Creating the client

Before creating a client for a remote kernel, set `skipTlsValidation=false`
and ensure the JVM trusts the server's certificate; see
[TLS and authentication](configuration.md#tls-and-authentication). This matters
before dialing because connection setup sends the configured credentials.

`ChronicleClient(options)` and `BlockingChronicleClient.connect(options)` dial
the kernel before they return. Dialing:

1. Resolves the address. A `chronicle+srv://` connection string is looked up
   in DNS; a list of hosts is narrowed to one by the `loadBalancer` policy.
2. Fetches an access token from the kernel's `/connect/token` endpoint with the
   connection string's user name and password (the development credentials
   when none are given). A failed token request is written to standard error
   but does not fail the constructor; the failure shows up later. A connection
   string with an `apiKey` option is rejected before this step with
   `IllegalArgumentException`, because the kernel has no API key
   authentication.
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

Credentials are not checked at this point. With wrong credentials the client
is created without error; the kernel then refuses the connection, and the
first append or `awaitRegistration()` throws `ChronicleConnectionFailed`. See
[Rejected connections](#rejected-connections).

In a Spring Boot application the client is a bean, so an unreachable kernel at
startup fails the application context with `APPLICATION FAILED TO START`.

## Compatibility

There is no published compatibility matrix between client and kernel versions.
The compatibility check at connect time is what enforces it: an incompatible
pairing fails before anything is sent.

A contracts dependency version is not a minimum supported kernel version.
Check the requirements of the features you use and test the client/kernel
pairing you intend to deploy. A successful compatibility check does not replace
that application testing. Do not infer support for an untested pairing from
an older example or from the version of the contracts package.

## Staying connected

Once connected, the client holds a connection stream open. The kernel sends a
keep-alive every second and the client answers each one. If no keep-alive
arrives for 5 seconds, or the stream fails, the client treats the connection as
lost and reconnects:

- It waits with exponential backoff, starting at about 1 second and capped at
  30 seconds, with jitter so a fleet of clients does not return all at once.
- The wait only starts over once a connection has stayed up for 30 seconds.
  A connection the kernel drops right after it was established keeps backing
  off, so the client does not reconnect, and register everything again, once
  a second.
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

## Re-establishing reactor and reducer streams

Each reactor and reducer holds its own observation stream. When that stream
ends while the connection stays up, the observer opens it again on its own,
after a wait:

- The first wait is between 1 and 2 seconds.
- Each stream that ends without having worked doubles the wait, up to 60
  seconds, so a kernel that is still busy is not asked again at the same pace.
- Every wait lands at random between half and all of that ceiling. Reactors
  and reducers whose streams failed together come back spread out, not as a
  herd.
- The wait starts over at 1–2 seconds once a stream has worked: it
  delivered something, or it stayed open for 3 minutes. A stream the kernel
  closes after a long life comes back quickly.
- The growth carries across reconnects. A new connection opens the stream at
  once, but if that stream also ends without working, the next wait continues
  from where it was.

These waits are not configurable.

A stream can also fail because the kernel no longer knows the client's
connection, for example after the kernel restarted. Retrying under the same
connection id cannot work, so the observer does not retry. It asks the client
to reconnect instead: the client ends the connection, reconnects with a new
connection id, and every reactor and reducer registers again under it. Many
observers that hit this together cause one reconnect, not one each. That
reconnect waits with the same jittered connection backoff described in
[Staying connected](#staying-connected), and the client writes:

```text
[Chronicle] Kernel no longer knows connection <connection-id>; reconnecting
```

A failed stream is written to standard error with the observer's id:

```text
[ReactorsService] 'my-reactor' failed: UNAVAILABLE: io exception
```

## Registration and the first append

With automatic registration on (the default), the client registers every
discovered artifact each time a connection is established. See
[Artifact Registration](../guides/artifact-registration.md) for the order.

The first append on an event store waits for the first registration attempt.
`awaitRegistration()` waits for that same attempt, so you can put the wait at
startup instead of on the first request. It does not prove that an event type
or any other artifact was registered successfully.

A pass that throws still releases the registration wait. Check standard error
for the failure and inspect the append's `AppendResult`; a normal return from
`awaitRegistration()` is not an acceptance check. Early registration failures
stop the pass and produce the generic automatic-registration diagnostic.
Reactor and reducer start failures are collected while the remaining observers,
captures and seeders are attempted. If those later steps finish, automatic
registration reports each failed observer on its own line. See
[Artifact registration](../guides/artifact-registration.md#when-registration-fails)
for both diagnostic forms, retry behavior and cancellation limits. A rejected
connection can instead make the waiting calls throw, as described next.

## Rejected connections

When the kernel refuses the connection, because the credentials are wrong
(`UNAUTHENTICATED`), the client is not permitted (`PERMISSION_DENIED`), or the
server turns out to be incompatible on a reconnect, the waiting calls do not
hang. `awaitRegistration()` and the first append throw
`io.cratis.chronicle.connection.ChronicleConnectionFailed`, whose message
names the cause. From Java, the blocking calls throw the same exception.

The client keeps retrying in the background. Once the kernel accepts a
connection, for example after the credentials are corrected on the server,
later calls go through normally.

## Bound the first call

Transient failures are different: when the kernel is unreachable or stops
answering after the client was created, the client keeps retrying, and the
first append and `awaitRegistration()` wait for it. The registration wait has
no time limit of its own. Put a limit on it at startup and fail with a message
that points at the cause:

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
for a bounded wait. A passive read model
([`@Passive`](annotations.md#passive), or a reducer with `isActive = false`)
is not stored: it is computed from the events on each read, so it includes
the event you just appended. See the shared
[read models](/chronicle/read-models/) documentation for designing around it.

## Disposing

Keep the client for the application's lifetime, rather than creating and
disposing one for each append. Call `dispose()` (or `close()`, or use
try-with-resources on `BlockingChronicleClient`) when the application stops.
It stops reconnecting, closes the connection, and gives in-flight calls up to
5 seconds to finish.
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

**The first append or `awaitRegistration()` throws `ChronicleConnectionFailed`.**
The kernel refused the connection. The message says why: `UNAUTHENTICATED`
means the client id or secret is wrong.

**The first append or `awaitRegistration()` never returns.** The kernel is not
reachable, and the client is still retrying. Look for `Connection lost` on
stderr, and bound the wait as shown above.

**Reactors stopped firing.** The connection was lost and is not yet back. Look
for `Connection lost` on stderr; the client reconnects on its own once the
kernel is reachable.

**`Automatic registration of artifacts failed` on stderr.** An artifact is
invalid or could not be created. Fix the artifact named in the message; see
[Artifact Registration](../guides/artifact-registration.md#when-registration-fails).

**A read model is `null` right after appending.** It has not caught up yet.
That is expected; see [Reading after writing](#reading-after-writing).
