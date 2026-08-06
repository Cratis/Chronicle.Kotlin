# Configuration

## ChronicleOptions

<!-- validate: skip -->

```kotlin
data class ChronicleOptions(
    val connectionString: ChronicleConnectionString,
    val programIdentifier: String = "Unknown",
    val defaultSinkTypeId: String = System.getenv("CHRONICLE_SINK_TYPE") ?: WellKnownSinkTypes.MONGODB
)
```

| Property | Default | Description |
| --- | --- | --- |
| `connectionString` | *(required)* | Parsed address of the server |
| `programIdentifier` | `"Unknown"` | Name of the connecting program |
| `defaultSinkTypeId` | `MongoDB` | Sink for reducers and projections |

`programIdentifier` is a human-readable label that shows up in diagnostics.
`defaultSinkTypeId` defaults to `WellKnownSinkTypes.MONGODB`, and can be
overridden per process with the `CHRONICLE_SINK_TYPE` environment variable
— for example `CHRONICLE_SINK_TYPE=SQL`.

There are two factories on the companion object:

<!-- validate: body -->

```kotlin
ChronicleOptions.fromConnectionString("chronicle://chronicle.internal:35000")
ChronicleOptions.development()
```

From Java, reach them through `Companion`:

<!-- validate: body -->

```java
ChronicleOptions.Companion.fromConnectionString("chronicle://chronicle.internal:35000");
ChronicleOptions.Companion.development();
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
| `apiKey` | *(none)* | API key to authenticate with |
| `loadBalancer` | `leastConnections` | Policy across multiple addresses |
| `srvNameServer` | *(none)* | DNS server for `chronicle+srv://` |

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
validate the certificate chain against the platform trust manager — do
that whenever the server's certificate is verifiable. Set
`disableTls=true` only for plaintext environments.

Credentials are supplied either as a username and password in the
connection string's user info section, or as an `apiKey` query option.

## Namespace

See [Namespaces](/chronicle/concepts/namespaces/) for the full tenancy model.
The default namespace is `"Default"`. Override it when calling
`getEventStore`:

<!-- validate: body needs=client -->

```kotlin
val store = client.getEventStore("MyApp", namespace = "production")
```

The `namespace` default applies to Kotlin callers only — from Java, pass
both arguments:

<!-- validate: body needs=client -->

```java
EventStore store = client.getEventStore("MyApp", "production");
```
