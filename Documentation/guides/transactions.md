---
sharedTopicBridge: true
---

# Transactions

A Chronicle transaction is an explicit `UnitOfWork` bound to one event
sequence and one immutable `OperationContext`. It stages ordered events across
any number of event-source identifiers and
commits them with exactly one atomic `appendMany` RPC.

Direct calls to `eventLog.append` never enroll in a transaction. There is no
current transaction, request-global manager, or thread-local lookup.

<!-- validate: skip -->

```kotlin
val transaction = UnitOfWork(store.eventLog, operationContext)
transaction.append("order-123", OrderPlaced("order-123"))
transaction.append("inventory-widget", InventoryReserved("widget", 1))
transaction.commit()
```

A unit of work stops accepting events as soon as commit begins, but
`isCompleted` becomes `true` only after it reaches `COMMITTED`, `ROLLED_BACK`,
or `FAILED`. Repeated completion and later staging are rejected. Failure and
coroutine cancellation are terminal too, so client code can never retry part of
a multi-source batch.

Completion callbacks registered during commit wait for a terminal state. Every
callback runs in registration order even when one throws; failures are reported
together as `UnitOfWorkCompletionCallbackException` after the state is terminal.
A callback failure does not change a successful commit or permit the append to
be retried.

| Member | Use it for |
| --- | --- |
| `eventSequence` | The one sequence the transaction is bound to. |
| `context` | Operation metadata for the whole batch. |
| `isCompleted` | True only after commit, rollback, or failure. |
| `isSuccess` | Whether every staged event committed cleanly. |
| `getConstraintViolations` | Constraint violations detected on commit. |
| `getConcurrencyViolations` | All concurrency violations detected on commit. |
| `getAppendErrors` | Append errors detected on commit. |
| `tryGetLastCommittedEventSequenceNumber` | Highest sequence committed. |
| `onCompleted` | A callback invoked once the transaction becomes terminal. |

Java starts the same explicit transaction from
`BlockingEventSequence.beginUnitOfWork(context)` and uses try-with-resources to
roll back when control exits before commit.
