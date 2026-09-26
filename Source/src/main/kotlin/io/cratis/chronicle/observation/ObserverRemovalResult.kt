// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * What came back from asking an event store to remove an observer.
 *
 * @property outcome What happened.
 * @property blockingNamespace The namespace whose observer blocked the removal, when [outcome] is a refusal;
 *   empty otherwise. The guard runs across every namespace in the event store, so a refusal that does not say
 *   where leaves nowhere to look.
 */
data class ObserverRemovalResult(val outcome: ObserverRemovalOutcome, val blockingNamespace: String) {
    /** Whether the observer was actually removed. */
    val isRemoved: Boolean get() = outcome == ObserverRemovalOutcome.Removed
}
