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

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class LambdaTestVtArgs {
    @Test
    @ValueTypeSuppressWarnings({"refCompare"})
    public void testTransformSucceeded() {
        assertTrue(Decimal64.fromDouble(42) == Decimal64.fromDouble(42));
    }

    @FunctionalInterface
    public interface Decimal64BiOperator {
        Decimal64 apply(Decimal64 a, Decimal64 b);
    }

    private static List<Integer> createTestArray() {
        return Arrays.asList(1, 2, 3, 4, 5);
    }

    @Test
    public void testDecimal64BiOperator() {

        Decimal64 d1 = Decimal64.ONE;
        Decimal64 d10 = Decimal64.TEN;

        Decimal64 decimalResult = Decimal64.fromLong(0);

        Decimal64BiOperator add = (x, y) -> x.add(y);
        Decimal64BiOperator sub = (x, y) -> x.subtract(y);

        assertTrue(d10.add(d1).equals(add.apply(d10, d1)));
        assertTrue(d10.add(d1).equals(add.apply(d1, d10)));
        assertTrue(add.apply(d10, d1).equals(d10.add(d1)));

        assertTrue(d10.subtract(d1).equals(sub.apply(d10, d1)));
        assertFalse(d10.subtract(d1).equals(sub.apply(d1, d10)));
        assertTrue(d1.subtract(d10).equals(sub.apply(d1, d10)));
        assertTrue(sub.apply(d10, d1).equals(d10.subtract(d1)));

        assertTrue(d1.equals(Decimal64.ONE));
        assertTrue(d10.equals(Decimal64.TEN));
    }
}
