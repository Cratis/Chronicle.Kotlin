```kotlin
val account = store.readModels.getInstanceByKey(AccountInfo::class, accountId)

if (account != null) {
    println("${account.name}: ${account.balance}")
}
```
