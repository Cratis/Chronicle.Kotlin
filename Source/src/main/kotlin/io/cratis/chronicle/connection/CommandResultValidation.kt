// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

/**
 * Validates the failure information shared by Chronicle command results.
 *
 * The 16.44.1 contracts were generated from a C# model whose `IsAuthorized` default is `true`.
 * Proto3 cannot encode that default, so a successful response which omits the field is read by JVM
 * clients as `false`. Authorization is therefore rejected only when the kernel supplies its concrete
 * failure reason. Validation and exception messages are always concrete failures.
 */
internal fun validateCommandResult(
    operation: String,
    authorizationFailureReason: String,
    validationMessages: List<String>,
    exceptionMessages: List<String>
) {
    val failures = buildList {
        if (authorizationFailureReason.isNotBlank()) add("authorization: $authorizationFailureReason")
        validationMessages.filterTo(this) { it.isNotBlank() }
        exceptionMessages.filterTo(this) { it.isNotBlank() }
    }
    check(failures.isEmpty()) {
        "Chronicle could not $operation: ${failures.joinToString("; ")}"
    }
}
