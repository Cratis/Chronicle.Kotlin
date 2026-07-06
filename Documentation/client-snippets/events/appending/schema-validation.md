```kotlin
val result = store.eventLog.append(eventSourceId, OrderPlaced(customerId, total))

if (!result.isSuccess) {
    result.errors.forEach { error ->
        println("Schema error: $error")
    }
}
```
