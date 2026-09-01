// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console

import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.concepts.ConceptAs
import io.cratis.chronicle.concepts.EventSourceId

/**
 * The values a customer is made of, each given a type of its own.
 *
 * Two things fall out of this that plain `String` properties cannot give you.
 *
 * The compiler stops telling you that a name and an email are the same thing. `CustomerRegistered`
 * takes a [FullName] and an [EmailAddress], so swapping the two arguments no longer compiles - and
 * the reducer in `CustomerReducer` can hand the event's values straight to the read model without
 * anyone having to double-check the order.
 *
 * More importantly for compliance: `@Pii` is declared **here**, once per kind of value, and never
 * again. Every event and every read model that reuses one of these concepts is PII-marked
 * automatically, so `Compliance.kt` contains no `@Pii` at all. Add a third event carrying an
 * [EmailAddress] tomorrow and it is encrypted without anyone remembering to annotate it, which is
 * the failure mode property-level annotations have: the annotation is only ever as complete as the
 * last person to add a property.
 *
 * A concept serializes as the value it wraps, so all of this is invisible on the wire -
 * `EmailAddress("eve@example.com")` goes out as `"eve@example.com"` and the schema the kernel
 * validates against is the same one a bare `String` would have produced.
 */

/**
 * Identifies a customer, and is the event source id every customer event is appended against.
 *
 * Deliberately **not** marked `@Pii`, and the schema generator would refuse it if it were: the
 * event source id is what the kernel looks up the encryption key by, so encrypting the id would
 * make its own key unfindable. That is why this is a random surrogate id rather than something
 * personal like an email address - keep the personal values in the `@Pii` concepts below.
 */
data class CustomerId(override val value: String) : EventSourceId

/** A customer's full legal name. */
@Pii(description = "Customer full legal name")
data class FullName(override val value: String) : ConceptAs<String>

/** A customer's email address. */
@Pii(description = "Customer email address")
data class EmailAddress(override val value: String) : ConceptAs<String>

/** A customer's phone contact number. */
@Pii(description = "Customer phone contact number")
data class PhoneNumber(override val value: String) : ConceptAs<String>

/** The street part of a customer's address. */
@Pii(description = "Customer street address")
data class StreetAddress(override val value: String) : ConceptAs<String>

/** The city a customer resides in. */
@Pii(description = "City of residence")
data class City(override val value: String) : ConceptAs<String>

/** The postal code of a customer's address. */
@Pii(description = "Postal code")
data class PostalCode(override val value: String) : ConceptAs<String>

/**
 * The country a customer resides in.
 *
 * Not marked `@Pii` - a country on its own identifies nobody. It is a concept anyway, so it cannot
 * be passed where a [City] or a [PostalCode] was expected.
 */
data class Country(override val value: String) : ConceptAs<String>
