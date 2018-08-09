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

public class ArrayTest {

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
    @ValueTypeSuppressWarnings({"refCompare"})
    public void testArray1() {
        final DateTime[] array = new DateTime[64];
        for (int i = 0; i < array.length; i++) {
            array[i] = NOW;
        }

        assertEquals(array.length, 64);
        for (DateTime now : array) {
            assertTrue(now == NOW);
        }
    }

    @Test
    @ValueTypeSuppressWarnings({"refCompare"})
    public void testArray2() {
        final DateTime[][] array = new DateTime[64][64];
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                array[i][j] = NOW;
            }
        }

        assertEquals(array.length, 64);
        for (int i = 0; i < array.length; i++) {
            assertEquals(array[i].length, 64);

            for (int j = 0; j < array[i].length; j++) {
                assertTrue(array[i][j] == NOW);
            }
        }
    }

    @Test
    @ValueTypeSuppressWarnings({"refCompare"})
    public void testArrayInitialization() {
        DateTime[][] array = new DateTime[][] {{NOW, null, NOW}, {null}};
        assertTrue(2 == array.length);
        assertTrue(3 == array[0].length);
        assertTrue(1 == array[1].length);
        assertTrue(NOW == array[0][0]);
        assertTrue(NOW == array[0][2]);
        assertTrue(null == array[0][1]);
    }

}
