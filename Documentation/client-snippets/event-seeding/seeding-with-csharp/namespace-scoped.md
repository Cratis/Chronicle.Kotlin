```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.seeding.ICanSeedEvents
import io.cratis.chronicle.seeding.IEventSeedingBuilder
import io.cratis.chronicle.seeding.Seeder

@EventType
data class EvtSeedingProductCreated(val name: String, val price: Double)

@EventType
data class EvtSeedingOrganizationCreated(val name: String)

@EventType
data class EvtSeedingBillingSetUp(val billingEmail: String)

@Seeder
class EvtSeedingTenantSeeding : ICanSeedEvents {
    override fun seed(builder: IEventSeedingBuilder) {
        // Global seed data - applied to every namespace
        builder.forEventType(
            EvtSeedingProductCreated::class,
            "product-1",
            listOf(EvtSeedingProductCreated("Laptop", 1299.00))
        )

        // Namespace-scoped seed data - applied only to the "acme" namespace
        builder.forNamespace("acme")
            .forEventType(
                EvtSeedingUserRegistered::class,
                "user-1",
                listOf(EvtSeedingUserRegistered("admin@acme.com", "Acme Admin"))
            )

        // A second namespace with different seed data
        builder.forNamespace("contoso")
            .forEventType(
                EvtSeedingUserRegistered::class,
                "user-1",
                listOf(EvtSeedingUserRegistered("admin@contoso.com", "Contoso Admin"))
            )
            .forEventSource(
                "org-1",
                listOf(
                    EvtSeedingOrganizationCreated("Contoso"),
                    EvtSeedingBillingSetUp("contoso@billing.com")
                )
            )
    }
}
```
