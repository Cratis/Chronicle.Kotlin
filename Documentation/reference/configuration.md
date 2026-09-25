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
    val openTelemetry: OpenTelemetry? = null
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

`programIdentifier` is a human-readable label that shows up in diagnostics.
`defaultSinkTypeId` defaults to `WellKnownSinkTypes.MONGODB`, and can be
overridden per process with the `CHRONICLE_SINK_TYPE` environment variable
— for example `CHRONICLE_SINK_TYPE=SQL`.

The last three control artifact discovery and registration. Two helpers cover
the common adjustments — see
[Artifact Registration](../guides/artifact-registration.md) for the full picture:

<!-- validate: body -->

```kotlin
ChronicleOptions.development().withoutAutoRegistration()
ChronicleOptions.development().withArtifactsFrom("com.acme.ordering")
```

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
| `apiKey` | *(none)* | Not sent in 6.4.0; see below |
| `loadBalancer` | `least-connections` | Policy across multiple addresses |
| `srvNameServer` | *(none)* | DNS server for `chronicle+srv://` |

`loadBalancer` accepts `least-connections`, `round-robin` or `random`.
Option names are matched without regard to case. An option name the client
does not recognize is ignored without an error, so check the spelling of
`skipTlsValidation` in particular: a misspelled one leaves certificate
validation off. An unknown `loadBalancer` value throws
`IllegalArgumentException`.

`skipTlsValidation` accepts self-signed certificates. Set it to `false` to
require full certificate chain validation.

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
`ChronicleConnectionString`. This is useful for logging or persisting a
connection string that was built up programmatically rather than typed by
hand:

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

:::caution[apiKey is not sent in 6.4.0]
The client parses an `apiKey` option, but version 6.4.0 does not send it to
the kernel. Setting it only turns off the token request, so the kernel
receives no credentials at all. Use a client id and secret.
:::

A kernel that is unreachable, or that rejects the client's TLS settings,
makes the client constructor throw. Wrong credentials do not: the client
keeps trying to connect. See
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
