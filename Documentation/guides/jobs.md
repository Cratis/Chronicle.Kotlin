# Jobs

This page shows how to observe and control jobs using the Chronicle Kotlin
client. Jobs are long-running, resumable units of work the Chronicle Kernel
runs on your behalf — for example, the reindex job that runs automatically
when you register a unique constraint. See [Jobs](/chronicle/jobs/) for the
concept this page assumes. There's no API for creating a job from the
client — jobs are created by the kernel in response to server-side work.

## Listing jobs

`getJobs` returns every job currently tracked for the event store and
namespace:

```kotlin
val jobs = store.jobs.getJobs()
jobs.forEach { job ->
    println("${job.type}: ${job.status}")
}
```

## Getting a single job

```kotlin
val job = store.jobs.getJob(jobId)
if (job != null) {
    println("${job.type}: ${job.status}")
}
```

`getJob` returns `null` when no job with that id exists.

## Getting a job's steps

A job is made up of steps that run — and can fail or stop — independently.
`getJobSteps` returns them in order:

```kotlin
val steps = store.jobs.getJobSteps(jobId)
steps.forEach { step ->
    println("${step.name}: ${step.status}")
}
```

## Controlling a job

Stop a running job, resume a stopped one, or delete it once you no longer
need its history:

```kotlin
store.jobs.stop(jobId)
store.jobs.resume(jobId)
store.jobs.delete(jobId)
```

## Best practices

- Poll `getJobs`/`getJob` rather than assuming a job you triggered
  indirectly (e.g. by registering a constraint) is still running by the
  time you check — short jobs often complete before you look.
- Use `getJobSteps` to surface granular progress in an admin UI rather than
  inferring progress from the job's overall status alone.
- Treat `stop` as cooperative — steps finish their current unit of work
  before honoring it, so a job doesn't necessarily stop instantly.
