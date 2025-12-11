package vat.core.store;

import org.junit.jupiter.api.Test;

class PgDialectTest extends DialectTest {


    @Test
    void testSelectOne() {
        super.testSelectOne(new PgDialect());
    }

    @Test
    void testSelectJoinOne() {
        super.testSelectJoinOne(new PgDialect());
    }
    @Test
    void testSelectJsonOne() {
        super.testSelectJsonOne(new PgDialect());
    }
    @Test
    void testSetJsonOne() {
        super.testSetJsonOne(new PgDialect());
    }
}
