```java
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.java.BlockingChronicleClient;

// The client always connects over TLS. Chronicle Server's auto-generated self-signed development
// certificate is never written to disk or added to a trust store, so there is nothing to validate
// it against - skipTlsValidation accepts it. This is already Java's default, spelled out here
// explicitly for a server started with a certificate generated the way this guide describes.
class HostingLocalCertificatesClientConfiguration {
    BlockingChronicleClient create() {
        ChronicleOptions options = ChronicleOptions.fromConnectionString(
            "chronicle://localhost:35000?skipTlsValidation=true");
        return BlockingChronicleClient.connect(options);
    }
}
```
