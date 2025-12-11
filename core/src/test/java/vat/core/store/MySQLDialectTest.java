package vat.core.store;

import org.junit.jupiter.api.Test;

class MySQLDialectTest extends DialectTest {


    @Test
    void testSelectOne() {
        super.testSelectOne(new MySQLDialect());
    }

    @Test
    void testSelectJoinOne() {
        super.testSelectJoinOne(new MySQLDialect());
    }
    @Test
    void testSelectJsonOne() {
        super.testSelectJsonOne(new MySQLDialect());
    }
    @Test
    void testSetJsonOne() {
        super.testSetJsonOne(new MySQLDialect());
    }
}
