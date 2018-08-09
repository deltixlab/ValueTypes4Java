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
import org.junit.Ignore;
import org.junit.Test;

import java.util.function.UnaryOperator;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

/**
 * Stub for lambda usage test
 */
public class LambdaTestVt {

    @Test
    @ValueTypeSuppressWarnings({"refCompare"})
    public void testTransformSucceeded() {
        assertTrue(Decimal64.fromDouble(42) == Decimal64.fromDouble(42));
    }

    @FunctionalInterface
    public interface ReturnDecimal64 {
        Decimal64 toDecimal(int x);
    }

    // TODO: Overloading Generic Functional Interfaces with ValueType will be supported later
    @Ignore
    @Test
    public void testDecimal64UnaryOperator() {

        UnaryOperator<Decimal64> percent = x -> x.divide(Decimal64.fromLong(100));

        assertTrue(Decimal64.fromDouble(0.42).equals(percent.apply(Decimal64.fromLong(42L))));
        assertTrue(percent.apply(Decimal64.fromLong(42L)).equals(Decimal64.fromDouble(0.42)));
        assertFalse(Decimal64.fromDouble(0.423).equals(percent.apply(Decimal64.fromLong(42L))));
        assertFalse(percent.apply(Decimal64.fromLong(43L)).equals(Decimal64.fromDouble(0.42)));

    }
}