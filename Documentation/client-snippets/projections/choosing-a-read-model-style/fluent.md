```text
This Chronicle client does not support this workflow yet.
The JVM fluent builder in io.cratis:chronicle 6.4.0 cannot set a constant or
computed value, so this projection cannot set isBorrowed or clear borrowedBy
when a book is returned, and would not produce the same BookStatus documents.
Use the reducer tab's approach for this read model.
```
