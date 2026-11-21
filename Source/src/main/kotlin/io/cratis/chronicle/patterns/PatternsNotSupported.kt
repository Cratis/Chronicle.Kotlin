// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.patterns

/** Thrown when behavior patterns are asked for through an event store implementation that does not support them. */
class PatternsNotSupported : UnsupportedOperationException(
    "This event store implementation does not support behavior patterns; it does not implement IPatternsCapability.",
)
