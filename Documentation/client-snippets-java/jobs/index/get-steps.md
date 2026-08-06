```java
import Cratis.Chronicle.Contracts.Jobs.JobsOuterClass;

import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.JobsServiceJavaBridge;

import java.util.List;

class JobsIndexGetSteps {
    void listJobSteps(EventStore store, String jobId) {
        List<JobsOuterClass.JobStep> steps = JobsServiceJavaBridge.getJobSteps(store.getJobs(), jobId);
        for (JobsOuterClass.JobStep step : steps) {
            System.out.println(step.getName() + ": " + step.getStatus());
        }
    }
}
```
