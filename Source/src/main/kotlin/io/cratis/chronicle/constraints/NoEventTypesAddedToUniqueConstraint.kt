// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

/**
 * Thrown when a unique constraint is built without any event type added to it.
 *
 * Call `on(...)` at least once inside `unique { ... }` to say which event type and properties the
 * constraint covers.
 */
class NoEventTypesAddedToUniqueConstraint : IllegalStateException(
    "No event types were added to the unique constraint. Call on(...) at least once to say which event " +
        "type and properties it covers."
)
