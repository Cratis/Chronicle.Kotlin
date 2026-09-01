```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.ClearWith
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.Nested
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.projections.SetValue
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "mb-clearing-contract-signed")
data class MbClearingContractSigned(val title: String, val noticeGiven: String)

@EventType(id = "mb-clearing-notice-withdrawn")
data class MbClearingNoticeWithdrawn(val placeholder: Boolean = true)

@EventType(id = "mb-clearing-contract-ended")
data class MbClearingContractEnded(val placeholder: Boolean = true)

@EventType(id = "mb-clearing-employee-hired")
data class MbClearingEmployeeHired(val placeholder: Boolean = true)

@FromEvent(MbClearingContractSigned::class)
@ClearWith(MbClearingContractEnded::class)
data class MbClearingContract(
    @SetFrom("title", MbClearingContractSigned::class)
    val title: String = "",

    // Clears this property of the nested object; the object itself stays.
    @SetFrom("noticeGiven", MbClearingContractSigned::class)
    @SetValue(MbClearingNoticeWithdrawn::class, clear = true)
    val noticeGiven: String? = null
)

// The Kotlin client only recognizes a model-bound read model once it carries at least one root
// @FromEvent - MbClearingContract's own @FromEvent above still drives the nested object.
@ReadModel
@FromEvent(MbClearingEmployeeHired::class)
data class MbClearingEmployee(
    @Nested
    val contract: MbClearingContract? = null
)
```
