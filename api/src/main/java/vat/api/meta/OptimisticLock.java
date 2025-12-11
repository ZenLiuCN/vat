package vat.api.meta;


import java.lang.annotation.*;

/**
 * Mark a integer (32bit) column is optimistic locker
 * @author Zen.Liu
 * @since 2025-10-20
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@Inherited
@Documented
public @interface OptimisticLock {

}
