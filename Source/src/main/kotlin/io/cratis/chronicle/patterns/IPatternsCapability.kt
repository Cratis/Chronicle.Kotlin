// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.patterns

/**
 * Optional capability of an [io.cratis.chronicle.IEventStore]: it can be asked about behavior patterns.
 *
 * It is a separate interface, rather than a member of `IEventStore`, so that event store implementations
 * written before patterns existed keep compiling and linking unchanged. [io.cratis.chronicle.EventStore]
 * implements it; reach the patterns through the `IEventStore.patterns` extension, which reports
 * [IPatterns.unsupported] for a store without it.
 */
interface IPatternsCapability {
    /** The behavior patterns of this event store. */
    val patterns: IPatterns
}
