```java
import Cratis.Chronicle.Contracts.Jobs.JobsOuterClass;

import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.JobsServiceJavaBridge;

import java.util.List;

class JobsIndexListAll {
    void listJobs(EventStore store) {
        List<JobsOuterClass.JobSummaryResponse> jobs = JobsServiceJavaBridge.getJobs(store.getJobs());
        for (JobsOuterClass.JobSummaryResponse job : jobs) {
            System.out.println(job.getType() + ": " + job.getStatus());
        }
    }
}
```
