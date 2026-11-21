// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

@file:JvmName("EventStorePatterns")

package io.cratis.chronicle

import io.cratis.chronicle.patterns.IPatterns
import io.cratis.chronicle.patterns.IPatternsCapability

/**
 * The behavior patterns of this event store, or [IPatterns.unsupported] when the implementation predates
 * patterns and does not implement [IPatternsCapability].
 */
val IEventStore.patterns: IPatterns
    get() = (this as? IPatternsCapability)?.patterns ?: IPatterns.unsupported
