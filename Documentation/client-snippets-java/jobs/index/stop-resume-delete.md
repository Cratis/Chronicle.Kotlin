```java
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.JobsServiceJavaBridge;

class JobsIndexControl {
    void controlJob(EventStore store, String jobId) {
        JobsServiceJavaBridge.stop(store.getJobs(), jobId);
        JobsServiceJavaBridge.resume(store.getJobs(), jobId);
        JobsServiceJavaBridge.delete(store.getJobs(), jobId);
    }
}
```
