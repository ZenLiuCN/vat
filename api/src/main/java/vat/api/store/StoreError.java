package vat.api.store;

import vat.api.DomainError;

///
/// @author Zen.Liu
/// @since 2025-10-24


public class StoreError extends DomainError {
    public enum ErrorCode {
        OTHER_ERROR(10000),
        MULTI_ERROR(10001),

        DATA_VIOLATION(10010),

        DATA_VIOLATION_DUPLICATED(10011),
        DATA_VIOLATION_MISSING_FK(10012),
        DATA_VIOLATION_NULL_VALUE(10013),
        DATA_VIOLATION_CONSTRAINT(10014),
        /// unexpect data size
        DATA_VIOLATION_UNEXPECT(10019),

        GRAMMAR_ERROR(10020),
        GRAMMAR_SYNTAX(10021),
        GRAMMAR_FUNCTION(10022),
        GRAMMAR_TABLE(10023),
        GRAMMAR_COLUMN(10024),

        DATA_ACCESS_ERROR(10030),
        DATA_ACCESS_OVERFLOW(10031),
        DATA_ACCESS_FORMAT(10032),
        DATA_ACCESS_TEXT(10033),
        DATA_ACCESS_RANG(10034),

        CONNECTION_ERROR(10040),

        LOCK_ERROR(10050),
        LOCK_DEAD_LOCK(10051),
        LOCK_MISSING_LOCK(10052),
        LOCK_WAIT_TIMEOUT(10053),

        PERMISSION_ERROR(10060),
        PERMISSION_AUTHORIZATION(10061),
        PERMISSION_PRIVILEGES(10062),

        RESOURCE_ERROR(10070),
        RESOURCE_LIMIT(10071),

        CONCURRENCY_ERROR(10080),
        ;
        public final int code;

        ErrorCode(int code) {
            this.code = code;
        }
    }


    public StoreError(ErrorCode code, String message, Throwable cause) {
        super(0, code.code, null, message, cause);
    }
}
