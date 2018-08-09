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
import deltix.dfp.Decimal64Utils;
import deltix.dt.DateTime;
import deltix.vtype.annotations.ValueTypeSuppressWarnings;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AutoInitTest {

    private static final DateTime NOW = DateTime.now();

    DateTime a, b;
    final DateTime c = null;
    static DateTime a1, b1, c1;

    Decimal64 d0, e0;
    static final Decimal64 d1 = Decimal64.HUNDRED, e1 = Decimal64.NaN;

    Decimal64 arrayA[] = new Decimal64[13];
    Decimal64 arrayB[];
    static Decimal64 arrayStaticA[] = new Decimal64[13];
    static Decimal64 arrayStaticB[] = new Decimal64[8];

    Decimal64 arrayMultiA[][][] = new Decimal64[8][4][2];
    Decimal64 arrayMultiB[][][];
    Decimal64 arrayMultiC[][];

    // TODO: Add cases with non-null with value = nullvalue
    // TODO: Add cases for null reference exception
    // (To another test class probably, or rename this to NullTest)
    public AutoInitTest() {

        arrayA[3] = Decimal64.fromDouble(123.45);
        arrayB = new Decimal64[13];
        arrayMultiB = new Decimal64[5][4][];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 2/* !! */; j++) {
                arrayMultiB[i][j] = new Decimal64[10];
            }
        }
    }

    @BeforeClass
    public static void setup() {
        AllocationDetector.install();
    }

    @AfterClass
    public static void tearDown() {
        AllocationDetector.uninstall();
    }

    @Test
    @ValueTypeSuppressWarnings({"refCompare"})
    public void testTransformSucceeded() {
        assertTrue(DateTime.create(0x12345678) == DateTime.create(0x12345678));
    }


    class AutoInitEmpty {
        DateTime a, b, c;
    }

    @Test
    @ValueTypeSuppressWarnings({"refCompare"})
    public void testAutoNullInit() {
        assertTrue(a == null);
        assertTrue(b == null);
        assertTrue(c == null);
        assertTrue(b == a);
        assertTrue(a == c);

        assertTrue(d0 == null);
        assertTrue(e0 == null);

        if (Decimal64.NaN == null) {
            assertTrue(d0 == Decimal64.NaN);
            assertTrue(e0 == Decimal64.NaN);
        }
    }

    @Test
    public void testAutoNullInitEmpty() {

        AutoInitEmpty cl = new AutoInitEmpty();
        assertTrue(cl.a == null);
        assertTrue(cl.b == null);
        assertTrue(cl.c == null);
    }

    @Test
    public void testAutoNullInitStatic() {
        assertFalse(a1 == DateTime.create(0));
        assertTrue(a1 == null);
        assertTrue(b1 == null);
        assertTrue(c1 == null);
        assertTrue(b1 == a1);
        assertTrue(a1 == c1);

        assertTrue(d1 != null);

        assertTrue(d1 == Decimal64.HUNDRED);
        assertTrue(d1 != Decimal64.NaN);
        assertTrue(d1 != null);

        // NOTE:
        // In new impl Nan == NaN since 2018-06-16
        // Also in new impl '==' is turned into isIdentical() since 2018-06-18
        assertTrue(e1 == e1);
        assertTrue(e1 == Decimal64.NaN);
        assertTrue(Decimal64.NaN == Decimal64.NaN);

        if (Decimal64.NaN == null) {
            assertTrue(e1 == null);
        }
    }

    @Test
    public void testAutoNullInitArray() {

        assertFalse(arrayA == null);
        assertTrue(arrayA[2] == null);
        assertTrue(arrayA[3] != null);
        assertFalse(Decimal64Utils.isNull(Decimal64.toUnderlying(arrayA[3])));
        assertFalse(arrayB == null);
        assertTrue(arrayB[2] == null);
    }

    // TODO: Multiarray null initialization is not implemented yet. Need API finalization. Low priority.
    //@Test
    public void testAutoNullMultiArray() {

        assertTrue(arrayMultiA[1][1][1] == null);
        // TODO: Expand

        assertTrue(arrayMultiB[1][1][1] == null);
        assertTrue(arrayMultiC == null);
    }
}
