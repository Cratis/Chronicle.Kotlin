// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

/**
 * What a set of declarations says about graphs.
 *
 * @property registrations What every graph registers with the kernel.
 * @property viewFailures The views that break a rule. They are left out of [registrations].
 */
internal data class GraphDeclarations(
    val registrations: List<GraphRegistration>,
    val viewFailures: List<InvalidGraphView>
) {
    /**
     * Reports the views that break a rule, once everything else has been registered: the first is thrown and the
     * others are attached to it as suppressed exceptions.
     */
    fun throwIfAnyViewIsInvalid() {
        val first = viewFailures.firstOrNull() ?: return
        viewFailures.drop(1).forEach { first.addSuppressed(it) }
        throw first
    }
}
