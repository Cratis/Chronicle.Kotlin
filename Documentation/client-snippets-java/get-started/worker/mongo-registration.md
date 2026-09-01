```java
// Java needs no separate MongoDB registration step here either - the same
// IEventStore.getReadModels() API used by the ASP.NET Core host works identically in a
// background worker. There is no raw MongoCollection to construct or register as a bean. See
// get-started/common/mongo-query for the equivalent query.
class GetStartedWorkerMongoRegistration {
}
```
