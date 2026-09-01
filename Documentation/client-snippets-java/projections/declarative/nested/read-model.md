```java title="Read model with nested object"
record SliceWithNestedCommand(String name, CommandItemForNestedCommand command) {}

record CommandItemForNestedCommand(String name, String schema) {}
```
