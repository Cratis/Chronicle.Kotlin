// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.patterns

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FacetSetTests {

    @Test
    fun `the empty set constrains nothing`() {
        assertTrue(FacetSet.empty.isEmpty)
        assertEquals(0, FacetSet.empty.specificity)
    }

    @Test
    fun `with adds a facet without changing the original`() {
        val original = FacetSet.empty

        val narrowed = original.with(FacetName.AggregateType, "Invoice")

        assertTrue(original.isEmpty)
        assertEquals(FacetValue("Invoice"), narrowed.valueOf(FacetName.AggregateType))
    }

    @Test
    fun `with replaces the value a name already had`() {
        val set = FacetSet.empty.with(FacetName.Day, "Monday").with(FacetName.Day, "Friday")

        assertEquals(listOf(Facet(FacetName.Day, FacetValue("Friday"))), set.facets)
    }

    @Test
    fun `an unspecified value does not constrain anything`() {
        assertFalse(FacetSet.empty.with(FacetName.Day, FacetValue.Unspecified).constrains(FacetName.Day))
    }

    @Test
    fun `sets with the same facets are equal however they were built`() {
        val one = FacetSet.empty.with(FacetName.Day, "Monday").with(FacetName.AggregateType, "Invoice")
        val other = FacetSet.empty.with(FacetName.AggregateType, "Invoice").with(FacetName.Day, "Monday")

        assertEquals(one, other)
        assertEquals(one.hashCode(), other.hashCode())
    }

    @Test
    fun `the action is the command type facet`() {
        val set = FacetSet.empty.with(FacetName.CommandType, "PayInvoice").with(FacetName.Day, "Monday")

        assertEquals(FacetValue("PayInvoice"), set.action)
        assertEquals(FacetValue.Unspecified, FacetSet.empty.action)
    }
}
