```java
enum DecFromEventSequenceOrderStatus {
    Created,
    Processing,
    Shipped,
    Delivered,
    Cancelled
}

class DecFromEventSequenceOrder {
    public String orderNumber = "";
    public String customerId = "";
    public double totalAmount = 0.0;
    public DecFromEventSequenceOrderStatus status = DecFromEventSequenceOrderStatus.Created;
    public String shippedAt = null;
}
```
