---
title: Configuration
description: Every ChronicleOptions property, the chronicle:// connection string format and its options, TLS and authentication, and namespaces for the JVM client.
---

## ChronicleOptions

<!-- validate: skip -->

```kotlin
data class ChronicleOptions(
    val connectionString: ChronicleConnectionString,
    val programIdentifier: String = "Unknown",
    val defaultSinkTypeId: String = System.getenv("CHRONICLE_SINK_TYPE") ?: WellKnownSinkTypes.MONGODB,
    val autoDiscoverAndRegister: Boolean = true,
    val artifacts: IClientArtifacts = ClientArtifacts.default,
    val artifactActivator: IArtifactActivator = ArtifactActivator,
    val openTelemetry: OpenTelemetry? = null,
    val maxReceiveMessageSize: Int? = DEFAULT_MAX_MESSAGE_SIZE
)
```

| Property | Default | Description |
| --- | --- | --- |
| `connectionString` | *(required)* | Parsed address of the server |
| `programIdentifier` | `"Unknown"` | Name of the connecting program |
| `defaultSinkTypeId` | `MongoDB` | Sink for reducers and projections |
| `autoDiscoverAndRegister` | `true` | Register artifacts on connect |
| `artifacts` | `ClientArtifacts.default` | What the application is made of |
| `artifactActivator` | `ArtifactActivator` | How artifacts are created |
| `openTelemetry` | `null` | Where spans go — see [Tracing](../guides/tracing.md) |
| `maxReceiveMessageSize` | `100 MiB` | Receive limit (bytes) |
| `readModelNamingPolicy` | `DefaultReadModelNamingPolicy` | Names each read model's container — see [Read model container names](#read-model-container-names) |
| `multiplexObservation` | `true` | One stream for all observers, see below |
| `defaultGraphSinkTypeId` | `Neo4j` | Sink for [graph projections](../guides/graph-projections.md), set with `withDefaultGraphSinkTypeId` |

`programIdentifier` is a human-readable label that shows up in diagnostics. When
you set it, it is also who the client registers projections as: the client then
waits until the kernel has applied what it accepted, and fails with
`ProjectionRegistrationNotApplied` if the kernel could not apply it. Left at
`"Unknown"`, a registration is sent without a registrant and is not waited for.
`defaultSinkTypeId` defaults to `WellKnownSinkTypes.MONGODB`, and can be
overridden per process with the `CHRONICLE_SINK_TYPE` environment variable
— for example `CHRONICLE_SINK_TYPE=SQL`.

`autoDiscoverAndRegister`, `artifacts` and `artifactActivator` control artifact
discovery and registration. Two helpers cover the common adjustments — see
[Artifact Registration](../guides/artifact-registration.md) for the full picture:

<!-- validate: body -->

```kotlin
ChronicleOptions.development().withoutAutoRegistration()
ChronicleOptions.development().withArtifactsFrom("com.acme.ordering")
```

`readModelNamingPolicy`, `defaultGraphSinkTypeId` and `multiplexObservation`
are not constructor parameters. Set them with `withReadModelNamingPolicy`,
described below, `withDefaultGraphSinkTypeId(sinkTypeId)` and
`withMultiplexObservation(enabled)`. `copy` does not carry them over,
so set them again after copying. `defaultGraphSinkTypeId` is the sink graph
projections are stored in: `WellKnownSinkTypes.NEO4J` unless set, or
`WellKnownSinkTypes.IN_MEMORY_GRAPH` for development and tests. See
[Graph projections](../guides/graph-projections.md).

`maxReceiveMessageSize` defaults to `100 * 1024 * 1024` bytes (100 MiB).
Set it in the constructor or with `withMaxReceiveMessageSize(bytes)`.
Passing `null` leaves grpc-java's own receive limit in effect. This is a
received-message limit, not an event-schema or append-payload validation rule.

`multiplexObservation` is on by default. A kernel that supports it serves every
reactor and reducer of a connection over one stream instead of one stream each,
which keeps a program with many observers well inside the limit on concurrent
HTTP/2 streams. The client uses the shared stream only when the kernel says it
supports it, so an older kernel keeps one stream per observer. Turn it off with
`withMultiplexObservation(false)` to keep one stream per observer against a
kernel that supports sharing.

Use `development()` only for a kernel on your own machine. Before connecting
to a remote kernel, set `skipTlsValidation=false` and ensure the JVM trusts
the server's certificate. Supply your client id and secret securely; see
[TLS and authentication](#tls-and-authentication) for the requirements and
parser limitations.

There are two factories on the companion object:

<!-- validate: body -->

```kotlin
ChronicleOptions.fromConnectionString(
    "chronicle://chronicle.internal:35000?skipTlsValidation=false"
)
ChronicleOptions.development()
```

Both are `@JvmStatic`, so Java calls them as ordinary static methods:

<!-- validate: body -->

```java
ChronicleOptions.fromConnectionString(
    "chronicle://chronicle.internal:35000?skipTlsValidation=false");
ChronicleOptions.development();
```

## Read model container names

Every read model is stored in a container: a MongoDB collection or a SQL
table. By default the client names it after the read model's identifier — the
`@ReadModel(id = ...)` value, or the class simple name. A `Customer` read model
is stored in a container called `Customer`.

That is not always the name another layer expects. Arc's MongoDB naming policy,
for example, pluralizes, so it reads `Customers` while the client writes
`Customer`. A `ReadModelNamingPolicy` decides the container name instead:

<!-- validate: skip -->

```kotlin
fun interface ReadModelNamingPolicy {
    fun getReadModelName(readModelClass: Class<*>): String
}
```

Set one on the options. The policy names only the container. The read model
identifier, and everything keyed on it — projections, reducers, observers, and
the queries that read a model back — stays as it was.

<!-- validate: body -->

```kotlin
import io.cratis.chronicle.readModels.ReadModelNamingPolicy

val options = ChronicleOptions.development()
    .withReadModelNamingPolicy(
        ReadModelNamingPolicy { "${it.simpleName}s" }
    )
```

<!-- validate: body -->

```java
import io.cratis.chronicle.ChronicleOptions;

var options = ChronicleOptions.development()
    .withReadModelNamingPolicy(
        readModelClass -> readModelClass.getSimpleName() + "s");
```

A policy that already exists as an object, such as Arc's, plugs in by
delegating to it from the lambda. In a Spring Boot application, declare a
`ReadModelNamingPolicy` bean and the starter uses it — see
[Spring Boot](../guides/spring-boot.md#replacing-what-the-starter-provides).

## Connection string format

```text
chronicle://<host>[:<port>][,<host>[:<port>]...][?<options>]
chronicle://<username>:<password>@<host>[:<port>][,...][?<options>]
chronicle+srv://<host>[:<port>][?<options>]
```

The port defaults to `35000` when omitted. IPv6 literals use bracket
notation — `chronicle://[::1]:35000`. A `chronicle+srv://` connection
string accepts exactly one host, and DNS SRV records supply the real
targets and ports.

### Query string options

| Option | Default | Description |
| --- | --- | --- |
| `disableTls` | `false` | Connect over plaintext instead of TLS |
| `skipTlsValidation` | `true` | Accept any server certificate |
| `apiKey` | *(none)* | Rejected at client creation; see below |
| `auth` | *(none)* | `none` skips authentication, even with credentials |
| `loadBalancer` | `least-connections` | Policy across multiple addresses |
| `srvNameServer` | *(none)* | DNS server for `chronicle+srv://` |
| `certificatePath` | *(none)* | PKCS#12 or PEM client certificate |
| `certificatePassword` | *(none)* | Password for PKCS#12 or encrypted PEM |
| `caCertificatePath` | *(none)* | PEM or PKCS#7 trusted authorities file |
| `skipCompatibilityCheck` | `false` | Skip the compatibility check on connect |
| `skipKeepAlive` | `false` | Skip the keep-alive session |

`loadBalancer` accepts `least-connections`, `round-robin` or `random`.
Option names are matched without regard to case. An option name the client
does not recognize is ignored without an error, so check the spelling of
`skipTlsValidation` in particular: a misspelled one leaves certificate
validation off. An unknown `loadBalancer` value, or a boolean option that is
neither `true` nor `false`, throws `IllegalArgumentException`.

Percent-encode reserved characters in the user name and password, such as `@`,
`:`, `/` and `?`; a literal `+` stays a plus. A connection string whose user
information leaks a separator into the options is rejected.

`skipTlsValidation` accepts self-signed certificates. Set it to `false` to
require full certificate chain validation.

`caCertificatePath` trusts a private certificate authority, or a kernel's
self-signed certificate, without touching the JVM trust store: it points at a
PEM or PKCS#7 file holding the authorities the server certificate must chain
to. It takes effect only with `skipTlsValidation=false`, but the file is read
whenever the option is set, so a missing or unreadable file, or one without
certificates, fails client creation.

For mutual TLS, `certificatePath` names a PKCS#12 file, or a PEM file holding
the certificate chain and a PKCS#8 private key (`BEGIN PRIVATE KEY` or
`BEGIN ENCRYPTED PRIVATE KEY`) in any order. `certificatePassword` unlocks a
PKCS#12 file or an encrypted PEM key. Convert a traditional `RSA PRIVATE KEY`
with `openssl pkcs8 -topk8`. The client presents the certificate on the gRPC
channel, the token request and the load balancer probes.

<!-- validate: body -->

```kotlin
val options = ChronicleOptions.fromConnectionString(
    "chronicle://my-client:my-secret@chronicle.internal:35000?skipTlsValidation=false"
)
```

### Round-tripping to a string

`ChronicleConnectionString.toString()` renders a parsed connection string
back to its `chronicle://`/`chronicle+srv://` textual form. The result
isn't guaranteed to be byte-identical to whatever was originally parsed —
for example a host without an explicit port is rendered with the resolved
default port — but re-parsing it always yields an equal
`ChronicleConnectionString`. This is useful for storing a connection string
that was built up programmatically rather than typed by hand. The rendered
string includes the password, so do not write it to logs; log its `target`
(the first host and port) instead:

<!-- validate: body -->

```kotlin
import io.cratis.chronicle.connection.ChronicleConnectionString

val original = ChronicleConnectionString.parse(
    "chronicle://my-client:my-secret@chronicle.internal:35000?skipTlsValidation=false"
)
val rendered = original.toString()
val reparsed = ChronicleConnectionString.parse(rendered)
check(reparsed == original)
```

## Development shortcut

<!-- validate: body -->

```kotlin
val client = ChronicleClient(ChronicleOptions.development())
```

Equivalent to a connection string of
`chronicle://chronicle-dev-client:chronicle-dev-secret@localhost:35000`.
It connects over TLS but skips certificate validation, so it works
against the Kernel's self-signed development certificate without further
configuration.

## TLS and authentication

The client connects over TLS by default. Certificate validation is
skipped unless you set `skipTlsValidation=false`, which makes the client
validate the certificate chain against the JVM's default trust store. Set
`disableTls=true` only for plaintext environments.

:::danger[Every connection string skips certificate validation by default]
`skipTlsValidation` defaults to `true` for every connection string, not only
for `development()`. Against anything other than a kernel on your own
machine, add `?skipTlsValidation=false`. Otherwise the client accepts any
certificate and sends its client secret to whoever answers.
:::

The client authenticates with a client id and secret: the user name and
password in the connection string's user info section. It exchanges them for
an access token at the kernel's `/connect/token` endpoint. Without user info
it uses the development credentials, `chronicle-dev-client` and
`chronicle-dev-secret`. Read the secret from the environment or a secret
store rather than writing it into source code.

The connection string is parsed as text: everything after the first `?` is
read as options, and nothing is URL-decoded. A secret that contains `?` cannot
be written into it. Construct the connection string from its parts instead:

<!-- validate: body -->

```kotlin
import io.cratis.chronicle.connection.ChronicleConnectionString
import io.cratis.chronicle.connection.ChronicleServerAddress

val options = ChronicleOptions(
    ChronicleConnectionString(
        addresses = listOf(ChronicleServerAddress("chronicle.internal", 35000)),
        username = "my-client",
        password = System.getenv("CHRONICLE_CLIENT_SECRET")
            ?: error("Set CHRONICLE_CLIENT_SECRET"),
        skipTlsValidation = false
    )
)
```

From Java, the constructor takes all nine properties in declaration order.

:::caution[apiKey is rejected]
The Chronicle kernel has no API key authentication. From 6.5.0, creating a
client from a connection string with an `apiKey` option throws
`IllegalArgumentException`; up to 6.4.0 the option was accepted and the kernel
received no credentials at all. Use a client id and secret.
:::

A kernel that is unreachable, or that rejects the client's TLS settings,
makes the client constructor throw. Wrong credentials are detected once the
client tries to connect: the first append or `awaitRegistration()` throws
`ChronicleConnectionFailed`. See
[Connection lifecycle](connection-lifecycle.md) for how each failure shows
up.

## Namespace

See [Namespaces](/chronicle/concepts/namespaces/) for the full tenancy model.
The default namespace is `"Default"`. Override it when calling
`getEventStore`:

<!-- validate: body needs=client -->

```kotlin
val store = client.getEventStore("MyApp", namespace = "production")
```

From Java, `BlockingChronicleClient` has both forms, so you can name the
namespace or leave it at the default:

<!-- validate: body -->

```java
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.java.BlockingChronicleClient;
import io.cratis.chronicle.java.BlockingEventStore;

var client = BlockingChronicleClient.connect(ChronicleOptions.development());
BlockingEventStore store = client.getEventStore("MyApp", "production");
```

`ChronicleClient.getEventStore` declares the namespace as a Kotlin default
parameter without `@JvmOverloads`, so Java callers of that method must pass
both arguments. See [Java interop](event-store-api.md#java-interop).
