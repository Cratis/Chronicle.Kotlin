```java
import java.util.ArrayList;
import java.util.List;

class PdlGroupMember {
    public String userId = "";
    public String name = "";
    public String role = "";
}

class PdlGroupReadModel {
    public String name = "";
    public List<PdlGroupMember> members = new ArrayList<>();
}
```
