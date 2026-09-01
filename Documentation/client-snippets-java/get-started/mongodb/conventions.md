```java
// Java has nothing to configure here. Chronicle stores event source ids as standard UUIDs and
// read model fields as camelCase names - which already match Java property names - and reads go
// through IEventStore.getReadModels() rather than a raw MongoDB driver, so there is no
// MongoClientSettings, GUID representation, or "ignore unknown elements" convention to register.
// Unknown document fields are simply not deserialized into properties the read model class does
// not declare.
class GetStartedMongoDbDefaults {
}
```
