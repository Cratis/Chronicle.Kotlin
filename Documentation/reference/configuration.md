# Configuration

## ChronicleOptions

```kotlin
data class ChronicleOptions(
    val host: String = "localhost",
    val port: Int = 35000
)
```

| Property | Default | Description |
|---|---|---|
| `host` | `"localhost"` | Hostname or IP address of the Chronicle Kernel |
| `port` | `35000` | gRPC port of the Chronicle Kernel |

## Connection string format

`ChronicleConnectionString` parses a `host:port` string:

```kotlin
val options = ChronicleOptions.from("chronicle.internal:35000")
```

## Development shortcut

```kotlin
val client = ChronicleClient.development()
```

Equivalent to `ChronicleClient(ChronicleOptions(host = "localhost", port = 35000))`.

## TLS and authentication

The current release does not support TLS or authentication at the client level. These concerns are handled at the infrastructure layer (service mesh, network policy, or load balancer termination).

## Namespace

The event store namespace scopes events within a single logical store. The default namespace is `"default"`. Override it when calling `getEventStore`:

```kotlin
val store = client.getEventStore("MyApp", namespace = "production")
```

Namespaces allow a single Chronicle Kernel to serve multiple isolated tenants or environments.
