package vat.api.meta;

import java.lang.annotation.*;

///
/// Mark an enum property use enum name as value.
///
/// Same effect as {@link Column#enumName()}
///
/// @author Zen.Liu
/// @since 2025-10-20
///
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD})
@Documented
public @interface EnumName {

}
