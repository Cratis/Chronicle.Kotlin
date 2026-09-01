```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@EventType
record PassiveReducersPaymentReceived(String category, double amount) {}

@ReadModel
record PassiveReducersMonthlyRevenueReport(
    double totalRevenue,
    Map<String, Double> revenueByCategory,
    int month,
    int year) {
    PassiveReducersMonthlyRevenueReport() {
        this(0.0, Map.of(), 0, 0);
    }
}

@Reducer(isActive = false)
class PassiveReducersMonthlyRevenueReportReducer {
    PassiveReducersMonthlyRevenueReport received(PassiveReducersPaymentReceived event, PassiveReducersMonthlyRevenueReport current, EventContext context) {
        double revenue = current == null ? 0.0 : current.totalRevenue();
        Map<String, Double> byCategory = new HashMap<>(current == null ? Map.of() : current.revenueByCategory());
        byCategory.merge(event.category(), event.amount(), Double::sum);

        ZonedDateTime occurred = context.getOccurred().atZone(ZoneOffset.UTC);

        return new PassiveReducersMonthlyRevenueReport(
            revenue + event.amount(),
            byCategory,
            occurred.getMonthValue(),
            occurred.getYear());
    }
}
```
