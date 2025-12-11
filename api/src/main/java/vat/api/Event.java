package vat.api;

import vat.api.meta.Describe;
import vat.api.meta.EventKind;

//formatter:off
///
/// @author Zen.Liu
/// @since 2025-10-20
///
///eventkind:
/// 1.  use static integer field KIND_NAME as kind enumerations. In such case, use method
///```java
/// int kind();
/// ```
/// 2.  use an enum value
///```java
/// @EventKind SomeEnum kind();
/// ```
//formatter:on
@Prototype
public interface Event extends Data {
    interface EnumBased<T extends Enum<T>> extends Event {
        @EventKind
        @Describe(value = "_EVENT_KIND", desc = "_DESC_EVENT_KIND")
        T kind();
    }

}
