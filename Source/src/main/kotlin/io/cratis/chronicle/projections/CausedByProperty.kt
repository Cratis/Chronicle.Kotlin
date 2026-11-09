// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

/**
 * The identity property of whoever caused an event, for the kernel's `$causedBy(...)` expression.
 *
 * Use it to set a read model property from the author of an event, or to key a dictionary entry by the
 * author, without spelling out the event context path.
 *
 * @property expressionName The name the kernel expects inside `$causedBy(...)`.
 */
enum class CausedByProperty(val expressionName: String) {
    /** The identity subject - the stable identifier of whoever caused the event. */
    SUBJECT("subject"),

    /** The display name of whoever caused the event. */
    NAME("name"),

    /** The user name of whoever caused the event. */
    USER_NAME("userName"),
}
