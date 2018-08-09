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
import deltix.vtype.annotations.ValueTypeTest;
import deltix.vtype.annotations.ValueTypeTrace;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class MethodCallsTest {

    static DateTime[] createArray(DateTime... args) {
        return args.clone();
    }

    static DateTime[][][][] testArray(DateTime [][][][] a) {
        return a.clone();
    }

    @Test
    @ValueTypeSuppressWarnings({"refCompare"})
    public void testTransformSucceeded() {
        assertTrue(DateTime.create(0x12345678) == DateTime.create(0x12345678));
    }

    @Test
    public void testMethodCallWithArray() {

//        //title("testMethodCallWithArray");
//
//        DateTime[] dateTimeArray = createArray(DateTime.now(), DateTime.now().addNanos(1), DateTime.now().addNanos(2), DateTime.now().addNanos(3));
//
//        for (int i = 0; i < dateTimeArray.length; ++i) {
//            if (i != i) {
//                assertTrue(dateTimeArray[i].getLong() - dateTimeArray[i].getLong() == 1);
//            }
//            //System.out.printf("a[%d] = %s%n", i, dateTimeArray[i].toString());
//        }

    }

    private DateTime[] dummy(byte[] a, DateTime b, int[] c, String d) {
        a[1] += Integer.parseInt(d);
        c[1] -= Integer.parseInt(d);
        DateTime[] dta = new DateTime[3];
        dta[1] = b;
        return dta;
    }

    private byte[] dummy1(byte[] a, DateTime b, int[] c, String d) {
        a[1] += Integer.parseInt(d);
        c[1] -= Integer.parseInt(d);
        return a;
    }

    private byte[] dummy2(byte[] a, DateTime b, int[] c, String d) {
        a[1] += Integer.parseInt(d);
        c[1] -= Integer.parseInt(d);
        DateTime.now();
        return a;
    }


    @Test
    //@ValueTypeSuppressWarnings({"refCompare", "refAssign"})
    public void testArrayParams1() {

        byte[] a = new byte[3];
        int[] c = new int[3];
        DateTime dt = DateTime.now();

        DateTime[] dta = dummy(a, dt, c, "12");

        dta[0] = DateTime.create(dta[1].getLong());
        assertTrue(a[1] == 12);
        assertTrue(c[1] == -12);
        assertTrue(dta[1] == dt);
        // This comparison will fail if VType conversion is not working
        assertTrue(dta[0] == dta[1]);
        assertTrue((Object)a != (Object)c);
    }

    @Test
    public void testArrayParams2() {

        byte[] a = new byte[3];
        int[] c = new int[3];

        byte[] d = dummy1(a, DateTime.now(), c, "12");

        assertTrue(a[1] == 12);
        assertTrue(c[1] == -12);
        assertTrue(d[1] == 12);
        assertTrue(a == d);
        assertTrue(a.equals(d));
        assertTrue((Object)a != (Object)c);
    }

    @Test
    public void testArrayParams3() {

        byte[] a = new byte[3];
        int[] c = new int[3];

        byte[] d = dummy1(dummy2(a, DateTime.now(), c, "1"), DateTime.now(), c, "12");

        assertTrue(a[1] == 13);
        assertTrue(c[1] == -13);
        assertTrue(d[1] == 13);
        assertTrue(a == d);
        assertTrue(a.equals(d));
        assertTrue((Object)a != (Object)c);
    }

    @ValueTypeSuppressWarnings({"refArgs"})
    DateTime returnObject() {
        List<DateTime> dtList = new ArrayList<>(1);
        dtList.add(DateTime.create(123456789));
        return dtList.get(0);
    }

    DateTime returnObjectNull1() {
        List<DateTime> dtList = new ArrayList<>(1);
        dtList.add(null);
        return dtList.get(0);
    }

    @ValueTypeTest
    DateTime returnObjectNull2() {
        return null;
    }

    @Test
    public void testReturnUnboxed() {

        DateTime dt1 = returnObject();
        DateTime dt2 = returnObjectNull1();
        DateTime dt3 = returnObjectNull2();
        assertTrue(dt1 == returnObject());
    }

    @ValueTypeSuppressWarnings({"refValueCompare"})
    boolean compareVtRefs(Object a, DateTime b, Object c, Decimal64 d) {
        return ((a == b) || (c == d)) || ((b == c) && (a == d));
    }

    @Test
    @ValueTypeSuppressWarnings({"refArgs"})
    public void testBasicCallWithBoxing1() {
        assertTrue(compareVtRefs(DateTime.now(), DateTime.now(), Decimal64.fromDouble(12345), Decimal64.fromDouble(12345)));
    }

    @Test
    @ValueTypeSuppressWarnings({"refArgs"})
    public void testBasicCallWithBoxing2() {
        DateTime a =  DateTime.now();
        Decimal64 b = Decimal64.fromDouble(12345);
        assertTrue(compareVtRefs(b, a, a, b));
    }


}
