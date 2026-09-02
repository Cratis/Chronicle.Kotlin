// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.jobs

import Cratis.Chronicle.Contracts.Jobs.JobsOuterClass

interface IJobsService {
    /**
     * Stops the job with the specified [jobId].
     */
    suspend fun stop(jobId: String)

    /**
     * Resumes the job with the specified [jobId].
     */
    suspend fun resume(jobId: String)

    /**
     * Deletes the job with the specified [jobId].
     */
    suspend fun delete(jobId: String)

    /**
     * Gets the job with the specified [jobId], or `null` if it could not be found.
     */
    suspend fun getJob(jobId: String): JobsOuterClass.JobSummaryResponse?

    /**
     * Gets all the jobs for the event store and namespace.
     */
    suspend fun getJobs(): List<JobsOuterClass.JobSummaryResponse>

    /**
     * Gets the steps for the job with the specified [jobId].
     */
    suspend fun getJobSteps(jobId: String): List<JobsOuterClass.JobStepSummaryResponse>
}
