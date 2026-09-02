```java
try (var transaction = eventLog.beginUnitOfWork(context)) {
    transaction.append("customer-1", new CustomerUpdated("first"));
    transaction.append("customer-2", new CustomerUpdated("second"));
    transaction.append("customer-1", new CustomerUpdated("third"));
    transaction.commit(); // one appendMany RPC, in exactly this order
}
```
