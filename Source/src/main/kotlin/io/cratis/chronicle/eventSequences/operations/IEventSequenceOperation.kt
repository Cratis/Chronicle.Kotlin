// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences.operations

/**
 * Marks a single operation composed against an event sequence.
 *
 * Appending is the only operation today, but the composed API is built around this marker rather
 * than around [AppendOperation] directly so a new kind of operation becomes a new implementation
 * instead of a change to everything that inspects a composed operation.
 */
interface IEventSequenceOperation
