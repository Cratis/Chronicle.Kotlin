// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.schemas;

import io.cratis.chronicle.compliance.Pii;

/**
 * A Java {@code record} whose components include primitives, exercised from Kotlin specs.
 *
 * Kotlin reflection cannot describe the canonical constructor of a record with a primitive
 * component — it throws rather than answering — so a schema generator that reaches for the Kotlin
 * primary constructor fails on a type shaped like this one. The {@code @Pii} component is here to
 * prove the Java reflection path still finds the annotation it was reaching for.
 */
public record JavaPayrollRunCompleted(
    String employeeId,
    @Pii(description = "Net pay for the run") double amount,
    int runNumber,
    boolean finalRun) {
}
