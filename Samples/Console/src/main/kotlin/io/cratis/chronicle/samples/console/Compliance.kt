// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console

import com.google.gson.Gson
import io.cratis.chronicle.EventStore
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.readModels.ReadModel

private val gson = Gson()

@EventType
data class CustomerRegistered(
    val customerId: String,
    val email: String,
    val fullName: String,
    val phoneNumber: String
)

@EventType
data class CustomerAddressUpdated(
    val customerId: String,
    val streetAddress: String,
    val city: String,
    val postalCode: String,
    val country: String
)

@ReadModel
data class Customer(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val streetAddress: String = "",
    val city: String = "",
    val postalCode: String = "",
    val country: String = "",
    val customerNumber: String = "",
    val accountStatus: String = "active",
    val totalOrders: Int = 0
)

@ReadModel(observerTypeValue = 1,
    observerClass = CustomerReducer::class
)
data class CustomerDetails(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val streetAddress: String = "",
    val city: String = "",
    val postalCode: String = "",
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
