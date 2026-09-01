```java
// EventSequenceId is a Kotlin value class, so its constructor is mangled for Java - the id is
// just this string wherever a Java API asks for one.
class SubscriptionsOutboxInboxId {
    static String resolve() {
        String inboxId = "inbox-source-event-store";
        // Resolves to: "inbox-source-event-store"
        return inboxId;
    }
}
```
