// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

/**
 * Something the kernel could not make sense of in a projection declaration.
 *
 * @property message What is wrong, in the kernel's words.
 * @property line The line of the declaration it is on, counting from one.
 * @property column The column on that line, counting from one.
 */
data class ProjectionDeclarationError(
    val message: String,
    val line: Int,
    val column: Int
) {
    /** The error as `line:column: message`, which is what you want in a log or an editor gutter. */
    override fun toString(): String = "$line:$column: $message"
}
