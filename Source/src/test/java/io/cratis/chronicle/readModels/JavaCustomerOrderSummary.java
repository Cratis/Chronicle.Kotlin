// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels;

import io.cratis.chronicle.Subject;

/**
 * A read model written as a Java record, so {@link Subject} resolution is proven against the
 * canonical constructor-parameter/field shape a Java record actually compiles to, not just the
 * Kotlin property shape.
 */
public record JavaCustomerOrderSummary(String id, @Subject String customerId, String status) {
}
