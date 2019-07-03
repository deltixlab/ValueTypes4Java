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

import static org.junit.Assert.assertTrue;


/**
 * Test very basic 1-arg lambdas with or without capture of the local variables
 * NOTE: these tests are doing bogus asserts to increase complexity and ensure validity of the transformed bytecode
 * ValueType vars are present in order to force this code through the Value Type transformation
 */
public class LambdaTestBasic {
    @Test
    @ValueTypeSuppressWarnings({"refCompare"})
    public void testTransformSucceeded() {
        assertTrue(DateTime.create(0x12345678) == DateTime.create(0x12345678));
    }

    public static void staticConsume3(Object x, Object y, Object z) {
    }

    public static void staticConsume3(Object x, long y, Object z) {
    }

    public static void staticConsumeObject(Object x) {
    }

    public static void staticConsumeInteger(Integer x) {
    }

    public void consume3(Object x, Object y, Object z) {
    }

    public void consume3L(long x, Object y, Object z) {
    }

    public void consumeObject(Object x) {
    }

    public void consumeInteger(Integer x) {
    }

    @Test
    public void testBasicStaticLambda1() {

        Decimal64 d1 = Decimal64.HUNDRED;
        List intArray =  Arrays.asList(1, 2, 3, 4, 5);
        Decimal64 d2 = Decimal64.MILLION;

        intArray.forEach(x -> System.out.print(x));

        assertTrue(d1.equals(Decimal64.HUNDRED));
        assertTrue(d2.equals(Decimal64.MILLION));

        intArray.forEach(x -> staticConsumeInteger((Integer)x));

        assertTrue(d1.equals(Decimal64.HUNDRED));
        assertTrue(d2.equals(Decimal64.MILLION));

        intArray.forEach(LambdaTestBasic::staticConsumeObject);
        for (int i = 0; i < 13; i++) {
            intArray.forEach(LambdaTestBasic::staticConsumeObject);
        }

        assertTrue(d1.equals(Decimal64.HUNDRED));
        assertTrue(d2.equals(Decimal64.MILLION));
    }


    @Test
    public void testBasicStaticLambda2() {

        Decimal64 d1 = Decimal64.HUNDRED;
        String s = "1234";
        List intArray =  Arrays.asList(1, 2, 3, 4, 5);
        long a = 42;
        Decimal64 d2 = Decimal64.MILLION;

        intArray.forEach(x -> staticConsume3(x, a, s));

        assertTrue(d1.equals(Decimal64.HUNDRED));
        assertTrue(d2.equals(Decimal64.MILLION));

        intArray.forEach(x -> staticConsume3(a, x, s));

        assertTrue(d1.equals(Decimal64.HUNDRED));
        assertTrue(d2.equals(Decimal64.MILLION));
    }

    @Test
    public void testBasicNonStaticLambda1() {

        Decimal64 d1 = Decimal64.HUNDRED;
        List intArray =  Arrays.asList(1, 2, 3, 4, 5);
        Decimal64 d2 = Decimal64.MILLION;

        intArray.forEach(x -> consumeInteger((Integer)x));

        assertTrue(d1.equals(Decimal64.HUNDRED));
        assertTrue(d2.equals(Decimal64.MILLION));

        intArray.forEach(this::consumeObject);

        assertTrue(d1.equals(Decimal64.HUNDRED));
        assertTrue(d2.equals(Decimal64.MILLION));
    }

    @Test
    public void testBasicNonStaticLambda2() {

        Decimal64 d1 = Decimal64.HUNDRED;
        String s = "1234";
        List intArray =  Arrays.asList(1, 2, 3, 4, 5);
        long l = 42;
        Decimal64 d2 = Decimal64.MILLION;

        // Despite visibly different arg order in these lambda invocations, actual generated order will be the same
        // Could possibly be different for some cases only, such as swapped captured args
        intArray.forEach(x -> consume3(x, l, s));

        assertTrue(d1.equals(Decimal64.HUNDRED));
        assertTrue(d2.equals(Decimal64.MILLION));

        intArray.forEach(x -> consume3L(l, x, s));
        for (int i = 0; i < 13; i++) {
            intArray.forEach(x -> consume3L(l, x, s));
        }

        assertTrue(d1.equals(Decimal64.HUNDRED));
        assertTrue(d2.equals(Decimal64.MILLION));
    }
}
