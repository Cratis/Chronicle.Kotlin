```kotlin
import io.cratis.chronicle.identity.Identity
import io.cratis.chronicle.identity.identityProvider

// Per thread, not per request or coroutine; clear in finally on thread-pooled paths.
class CorrelationIdentityCausationIdentity {
    fun setForRequest(subject: String, name: String, userName: String) {
        identityProvider.setCurrentIdentity(Identity(subject, name, userName))
    }

    fun getCurrent(): Identity = identityProvider.currentIdentity
}
```
