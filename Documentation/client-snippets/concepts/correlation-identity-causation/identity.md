```kotlin
import io.cratis.chronicle.identity.Identity
import io.cratis.chronicle.identity.identityProvider

class CorrelationIdentityCausationIdentity {
    fun setForRequest(subject: String, name: String, userName: String) {
        identityProvider.setCurrentIdentity(Identity(subject, name, userName))
    }

    fun getCurrent(): Identity = identityProvider.currentIdentity
}
```
