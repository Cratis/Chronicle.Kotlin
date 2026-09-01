```kotlin
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions

// The client always connects over TLS. Chronicle Server's auto-generated self-signed development
// certificate is never written to disk or added to a trust store, so there is nothing to validate
// it against - skipTlsValidation accepts it. This is already Kotlin's default, spelled out here
// explicitly for a server started with a certificate generated the way this guide describes.
fun createClientForLocalCertificate(): ChronicleClient {
    val options = ChronicleOptions.fromConnectionString("chronicle://localhost:35000?skipTlsValidation=true")
    return ChronicleClient(options)
}
```
