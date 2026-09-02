```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record DecConstantKeyPageViewed(String pageUrl) {}

@EventType
record DecConstantKeyButtonClicked(String buttonId) {}

@EventType
record DecConstantKeyFormSubmitted(String formId) {}

class DecConstantKeyEngagementMetrics {
    public int pageViews = 0;
    public int buttonClicks = 0;
    public int formSubmissions = 0;
}

class DecConstantKeyEngagementMetricsProjection implements IProjectionFor<DecConstantKeyEngagementMetrics> {
    @Override
    public void define(IProjectionBuilderFor<DecConstantKeyEngagementMetrics> builder) {
        builder
            .from(DecConstantKeyPageViewed.class, fb -> {
                fb.usingConstantKey("metrics");
                fb.count("pageViews");
            })
            .from(DecConstantKeyButtonClicked.class, fb -> {
                fb.usingConstantKey("metrics");
                fb.count("buttonClicks");
            })
            .from(DecConstantKeyFormSubmitted.class, fb -> {
                fb.usingConstantKey("metrics");
                fb.count("formSubmissions");
            });
    }
}
```
