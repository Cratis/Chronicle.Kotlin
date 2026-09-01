```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType(id = "dec-constant-key-page-viewed")
record DecConstantKeyPageViewed(String pageUrl) {}

@EventType(id = "dec-constant-key-button-clicked")
record DecConstantKeyButtonClicked(String buttonId) {}

@EventType(id = "dec-constant-key-form-submitted")
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
                return null; // Java lambda returning Unit
            })
            .from(DecConstantKeyButtonClicked.class, fb -> {
                fb.usingConstantKey("metrics");
                fb.count("buttonClicks");
                return null; // Java lambda returning Unit
            })
            .from(DecConstantKeyFormSubmitted.class, fb -> {
                fb.usingConstantKey("metrics");
                fb.count("formSubmissions");
                return null; // Java lambda returning Unit
            });
    }
}
```
