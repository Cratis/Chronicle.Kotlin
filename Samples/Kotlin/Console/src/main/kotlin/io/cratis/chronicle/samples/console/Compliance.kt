// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console

import io.cratis.chronicle.EventStore
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.concepts.ConceptAs
import io.cratis.chronicle.concepts.appendMany
import io.cratis.chronicle.concepts.getInstanceByKey
import io.cratis.chronicle.concepts.redactForEventSource
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.RedactionReason
import io.cratis.chronicle.readModels.ReadModel
import kotlin.reflect.full.findAnnotation

// Not a single `@Pii` anywhere in this file, and every personal value below is still encrypted at
// rest. The annotation lives once on each concept in `CustomerConcepts.kt`, and comes along with
// the type wherever it is used.

@EventType
data class CustomerRegistered(
    val customerId: CustomerId,
    val email: EmailAddress,
    val fullName: FullName,
    val phoneNumber: PhoneNumber
)

@EventType
data class CustomerAddressUpdated(
    val customerId: CustomerId,
    val streetAddress: StreetAddress,
    val city: City,
    val postalCode: PostalCode,
    val country: Country
)

@ReadModel
data class Customer(
    val id: CustomerId = CustomerId(""),
    val fullName: FullName = FullName(""),
    val email: EmailAddress = EmailAddress(""),
    val phoneNumber: PhoneNumber = PhoneNumber(""),
    val streetAddress: StreetAddress = StreetAddress(""),
    val city: City = City(""),
    val postalCode: PostalCode = PostalCode(""),
    val country: Country = Country(""),
    val customerNumber: String = "",
    val accountStatus: String = "active",
    val totalOrders: Int = 0
)

@ReadModel
data class CustomerDetails(
    val id: CustomerId = CustomerId(""),
    val fullName: FullName = FullName(""),
    val email: EmailAddress = EmailAddress(""),
    val phoneNumber: PhoneNumber = PhoneNumber(""),
    val streetAddress: StreetAddress = StreetAddress(""),
    val city: City = City(""),
    val postalCode: PostalCode = PostalCode(""),
    val country: Country = Country("")
)

data class SampleCustomerData(
    val id: CustomerId,
    val fullName: FullName,
    val email: EmailAddress,
    val phoneNumber: PhoneNumber,
    val streetAddress: StreetAddress,
    val city: City,
    val postalCode: PostalCode,
    val country: Country
)

val sampleCustomer = SampleCustomerData(
    id = CustomerId("c0000001-0000-0000-0000-000000000000"),
    fullName = FullName("Eve Jackson"),
    email = EmailAddress("eve.jackson@example.com"),
    phoneNumber = PhoneNumber("+1-202-555-0143"),
    streetAddress = StreetAddress("742 Evergreen Terrace"),
    city = City("Springfield"),
    postalCode = PostalCode("49007"),
    country = Country("USA")
)

suspend fun registerCustomerWithPii(store: io.cratis.chronicle.IEventStore) {
    val registered = CustomerRegistered(
        customerId = sampleCustomer.id,
        email = sampleCustomer.email,
        fullName = sampleCustomer.fullName,
        phoneNumber = sampleCustomer.phoneNumber
    )
    val addressUpdated = CustomerAddressUpdated(
        customerId = sampleCustomer.id,
        streetAddress = sampleCustomer.streetAddress,
        city = sampleCustomer.city,
        postalCode = sampleCustomer.postalCode,
        country = sampleCustomer.country
    )
    // Takes the CustomerId as-is — every place the client accepts an event source id as a String
    // has an overload taking a concept, so the type survives all the way to the call.
    val results = store.eventLog.appendMany(sampleCustomer.id, listOf(registered, addressUpdated))
    val failures = results.filter { !it.isSuccess }
    if (failures.isNotEmpty()) {
        val violations = failures.flatMap { it.constraintViolations }.map { it.message }
        println("[pii] Could not register ${sampleCustomer.fullName.value}: ${violations.joinToString("; ")}")
        return
    }
    val lastSeq = results.last().sequenceNumber.value
    println("[pii] Registered ${sampleCustomer.fullName.value} (${sampleCustomer.id.value}) with PII events up to sequence $lastSeq")
}

/**
 * Deletes the encryption key used for [sampleCustomer]'s PII — a real "right to be forgotten" erasure.
 * Existing encrypted PII values become permanently unreadable; no re-encryption or rollback is possible.
 */
suspend fun deleteCustomerEncryptionKey(store: EventStore) {
    store.compliance.deleteEncryptionKey(sampleCustomer.id.value)
    println("[pii] Deleted the encryption key for ${sampleCustomer.fullName.value} (${sampleCustomer.id.value}). Its encrypted PII can no longer be decrypted.")
}

/**
 * Permanently redacts the most recent address-change event for [person].
 *
 * Redaction is a destructive content rewrite, not a soft delete or a field mask — once this
 * returns, the original address content for that specific event is gone from the event store for
 * good. Demonstrates [io.cratis.chronicle.eventSequences.IEventSequence.getForEventSourceIdAndEventTypes]
 * to locate the event's sequence number, then [io.cratis.chronicle.eventSequences.IEventSequence.redact]
 * to erase its content.
 */
suspend fun redactLastAddressChange(store: io.cratis.chronicle.IEventStore, person: Person) {
    val addressEvents = store.eventLog.getForEventSourceIdAndEventTypes(
        person.id,
        listOf(EmployeeAddressSet::class, EmployeeMoved::class)
    )
    val last = addressEvents.maxByOrNull { it.context.sequenceNumber }
    if (last == null) {
        println("[redact] ${person.firstName} ${person.lastName} has no address-change events to redact.")
        return
    }
    store.eventLog.redact(
        EventSequenceNumber(last.context.sequenceNumber),
        RedactionReason("Sample: erase address history")
    )
    println(
        "[redact] Permanently redacted the address event at sequence ${last.context.sequenceNumber} " +
            "for ${person.firstName} ${person.lastName}. The original content is gone — this cannot be undone."
    )
}

/**
 * Permanently redacts every event for [sampleCustomer] — a full "right to be forgotten" erasure.
 *
 * More thorough than [deleteCustomerEncryptionKey]: instead of leaving encrypted-but-unreadable
 * content behind, this rewrites every event's content for the event source, gone for good. Like
 * [redactLastAddressChange], this is destructive and irreversible — only use it for a confirmed
 * compliance/erasure request.
 */
suspend fun redactAllCustomerEvents(store: io.cratis.chronicle.IEventStore) {
    store.eventLog.redactForEventSource(sampleCustomer.id, RedactionReason("Sample: GDPR erasure request"))
    println(
        "[redact] Permanently redacted every event for ${sampleCustomer.fullName.value} (${sampleCustomer.id.value}). " +
            "This cannot be undone."
    )
}

suspend fun showCustomerReadModel(store: io.cratis.chronicle.IEventStore) {
    val customer = store.readModels.getInstanceByKey(CustomerDetails::class, sampleCustomer.id)
    if (customer == null || customer.id.value.isEmpty()) {
        println("[pii] No CustomerDetails read model found for ${sampleCustomer.id.value}. Register the customer first (press C).")
        return
    }

    println(listOf(
        "Customer read model for ${customer.id.value}:",
        fmt("Full name",      customer.fullName),
        fmt("Email",          customer.email),
        fmt("Phone number",   customer.phoneNumber),
        fmt("Street address", customer.streetAddress),
        fmt("City",           customer.city),
        fmt("Postal code",    customer.postalCode),
        fmt("Country",        customer.country),
        "  Every [PII] value above is encrypted at rest and decrypted on read. Press K to delete the",
        "  encryption key and view this again — the PII comes back empty, Country is untouched."
    ).joinToString("\n"))
}

/**
 * Formats one value, reading the `[PII]` marker off the concept's own type.
 *
 * Nothing here keeps a list of which fields are personal — [Pii] is metadata on the type, so
 * anything that needs to know can ask, and it can never fall out of date with what is actually
 * encrypted. Chronicle's schema generator answers the same question the same way.
 */
private fun fmt(label: String, value: ConceptAs<String>): String {
    val pii = value::class.findAnnotation<Pii>()
    val marker = pii?.let { "   [PII: ${it.description}]" } ?: ""
    return "  ${label.padEnd(15)}: ${value.value.ifEmpty { "(empty)" }}$marker"
}
