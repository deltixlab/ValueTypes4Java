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

import static org.junit.Assert.assertTrue;

/**
 * Stub for lambda usage test
 */
public class LambdaTestVtCapture {
    @Test
    @ValueTypeSuppressWarnings({"refCompare"})
    public void testTransformSucceeded() {
        assertTrue(Decimal64.fromDouble(42) == Decimal64.fromDouble(42));
    }


//    @Test
//    public void testDecimal64RunnableCaptureDecimal64() {
//
//        decimalResult = Decimal64.fromLong(0);
//        Decimal64 increment = Decimal64.fromLong(10000);
//        Runnable r = () -> decimalResult = decimalResult.add(increment);
//
//        for (int i = 0; i < 77; i++) {
//            r.run();
//        }
//
//        assertTrue(increment.equals(Decimal64.fromLong(10000)));
//        assertTrue(decimalResult.equals(Decimal64.fromLong(770000)));
//    }

    //@Test
    public void testDecimal64Lambda3() {
//        List<Integer> intArray =  Arrays.asList(1, 2, 3, 4, 5);
//
//        decimalResult = Decimal64.fromLong(0);
//        intArray.forEach(x -> decimalResult = decimalResult.add(Decimal64.fromLong(x)));
//
//        assertTrue(decimalResult.equals(Decimal64.fromLong(15)));
//
//        for (int i = 0; i < 9; i++) {
//            intArray.forEach(x -> decimalResult = decimalResult.add(Decimal64.fromLong(x)));
//        }
//
//        assertTrue(decimalResult.equals(Decimal64.fromLong(150)));
    }
}
