```java
import Cratis.Chronicle.Contracts.Jobs.JobsOuterClass;

import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.JobsServiceJavaBridge;

class JobsIndexGetOne {
    void getReindexJob(EventStore store, String jobId) {
        JobsOuterClass.Job job = JobsServiceJavaBridge.getJob(store.getJobs(), jobId);
        if (job != null) {
            System.out.println(job.getType() + ": " + job.getStatus());
        }
    }
}
```
