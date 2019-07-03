/*
 * Copyright 2017-2018 Deltix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package deltix.vtype.test;

import deltix.dfp.Decimal64;
import deltix.vtype.annotations.ValueTypeSuppressWarnings;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LambdaTestVtReturn {

    @Test
    @ValueTypeSuppressWarnings({"refCompare"})
    public void testTransformSucceeded() {
        assertTrue(Decimal64.fromDouble(42) == Decimal64.fromDouble(42));
    }

    @FunctionalInterface
    public interface ReturnDecimal64 {
        Decimal64 toDecimal(int x);
    }

    private static List<Integer> createTestArray() {
        return Arrays.asList(1, 2, 3, 4, 5);
    }

    @Test
    public void testDecimal64Return1() {

        Decimal64 d1 = Decimal64.HUNDRED;
        Decimal64 d2 = Decimal64.MILLION;

        Decimal64 decimalResult = Decimal64.fromLong(0);

        ReturnDecimal64 percent = (int x) -> Decimal64.fromLong(x).divide(Decimal64.fromLong(100));
        ReturnDecimal64 percent2 = x -> Decimal64.fromLong(x).divide(Decimal64.fromLong(100));

        assertTrue(Decimal64.fromDouble(0.42).equals(percent.toDecimal(42)));
        assertTrue(percent.toDecimal(42).equals(Decimal64.fromDouble(0.42)));
        assertTrue(Decimal64.fromLong(0).equals(percent.toDecimal(0)));
        assertTrue(percent.toDecimal(0).equals(Decimal64.fromLong(0)));
        assertTrue(percent.toDecimal(42).equals(percent2.toDecimal(42)));

        assertFalse(Decimal64.fromDouble(0.421).equals(percent.toDecimal(42)));
        assertFalse(percent.toDecimal(421).equals(Decimal64.fromDouble(0.42)));

        assertTrue(d1.equals(Decimal64.HUNDRED));
        assertTrue(d2.equals(Decimal64.MILLION));
    }


    @Test
    public void testDecimal64Return2() {

        String a = "1234";
        double b = 0.5;
        long c = 42;
        int d = 321;
        boolean e = true;
        Decimal64 d1 = Decimal64.HUNDRED;
        Decimal64 d2 = Decimal64.MILLION;

        ReturnDecimal64 percent = x -> a.length() < 100 && b < 999 && c < 999 && d < 999 && e
                ? Decimal64.fromLong(x).divide(Decimal64.fromLong(100)) : Decimal64.NaN;

        assertTrue(Decimal64.fromDouble(0.42).equals(percent.toDecimal(42)));
        assertTrue(percent.toDecimal(42).equals(Decimal64.fromDouble(0.42)));
        assertTrue(Decimal64.fromLong(0).equals(percent.toDecimal(0)));
        assertTrue(percent.toDecimal(0).equals(Decimal64.fromLong(0)));

        assertFalse(Decimal64.fromDouble(0.421).equals(percent.toDecimal(42)));
        assertFalse(percent.toDecimal(421).equals(Decimal64.fromDouble(0.42)));

        assertTrue(d1.equals(Decimal64.HUNDRED));
        assertTrue(d2.equals(Decimal64.MILLION));
    }
}
