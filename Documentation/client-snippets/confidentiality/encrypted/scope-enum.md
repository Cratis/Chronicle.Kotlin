```kotlin
import io.cratis.chronicle.confidentiality.EncryptionScope

// EncryptionScope has exactly these three members - the when below is exhaustive over them.
fun encryptionScopeKeyBoundary(scope: EncryptionScope): String = when (scope) {
    EncryptionScope.Subject -> "per compliance identity - the default"
    EncryptionScope.Namespace -> "one key shared by every value marked this way in the event store namespace"
    EncryptionScope.Global -> "one key shared by every value marked this way across the whole installation"
}
```
