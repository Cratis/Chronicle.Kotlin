```java
import io.cratis.chronicle.compliance.Pii;

class ChoosingStyleBookStatus {
    public String id = "";
    public String title = null;
    public String isbn = null;
    public boolean isBorrowed = false;
    @Pii
    public String borrowedBy = null;
}
```
