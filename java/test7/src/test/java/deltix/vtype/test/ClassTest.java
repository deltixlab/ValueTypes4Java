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

import deltix.dt.DateTime;
import deltix.vtype.annotations.ValueTypeSuppressWarnings;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ClassTest {

    private static final DateTime NOW = DateTime.now();

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

    @Test
    public void testConstructors1() {
        Clazz clazz = new Clazz(new DateTime[][]{{NOW, null, NOW}, {null}});
    }

    @Test
    @ValueTypeSuppressWarnings({"refCompare"})
    public void testConstructors() {
        Clazz clazz = new Clazz();
        DateTime dt = clazz.field;
        assertTrue(dt == null);
        assertTrue(clazz.field == null);
        assertTrue(clazz.array1 == null);
        assertTrue(clazz.array2 == null);

        clazz = new Clazz((DateTime) null);
        assertTrue(clazz.field == null);
        assertTrue(clazz.array1 == null);
        assertTrue(clazz.array2 == null);

        clazz = new Clazz(NOW);
        assertTrue(clazz.field == NOW);
        assertTrue(clazz.array1 == null);
        assertTrue(clazz.array2 == null);

        clazz = new Clazz((DateTime[]) null);
        assertTrue(clazz.field == null);
        assertTrue(clazz.array1 == null);
        assertTrue(clazz.array2 == null);

        clazz = new Clazz(NOW, null, NOW);
        assertTrue(clazz.field == null);
        assertTrue(clazz.array2 == null);
        assertTrue(clazz.array1 != null);
        assertEquals(3, clazz.array1.length);
        assertTrue(clazz.array1[0] == NOW);
        assertTrue(clazz.array1[1] == null);
        assertTrue(clazz.array1[2] == NOW);

        clazz = new Clazz((DateTime[][]) null);
        assertTrue(clazz.field == null);
        assertTrue(clazz.array1 == null);
        assertTrue(clazz.array2 == null);

        clazz = new Clazz(new DateTime[][]{{NOW, null, NOW}, {null}});
        assertTrue(clazz.field == null);
        assertTrue(clazz.array1 == null);
        assertTrue(clazz.array2 != null);
        assertTrue(clazz.array2.length == 2);
        assertTrue(clazz.array2[0].length == 3);
        assertTrue(clazz.array2[1].length == 1);
        assertTrue(clazz.array2[0][0] == NOW);
        assertTrue(clazz.array2[0][1] == null);
        assertTrue(clazz.array2[0][2] == NOW);
        assertTrue(clazz.array2[1][0] == null);
    }

    @Test
    @ValueTypeSuppressWarnings({"refCompare"})
    public void testFields() {
        final Clazz clazz = new Clazz();

        clazz.field = null;
        assertTrue(clazz.field == null);

        clazz.field = NOW;
        assertTrue(clazz.field == NOW);

        clazz.array1 = null;
        assertTrue(clazz.array1 == null);

        clazz.array1 = new DateTime[]{NOW, null};
        assertTrue(clazz.array1 != null);
        assertEquals(2, clazz.array1.length);
        assertTrue(clazz.array1[0] != null);
        assertTrue(clazz.array1[1] == null);

        clazz.array2 = null;
        assertTrue(clazz.array2 == null);

        clazz.array2 = new DateTime[][]{{null, NOW}, {null}};
        assertTrue(clazz.array2 != null);
        assertEquals(2, clazz.array2.length);
        assertEquals(2, clazz.array2[0].length);
        assertEquals(1, clazz.array2[1].length);
        assertTrue(clazz.array2[0][0] == null);
        assertTrue(clazz.array2[0][1] == NOW);
        assertTrue(clazz.array2[1][0] == null);
    }

    @Test
    @ValueTypeSuppressWarnings({"refCompare"})
    public void testMethods() {
        final Clazz clazz = new Clazz();

        clazz.setField(null);
        assertTrue(clazz.getField() == null);

        clazz.setField(NOW);
        assertTrue(clazz.getField() == NOW);

        clazz.setArray1((DateTime[]) null);
        assertTrue(clazz.getArray1() == null);

        clazz.setArray1(NOW, null);
        assertTrue(clazz.getArray1() != null);
        assertEquals(2, clazz.getArray1().length);
        assertTrue(clazz.getArray1()[0] != null);
        assertTrue(clazz.getArray1()[1] == null);

        clazz.setArray2(null);
        assertTrue(clazz.getArray2() == null);

        clazz.setArray2(new DateTime[][]{{null, NOW}, {null}});
        assertTrue(clazz.getArray2() != null);
        assertEquals(2, clazz.getArray2().length);
        assertEquals(2, clazz.getArray2()[0].length);
        assertEquals(1, clazz.getArray2()[1].length);
        assertTrue(clazz.getArray2()[0][0] == null);
        assertTrue(clazz.getArray2()[0][1] == NOW);
        assertTrue(clazz.getArray2()[1][0] == null);
    }

    private static final class Clazz {

        private DateTime field = null;  // NOTE: not initialized scalar VT fields are automatically set to null
        private DateTime[] array1;
        private DateTime[][] array2;

        public Clazz() {
        }

        public Clazz(final DateTime field) {
            this.field = field;
        }

        public Clazz(final DateTime... array1) {
            this.array1 = array1;
        }

        public Clazz(final DateTime[][] array2) {
            this.array2 = array2;
        }

        public DateTime getField() {
            return field;
        }

        public void setField(final DateTime field) {
            this.field = field;
        }

        public DateTime[] getArray1() {
            return array1;
        }

        public void setArray1(final DateTime... array1) {
            this.array1 = array1;
        }

        public DateTime[][] getArray2() {
            return array2;
        }

        public void setArray2(final DateTime[][] array2) {
            this.array2 = array2;
        }

    }

}
