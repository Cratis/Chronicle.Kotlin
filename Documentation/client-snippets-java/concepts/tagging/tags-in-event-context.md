```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

@ReadModel
record TaggingUserAnalytics(int loginCount, int criticalLoginCount) {
    static TaggingUserAnalytics initial() {
        return new TaggingUserAnalytics(0, 0);
    }
}

@Reducer
class TaggingUserAnalyticsReducer {
    TaggingUserAnalytics loggedIn(TaggingUserLoggedIn event, TaggingUserAnalytics current, EventContext context) {
        TaggingUserAnalytics analytics = current != null ? current : TaggingUserAnalytics.initial();

        // Tags the event was appended with are available on the context.
        boolean isCritical = context.getTags().contains("critical");

        return new TaggingUserAnalytics(
            analytics.loginCount() + 1,
            analytics.criticalLoginCount() + (isCritical ? 1 : 0));
    }
}
```
