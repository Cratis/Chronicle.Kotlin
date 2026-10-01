// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.jobs

import Cratis.Chronicle.Contracts.Jobs.JobsGrpcKt
import Cratis.Chronicle.Contracts.Jobs.JobsOuterClass
import bcl.Bcl
import io.cratis.chronicle.toBclGuid
import java.util.UUID

class JobsService(
    private val eventStoreName: String,
    private val namespace: String,
    private val stub: JobsGrpcKt.JobsCoroutineStub
) : IJobsService {

    override suspend fun stop(jobId: String) {
        stub.stopJob(
            JobsOuterClass.StopJobRequest.newBuilder()
                .setEventStore(eventStoreName)
                .setNamespace(namespace)
                .setJobId(jobId.toContractsGuid())
                .build()
        )
    }

    override suspend fun resume(jobId: String) {
        stub.resumeJob(
            JobsOuterClass.ResumeJobRequest.newBuilder()
                .setEventStore(eventStoreName)
                .setNamespace(namespace)
                .setJobId(jobId.toContractsGuid())
                .build()
        )
    }

    override suspend fun delete(jobId: String) {
        stub.deleteJob(
            JobsOuterClass.DeleteJobRequest.newBuilder()
                .setEventStore(eventStoreName)
                .setNamespace(namespace)
                .setJobId(jobId.toContractsGuid())
                .build()
        )
    }

    /**
     * The kernel has no single-job query - it serves the whole set and expects the caller to pick.
     * Keeping this on [IJobsService] means a caller after one job still writes one line.
     */
    override suspend fun getJob(jobId: String): JobsOuterClass.JobSummaryResponse? {
        val id = jobId.toContractsGuid()
        return getJobs().firstOrNull { it.id == id }
    }

    /**
     * All jobs for the event store and namespace.
     *
     * The kernel serves this as an observable query - a stream that emits the whole set again every
     * time a job changes - so this takes the first emission and lets the subscription go.
     */
    override suspend fun getJobs(): List<JobsOuterClass.JobSummaryResponse> {
        val request = JobsOuterClass.AllJobsRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .build()

        return stub.allJobs(request).dataList
    }

    override suspend fun getJobSteps(jobId: String): List<JobsOuterClass.JobStepSummaryResponse> {
        val request = JobsOuterClass.GetJobStepsRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setJobId(jobId.toContractsGuid())
            .build()

        return stub.getJobSteps(request).dataList
    }
}

private fun String.toContractsGuid(): Bcl.Guid = UUID.fromString(this).toBclGuid()
