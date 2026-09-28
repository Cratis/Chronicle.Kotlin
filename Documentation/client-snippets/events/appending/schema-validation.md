```kotlin
val result = store.eventLog.append(eventSourceId, OrderPlaced(customerId, total))

if (!result.isSuccess) {
    result.constraintViolations
        .filter { it.constraintId == "SchemaValidation" }
        .forEach { violation ->
            println("Schema error at ${violation.details["path"]}: ${violation.message}")
        }
}
```
