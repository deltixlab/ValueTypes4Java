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
import deltix.dt.DateTime;
import deltix.vtype.annotations.ValueTypeSuppressWarnings;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LambdaTestVtBasic {

    public long longResult;
    public Decimal64 decimalResult;

    @Test
    @ValueTypeSuppressWarnings({"refCompare"})
    public void testTransformSucceeded() {
        assertTrue(Decimal64.fromDouble(42) == Decimal64.fromDouble(42));
    }

    private static List<Integer> createTestArray() {
        return Arrays.asList(1, 2, 3, 4, 5);
    }

    @Test
    public void testDecimal64Lambda1() {

        Decimal64 d1 = Decimal64.HUNDRED;
        String s = "1234";
        List<Integer> intArray = createTestArray();
        long a = 42;
        Decimal64 d2 = Decimal64.MILLION;

        decimalResult = Decimal64.fromLong(0);
        intArray.forEach(x -> decimalResult = decimalResult.add(Decimal64.fromLong(x)));

        assertTrue(d1.equals(Decimal64.HUNDRED));
        assertTrue(d2.equals(Decimal64.MILLION));
        assertTrue(decimalResult.equals(Decimal64.fromLong(15)));

        for (int i = 0; i < 9; i++) {
            intArray.forEach(x -> decimalResult = decimalResult.add(Decimal64.fromLong(x)));
        }

        assertTrue(d1.equals(Decimal64.HUNDRED));
        assertTrue(d2.equals(Decimal64.MILLION));
        assertTrue(decimalResult.equals(Decimal64.fromLong(150)));
    }

    @Test
    public void testDecimal64RunnableCaptureLong() {

        decimalResult = Decimal64.fromLong(0);
        long incrementLong = 1000;
        Runnable r = () -> decimalResult = decimalResult.add(Decimal64.fromLong(incrementLong));

        for (int i = 0; i < 55; i++) {
            r.run();
        }

        assertTrue(incrementLong == 1000);
        assertTrue(decimalResult.equals(Decimal64.fromLong(55000)));
    }

    @Test
    public void testDecimal64IntUnaryOperator() {

        UnaryOperator<Long> percent = x -> Decimal64.toUnderlying(Decimal64.fromLong(x).divide(Decimal64.fromLong(100)));

        assertTrue(Decimal64.fromDouble(0.42).equals(Decimal64.fromUnderlying(percent.apply(42L))));
        assertTrue(Decimal64.fromUnderlying(percent.apply(42L)).equals(Decimal64.fromDouble(0.42)));
        assertFalse(Decimal64.fromDouble(0.423).equals(Decimal64.fromUnderlying(percent.apply(42L))));
        assertFalse(Decimal64.fromUnderlying(percent.apply(43L)).equals(Decimal64.fromDouble(0.42)));

        assertTrue(Decimal64.toUnderlying(Decimal64.fromDouble(0.423)) != percent.apply(42L));
    }
}
