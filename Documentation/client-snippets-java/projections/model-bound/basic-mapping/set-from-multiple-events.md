```java title="Multiple set mappings"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "account-opened-for-rename")
record AccountOpenedForRename(String accountName) {}

@EventType(id = "account-renamed-for-rename")
record AccountRenamedForRename(String newName) {}

@ReadModel
@FromEvent(eventType = AccountOpenedForRename.class)
@FromEvent(eventType = AccountRenamedForRename.class)
class RenameableAccount {
    @SetFrom(propertyPath = "accountName", eventType = AccountOpenedForRename.class)
    @SetFrom(propertyPath = "newName", eventType = AccountRenamedForRename.class)
    public String name = "";
}
```
