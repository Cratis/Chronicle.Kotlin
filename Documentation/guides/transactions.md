# Transactions

A unit of work groups multiple event appends into a single atomic operation.
Either all events in the unit of work are appended, or none are.

## Create a unit of work

Use `UnitOfWorkManager` to start a scoped transaction:

```kotlin
val uow = store.unitOfWork.begin()
try {
    uow.eventLog.append(
        "order-42",
        OrderPlaced(orderId = "order-42", totalAmount = 99.99)
    )
    uow.eventLog.append(
        "inventory-widget",
        StockReserved(sku = "widget", quantity = 1)
    )
    uow.commit()
} catch (e: Exception) {
    uow.rollback()
    throw e
}
```

Or use `withUnitOfWork` for automatic commit/rollback:

```kotlin
store.withUnitOfWork {
    it.eventLog.append("order-42", OrderPlaced(...))
    it.eventLog.append("inventory-widget", StockReserved(...))
    // commits automatically on successful return, rolls back on exception
}
```

## When to use transactions

Use a unit of work when you need multiple events to appear atomically — for
example, when a single business action produces events for more than one event
source, and partial application would leave the system in an inconsistent
state.

Do not use transactions as a workaround for constraint violations — constraints
fire before the append, not at commit time.
