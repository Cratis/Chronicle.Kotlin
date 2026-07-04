```java
class PdlCommandItem {
    public String name = "";
    public String schema = "";
}

class PdlSliceReadModel {
    public String name = "";
    public PdlCommandItem command = null; // nullable — null until set
}
```
