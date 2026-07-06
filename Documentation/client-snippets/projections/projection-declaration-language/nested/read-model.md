```kotlin
class PdlCommandItem {
    var name: String = ""
    var schema: String = ""
}

class PdlSliceReadModel {
    var name: String = ""
    var command: PdlCommandItem? = null // nullable — null until set
}
```
