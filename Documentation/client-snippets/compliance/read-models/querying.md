```kotlin
import io.cratis.chronicle.IEventStore

class ComplianceReadModelsEmployeeService(private val eventStore: IEventStore) {
    suspend fun getEmployee(id: String): ComplianceReadModelsEmployee? =
        eventStore.readModels.getInstanceByKey(ComplianceReadModelsEmployee::class, id)
}
```
