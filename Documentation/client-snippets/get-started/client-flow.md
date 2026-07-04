```kotlin title="Main.kt"
val client = ChronicleClient(ChronicleOptions.development())
val eventStore = client.getEventStore("ChronicleConsole")

eventStore.eventLog.append("some-event-source", TestEvent("Hello world!"))
```
