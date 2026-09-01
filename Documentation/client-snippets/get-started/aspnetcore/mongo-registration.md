```kotlin
/**
 * Kotlin needs no separate MongoDB registration step here. Queries go through
 * `IEventStore.readModels`, which the connected client already exposes - there is no raw
 * `MongoCollection` to construct or register as a bean, in Spring Boot or anywhere else. See
 * `get-started/common/mongo-query` for the equivalent query.
 */
object AspNetCoreMongoRegistration
```
