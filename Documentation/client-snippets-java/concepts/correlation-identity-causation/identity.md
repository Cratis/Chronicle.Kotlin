```java
import io.cratis.chronicle.identity.Identity;
import io.cratis.chronicle.identity.IdentityProviderKt;

// Per thread, not per request or coroutine; clear in finally on thread-pooled paths.
class CorrelationIdentityCausationIdentity {
    void setForRequest(String subject, String name, String userName) {
        IdentityProviderKt.getIdentityProvider().setCurrentIdentity(new Identity(subject, name, userName, null));
    }

    Identity getCurrent() {
        return IdentityProviderKt.getIdentityProvider().getCurrentIdentity();
    }
}
```
