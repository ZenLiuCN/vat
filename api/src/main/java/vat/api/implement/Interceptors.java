package vat.api.implement;


import java.util.function.DoubleUnaryOperator;
import java.util.function.IntUnaryOperator;
import java.util.function.LongUnaryOperator;

///
/// @author Zen.Liu
/// @since 2025-10-30


public interface Interceptors {


    interface LongInterceptor extends LongUnaryOperator {
        long apply(long v);

        @Override
        default long applyAsLong(long operand) {
            return apply(operand);
        }
    }

    interface ByteInterceptor extends IntUnaryOperator {
        byte apply(byte l);

        @Override
        default int applyAsInt(int operand) {
            return apply((byte) operand);
        }
    }

    interface ShortInterceptor extends IntUnaryOperator {
        short apply(short l);

        @Override
        default int applyAsInt(int operand) {
            return apply((short) operand);
        }
    }

    interface IntInterceptor extends IntUnaryOperator {
        int apply(int l);

        @Override
        default int applyAsInt(int operand) {
            return apply(operand);
        }
    }

    interface FloatInterceptor {
        float apply(float l);
    }

    interface DoubleInterceptor extends DoubleUnaryOperator {
        double apply(double l);

        @Override
        default double applyAsDouble(double operand) {
            return apply(operand);
        }
    }

}
