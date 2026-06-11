# PII Compliance

Chronicle can store, encrypt, and manage personally identifiable information (PII). Annotating a field with `@Pii` signals to the kernel that it should be treated according to your compliance policies — for example, encrypting it at rest or releasing it on request.

## Mark a field as PII

Apply `@Pii` to a property on your event or read model:

```kotlin
@EventType(id = "CustomerRegistered")
data class CustomerRegistered(
    val customerId: String,
    @Pii val email: String,
    @Pii val fullName: String,
    @Pii val phoneNumber: String
)
```

When Chronicle stores events containing annotated fields, it can encrypt the values automatically using a per-subject encryption key. The `customerId` becomes the subject identifier.

## Release PII data

When a customer exercises their right to erasure, call `release` to decrypt and return their data before wiping the encryption key:

```kotlin
val payload = gson.toJson(customer)  // raw data to release
val released = store.compliance.release(
    subject = "cust-42",
    schema = Customer::class.java.name,
    payload = payload
)
println(released) // decrypted JSON
```

After release the encryption key is deleted; all stored PII for that subject becomes unreadable.

## Read model with PII fields

PII-annotated read model properties are returned in their encrypted form when queried. Pair compliance with a read model to show the current state before releasing:

```kotlin
@ReadModel
data class Customer(
    val id: String = "",
    @Pii val fullName: String = "",
    @Pii val email: String = "",
    @Pii val phoneNumber: String = ""
)
```

```kotlin
val customer = store.readModels.getInstanceByKey(Customer::class, "cust-42")
println(customer?.email) // encrypted value; use compliance.release() to decrypt
```
