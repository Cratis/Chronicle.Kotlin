```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ClearWith;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.Nested;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.projections.SetValue;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "mb-clearing-contract-signed")
record MbClearingContractSigned(String title, String noticeGiven) {}

@EventType(id = "mb-clearing-notice-withdrawn")
record MbClearingNoticeWithdrawn() {}

@EventType(id = "mb-clearing-contract-ended")
record MbClearingContractEnded() {}

@EventType(id = "mb-clearing-employee-hired")
record MbClearingEmployeeHired() {}

@FromEvent(eventType = MbClearingContractSigned.class)
@ClearWith(eventType = MbClearingContractEnded.class)
class MbClearingContract {
    @SetFrom(propertyPath = "title", eventType = MbClearingContractSigned.class)
    public String title = "";

    // Clears this field of the nested object; the object itself stays.
    @SetFrom(propertyPath = "noticeGiven", eventType = MbClearingContractSigned.class)
    @SetValue(eventType = MbClearingNoticeWithdrawn.class, clear = true)
    public String noticeGiven;
}

// The Kotlin client only recognizes a model-bound read model once it carries at least one root
// @FromEvent - MbClearingContract's own @FromEvent above still drives the nested object.
@ReadModel
@FromEvent(eventType = MbClearingEmployeeHired.class)
class MbClearingEmployee {
    @Nested
    public MbClearingContract contract;
}
```
