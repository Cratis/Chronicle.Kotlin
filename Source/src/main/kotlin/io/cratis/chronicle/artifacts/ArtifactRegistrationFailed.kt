// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.artifacts

/** One observer that could not be started during artifact registration. */
data class ObserverStartFailure(val name: String, val cause: Throwable)

/** A registration pass that started some observers but failed to start [failures]. */
class ArtifactRegistrationFailed(val failures: List<ObserverStartFailure>) : IllegalStateException(
    "Could not start observer(s): ${failures.joinToString { it.name }}"
) {
    init {
        failures.forEach { addSuppressed(it.cause) }
    }
}
