// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console

import io.cratis.chronicle.EventStore
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.RedactionReason
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class CustomerRegistered(
    val customerId: String,
    @Pii(description = "Customer email address") val email: String,
    @Pii(description = "Customer full legal name") val fullName: String,
    @Pii(description = "Customer phone contact number") val phoneNumber: String
)

@EventType
data class CustomerAddressUpdated(
    val customerId: String,
    @Pii(description = "Customer street address") val streetAddress: String,
    @Pii(description = "City of residence") val city: String,
    @Pii(description = "Postal code") val postalCode: String,
    val country: String
)

@ReadModel
data class Customer(
    val id: String = "",
    @Pii(description = "Customer full legal name") val fullName: String = "",
    @Pii(description = "Customer email address") val email: String = "",
    @Pii(description = "Customer phone contact number") val phoneNumber: String = "",
    @Pii(description = "Customer street address") val streetAddress: String = "",
    @Pii(description = "City of residence") val city: String = "",
    @Pii(description = "Postal code") val postalCode: String = "",
    val country: String = "",
    val customerNumber: String = "",
    val accountStatus: String = "active",
    val totalOrders: Int = 0
)

@ReadModel
data class CustomerDetails(
    val id: String = "",
    @Pii(description = "Customer full legal name") val fullName: String = "",
    @Pii(description = "Customer email address") val email: String = "",
    @Pii(description = "Customer phone contact number") val phoneNumber: String = "",
    @Pii(description = "Customer street address") val streetAddress: String = "",
    @Pii(description = "City of residence") val city: String = "",
    @Pii(description = "Postal code") val postalCode: String = "",
    val country: String = ""
)

data class SampleCustomerData(
    val id: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val streetAddress: String,
    val city: String,
    val postalCode: String,
    val country: String
)

val sampleCustomer = SampleCustomerData(
    id = "c0000001-0000-0000-0000-000000000000",
    fullName = "Eve Jackson",
    email = "eve.jackson@example.com",
    phoneNumber = "+1-202-555-0143",
    streetAddress = "742 Evergreen Terrace",
    city = "Springfield",
    postalCode = "49007",
    country = "USA"
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
    val results = store.eventLog.appendMany(sampleCustomer.id, listOf(registered, addressUpdated))
    val failures = results.filter { !it.isSuccess }
    if (failures.isNotEmpty()) {
        val violations = failures.flatMap { it.constraintViolations }.map { it.message }
        println("[pii] Could not register ${sampleCustomer.fullName}: ${violations.joinToString("; ")}")
        return
    }
    val lastSeq = results.last().sequenceNumber.value
    println("[pii] Registered ${sampleCustomer.fullName} (${sampleCustomer.id}) with PII events up to sequence $lastSeq")
}

/**
 * Deletes the encryption key used for [sampleCustomer]'s PII — a real "right to be forgotten" erasure.
 * Existing encrypted PII values become permanently unreadable; no re-encryption or rollback is possible.
 */
suspend fun deleteCustomerEncryptionKey(store: EventStore) {
    store.compliance.deleteEncryptionKey(sampleCustomer.id)
    println("[pii] Deleted the encryption key for ${sampleCustomer.fullName} (${sampleCustomer.id}). Its encrypted PII can no longer be decrypted.")
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
        "[redact] Permanently redacted every event for ${sampleCustomer.fullName} (${sampleCustomer.id}). " +
            "This cannot be undone."
    )
}

suspend fun showCustomerReadModel(store: io.cratis.chronicle.IEventStore) {
    val customer = store.readModels.getInstanceByKey(CustomerDetails::class, sampleCustomer.id)
    if (customer == null || customer.id.isEmpty()) {
        println("[pii] No CustomerDetails read model found for ${sampleCustomer.id}. Register the customer first (press C).")
        return
    }
    fun fmt(label: String, value: String, isPii: Boolean): String =
        "  ${label.padEnd(15)}: ${value.ifEmpty { "(empty)" }}${if (isPii) "   [PII]" else ""}"

    println(listOf(
        "Customer read model for ${customer.id}:",
        fmt("Full name",       customer.fullName,       true),
        fmt("Email",           customer.email,          true),
        fmt("Phone number",    customer.phoneNumber,    true),
        fmt("Street address",  customer.streetAddress,  true),
        fmt("City",            customer.city,           true),
        fmt("Postal code",     customer.postalCode,     true),
        fmt("Country",         customer.country,        false),
        "  PII fields are stored encrypted at rest — values above are the encrypted form."
    ).joinToString("\n"))
}
