```java
import io.cratis.chronicle.compliance.Pii;

// Every value this type holds is personal, so mark the type once.
@Pii
record PiiAttrDiagnosis(String condition, String diagnosedBy) {
}

// Both condition and diagnosedBy are encrypted wherever a PiiAttrDiagnosis appears.
record PiiAttrPatientRecord(String name, PiiAttrDiagnosis diagnosis) {
}
```
