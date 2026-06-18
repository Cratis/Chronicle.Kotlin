# Appending Events

## Single event

```kotlin
val result = store.eventLog.append(
    eventSourceId = "order-42",
    event = OrderPlaced(
        orderId = "order-42",
        customerId = "cust-7",
        totalAmount = 149.99
    )
)

if (result.isSuccess) {
    println("Sequence number: ${result.sequenceNumber.value}")
}
```

`AppendResult` has three fields:

| Field | Type | Description |
| --- | --- | --- |
| `isSuccess` | `Boolean` | `true` when no constraint violations |
| `sequenceNumber` | `EventSequenceNumber` | Position of appended event |
| `constraintViolations` | `List<ConstraintViolation>` | On rejection |

## Multiple events

Append several events for the same event source atomically:

```kotlin
val results = store.eventLog.appendMany(
    eventSourceId = "order-42",
    events = listOf(
        OrderPlaced(orderId = "order-42", customerId = "cust-7", totalAmount = 149.99),
        PaymentRequested(orderId = "order-42", amount = 149.99)
    )
)

val failures = results.filter { !it.isSuccess }
if (failures.isNotEmpty()) {
    failures.flatMap { it.constraintViolations }.forEach {
        println("Constraint violation: ${it.message}")
    }
}
```

## Handling constraint violations

When a constraint rejects an event, `isSuccess` is `false` and
`constraintViolations` lists the reasons. Handle them at the call site:

```kotlin
val result = store.eventLog.append("emp-001", EmployeeHired(email = "jane@example.com"))
if (!result.isSuccess) {
    val messages = result.constraintViolations.joinToString { it.message }
    println("Could not hire employee: $messages")
    return
}
```

## Append options

Pass `AppendOptions` to override the default causation or correlation:

```kotlin
store.eventLog.append(
    eventSourceId = "order-42",
    event = OrderPlaced(...),
    options = AppendOptions(correlationId = myCorrelationId)
)
```
