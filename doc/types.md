# type and concepts

## scalar types

1. boolean: 8 bits data of 0 or 1
2. byte: 8 bits signed integer
3. short: 16 bits signed integer
4. int: 32 bits signed integer
5. long: 64 bits signed integer
6. char: 32 bits signed integer
7. float: 32 bits signed floating-point decimal
8. double: 64 bits signed floating-point decimal
9. String: dynamic length UTF-8 characters
10. Decimal: dynamic length of numeric value
11. Numeric: dynamic length of numeric value
12. Date: local date value
13. Time: local time value
14. DateTime: local date time value
15. Instant: UTC timestamp
16. TimeTZ: time with time zone
17. DateTimeTZ: date time with time zone
18. JsonObject: json object
19. JsonArray: json array object
20. Duration: time interval of maximum of days
21. Period: time interval of minimal of days
22. Binary: variable length or bytes, maximum length is 2^(32-1)
23. Buffer: variable length or bytes
24. UUID: universal unique identifier present as fixed 32 characters `String`

## complex types

1. Enum: present as `String` or ordinal `int` number
2. Array: variable length of repeated type elements
3. List: variable length of repeated type elements
4. Set: `List` with unique element
5. Projection: `Map<K,V>`
6. Data: group of properties with an type identifier.

## domain concepts

1. Data: just THE `Data`
2. Entity: data that persistence. must have a numeric or UUID identity property.
3. Event: same as `Data` but with extra required `EventKind` property
4. Actor: Entity that present a user
5. Ability: Entity that relative to a user
6. Record: Entity that records data.
7. Activities: group of actions for a domain.

## runtime concepts

1. Node: contains groups of domains.
2. Verticle: runtime container of one domain.
