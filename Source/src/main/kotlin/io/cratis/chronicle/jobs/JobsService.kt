// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.jobs

import Cratis.Chronicle.Contracts.Jobs.JobsGrpcKt
import Cratis.Chronicle.Contracts.Jobs.JobsOuterClass
import bcl.Bcl
import java.util.UUID

class JobsService(
    private val eventStoreName: String,
    private val namespace: String,
    private val stub: JobsGrpcKt.JobsCoroutineStub
) : IJobsService {

    override suspend fun stop(jobId: String) {
        stub.stop(
            JobsOuterClass.StopJob.newBuilder()
                .setEventStore(eventStoreName)
                .setNamespace(namespace)
                .setJobId(jobId.toContractsGuid())
                .build()
        )
    }

    override suspend fun resume(jobId: String) {
        stub.resume(
            JobsOuterClass.ResumeJob.newBuilder()
                .setEventStore(eventStoreName)
                .setNamespace(namespace)
                .setJobId(jobId.toContractsGuid())
                .build()
        )
    }

    override suspend fun delete(jobId: String) {
        stub.delete(
            JobsOuterClass.DeleteJob.newBuilder()
                .setEventStore(eventStoreName)
                .setNamespace(namespace)
                .setJobId(jobId.toContractsGuid())
                .build()
        )
    }

    override suspend fun getJob(jobId: String): JobsOuterClass.Job? {
        val request = JobsOuterClass.GetJobRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setJobId(jobId.toContractsGuid())
            .build()

        val response = stub.getJob(request)
        return if (response.hasValue0()) response.value0 else null
    }

    override suspend fun getJobs(): List<JobsOuterClass.Job> {
        val request = JobsOuterClass.GetJobsRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .build()

        return stub.getJobs(request).itemsList
    }

    override suspend fun getJobSteps(jobId: String): List<JobsOuterClass.JobStep> {
        val request = JobsOuterClass.GetJobStepsRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setJobId(jobId.toContractsGuid())
            .build()

        return stub.getJobSteps(request).itemsList
    }
}

private fun String.toContractsGuid(): Bcl.Guid {
    // bcl.Guid: lo = first 8 bytes, hi = second 8 bytes, little-endian.
    // Java UUID.mostSignificantBits and leastSignificantBits are big-endian, so reverse each half.
    val uuid = UUID.fromString(this)
    return Bcl.Guid.newBuilder()
        .setLo(java.lang.Long.reverseBytes(uuid.mostSignificantBits))
        .setHi(java.lang.Long.reverseBytes(uuid.leastSignificantBits))
        .build()
}
