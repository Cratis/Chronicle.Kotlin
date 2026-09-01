```kotlin
import io.cratis.chronicle.EventStoreNamespaceName
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver

/**
 * Takes the namespace from a claim on the currently authenticated principal, falling back to the
 * default namespace for anonymous requests or work happening outside a request. The Spring Boot
 * integration ships this exact strategy as
 * io.cratis.chronicle.spring.namespaces.AuthenticationNamespaceResolver; this version takes the
 * current claims as a parameter so it has no framework dependency.
 */
class ClaimsBasedNamespaceResolver(
    private val claim: String,
    private val currentClaims: () -> Map<String, String>?
) : IEventStoreNamespaceResolver {
    override fun resolve(): String = currentClaims()?.get(claim) ?: EventStoreNamespaceName.default.value
}

fun createWithDefaultClaimType(currentClaims: () -> Map<String, String>?): IEventStoreNamespaceResolver =
    ClaimsBasedNamespaceResolver("tenant_id", currentClaims)

fun createWithCustomClaimType(currentClaims: () -> Map<String, String>?): IEventStoreNamespaceResolver =
    ClaimsBasedNamespaceResolver("custom_tenant_claim", currentClaims)
```
