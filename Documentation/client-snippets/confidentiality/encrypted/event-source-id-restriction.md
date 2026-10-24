```kotlin
import io.cratis.chronicle.concepts.EventSourceId
import io.cratis.chronicle.confidentiality.Encrypted

// This will throw EncryptedNotSupportedOnEventSourceId
@Encrypted
data class EncryptedAttrPartnerId(val value: String) : EventSourceId {
    override fun value(): String = value
}
```
