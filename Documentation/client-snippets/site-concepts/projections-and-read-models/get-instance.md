```kotlin
val status = eventStore.readModels.getInstanceByKey(BookStatus::class, bookId)

if (status != null) {
    println("${status.title}: ${if (status.isBorrowed) status.borrowedBy else "on the shelf"}")
}
```
