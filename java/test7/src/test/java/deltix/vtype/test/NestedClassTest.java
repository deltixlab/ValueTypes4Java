package deltix.vtype.test;

import deltix.dfp.Decimal64;
import deltix.dt.DateTime;
import deltix.vtype.annotations.ValueTypeSuppressWarnings;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class NestedClassTest {
    private Decimal64 x;
    private static final Decimal64 xStatic = Decimal64.fromDouble(16);

    private static final class InnerClass {
        private Decimal64 x;
        private static final Decimal64 xStatic = Decimal64.fromDouble(4);

        private static final class InnerInnerClass {
            private Decimal64 x = Decimal64.fromDouble(1);

            public Decimal64 getValue() {
                return NestedClassTest.xStatic.add(InnerClass.xStatic).add(x);
            }

            @ValueTypeSuppressWarnings({"refCompare"})
            public boolean transformOk() {
                return getValue() == Decimal64.fromDouble(21);
            }
        }
    }

    @Test
    @ValueTypeSuppressWarnings({"refCompare"})
    public void testTransformSucceeded() {
        assertTrue(DateTime.create(0x12345678).addNanos(1) == DateTime.create(0x12345678).addNanos(1));
        assertTrue(new InnerClass.InnerInnerClass().transformOk());
    }

    @Test
    public void testResult() {
        Decimal64 y = new InnerClass.InnerInnerClass().getValue();
        assertTrue(y.toDouble() == 21);
        assertTrue(y == Decimal64.fromDouble(21));
    }
}