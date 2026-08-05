// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console

import io.cratis.chronicle.EventStore

/**
 * Lists jobs currently tracked for this event store/namespace — e.g. the reindex job the kernel
 * runs in the background when a unique constraint is registered.
 */
suspend fun listJobs(store: EventStore) {
    val jobs = store.jobs.getJobs()
    if (jobs.isEmpty()) {
        println("[jobs] No active jobs right now (short-lived jobs like the constraint reindex often finish before you get to list them).")
        return
    }
    println("[jobs] ${jobs.size} job(s):")
    jobs.forEach { job ->
        println("  ${job.type} - status=${job.status} (${job.progress.successfulSteps}/${job.progress.totalSteps} step(s))")
    }
}
