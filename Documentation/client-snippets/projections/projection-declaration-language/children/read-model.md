```kotlin
class PdlGroupMember {
    var userId: String = ""
    var name: String = ""
    var role: String = ""
}

class PdlGroupReadModel {
    var name: String = ""
    var members: MutableList<PdlGroupMember> = mutableListOf()
}
```
