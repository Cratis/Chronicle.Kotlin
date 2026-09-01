```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.seeding.ICanSeedEvents;
import io.cratis.chronicle.seeding.IEventSeedingBuilder;
import io.cratis.chronicle.seeding.IEventSeedingScopeBuilder;
import io.cratis.chronicle.seeding.Seeder;

import io.cratis.chronicle.java.EventSeedingBuilderJavaBridge;
import io.cratis.chronicle.java.EventSeedingScopeBuilderJavaBridge;

import java.util.List;

@EventType
record EvtSeedingProductCreated(String name, double price) {}

@EventType
record EvtSeedingOrganizationCreated(String name) {}

@EventType
record EvtSeedingBillingSetUp(String billingEmail) {}

@Seeder
class EvtSeedingTenantSeeding implements ICanSeedEvents {
    @Override
    public void seed(IEventSeedingBuilder builder) {
        // Global seed data - applied to every namespace
        EventSeedingBuilderJavaBridge.forEventType(
            builder,
            EvtSeedingProductCreated.class,
            "product-1",
            List.of(new EvtSeedingProductCreated("Laptop", 1299.00)));

        // Namespace-scoped seed data - applied only to the "acme" namespace
        IEventSeedingScopeBuilder acme = builder.forNamespace("acme");
        EventSeedingScopeBuilderJavaBridge.forEventType(
            acme,
            EvtSeedingUserRegistered.class,
            "user-1",
            List.of(new EvtSeedingUserRegistered("admin@acme.com", "Acme Admin")));

        // A second namespace with different seed data
        IEventSeedingScopeBuilder contoso = builder.forNamespace("contoso");
        EventSeedingScopeBuilderJavaBridge.forEventType(
            contoso,
            EvtSeedingUserRegistered.class,
            "user-1",
            List.of(new EvtSeedingUserRegistered("admin@contoso.com", "Contoso Admin")));
        contoso.forEventSource(
            "org-1",
            List.of(
                new EvtSeedingOrganizationCreated("Contoso"),
                new EvtSeedingBillingSetUp("contoso@billing.com")));
    }
}
```
