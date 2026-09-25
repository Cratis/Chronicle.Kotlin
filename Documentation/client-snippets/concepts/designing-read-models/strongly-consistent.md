```text
This Chronicle client does not support this workflow yet.
Kotlin and Java: @Passive only applies to a model-bound read model
(@ReadModel with @FromEvent). With io.cratis:chronicle 6.4.0 against
Chronicle 19.4.8, a passive model-bound read was not observed to return
current state, so no strongly consistent read is shown here.
Treat getInstanceByKey on a materialized read model as eventually
consistent: it can return null or an older state right after an append.
```
