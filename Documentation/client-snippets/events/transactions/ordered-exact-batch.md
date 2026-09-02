```kotlin
val transaction = UnitOfWork(store.eventLog, context)
transaction.append("customer-1", CustomerUpdated("first"))
transaction.append("customer-2", CustomerUpdated("second"))
transaction.append("customer-1", CustomerUpdated("third"))
transaction.commit() // one appendMany RPC, in exactly this order
```
