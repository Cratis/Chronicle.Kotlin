```text
This Chronicle client does not support this workflow yet.
The JVM fluent builder in io.cratis:chronicle 6.4.0 cannot set a constant
value, so it cannot set status alongside auto-mapped properties. Mapping an
event property explicitly works: set(...).toProperty("eventProperty").
```
