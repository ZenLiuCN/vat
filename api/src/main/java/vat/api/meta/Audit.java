package vat.api.meta;


import java.lang.annotation.*;

/**
 * Entity Audit field marks
 *
 * @author Zen.Liu
 * @since 2025-10-20
 */

public interface Audit {
    /**
     * Mark an Identity type column is audit of creator. which should always use with other 4 audit annotation.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    @Inherited
    @Documented
    @interface Creator {

    }

    /**
     * Mark a timestamp compact type column is audit of last created time. which should always use with other 4 audit annotation.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    @Inherited
    @Documented
    @interface Created {

    }

    /**
     * Mark an Identity type column is audit of modifier. which should always use with other 4 audit annotation.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    @Inherited
    @Documented
    @interface Modifier {

    }

    /**
     * Mark a timestamp compact type column is audit of last modified time. which should always use with other 4 audit annotation.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    @Inherited
    @Documented
    @interface Modified {

    }

}
