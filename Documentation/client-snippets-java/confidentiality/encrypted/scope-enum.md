```java
import io.cratis.chronicle.confidentiality.EncryptionScope;

// EncryptionScope has exactly these three members - the switch below is exhaustive over them.
class EncryptionScopeKeyBoundaries {
    static String describe(EncryptionScope scope) {
        return switch (scope) {
            case Subject -> "per compliance identity - the default";
            case Namespace -> "one key shared by every value marked this way in the event store namespace";
            case Global -> "one key shared by every value marked this way across the whole installation";
        };
    }
}
```
