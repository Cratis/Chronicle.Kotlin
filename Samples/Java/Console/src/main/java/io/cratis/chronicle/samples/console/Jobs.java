// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import Cratis.Chronicle.Contracts.Jobs.JobsOuterClass;

import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.JobsServiceJavaBridge;

import java.util.List;

public class Jobs {
    /**
     * Lists jobs currently tracked for this event store/namespace — e.g. the reindex job the kernel
     * runs in the background when a unique constraint is registered.
     */
    public static void listJobs(EventStore store) {
        List<JobsOuterClass.JobSummaryResponse> jobs = JobsServiceJavaBridge.getJobs(store.getJobs());
        if (jobs.isEmpty()) {
            System.out.println("[jobs] No active jobs right now (short-lived jobs like the constraint reindex often finish before you get to list them).");
            return;
        }
        System.out.println("[jobs] " + jobs.size() + " job(s):");
        for (JobsOuterClass.JobSummaryResponse job : jobs) {
            System.out.println("  " + job.getType() + " - status=" + job.getStatus() +
                " (" + job.getProgress().getSuccessfulSteps() + "/" + job.getProgress().getTotalSteps() + " step(s))");
        }
    }
}
