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

package deltix.vtype.common;

public abstract class Utils {
    /**
     * Reverse element order in a range within an array
     * @param a array
     * @param iFirst first Index
     * @param iLast last Index included
     */
    public static void reverse(int[] a, int iFirst, int iLast) {

        while (iFirst < iLast) {
            int t = a[iFirst];
            a[iFirst++] = a[iLast];
            a[iLast--] = t;
        }
    }

    /**
     * Reverse element order in a range within an array
     * @param a array
     * @param iFirst first Index
     * @param iLast last Index included
     */
    public static void reverse(Object[] a, int iFirst, int iLast) {

        while (iFirst < iLast) {
            Object t = a[iFirst];
            a[iFirst++] = a[iLast];
            a[iLast--] = t;
        }
    }

    public static void reverse(int[] a, int count) {
        reverse(a, 0, count - 1);
    }

    public static void reverse(Object[] a, int count) {
        reverse(a, 0, count - 1);
    }

    public static void reverse(int[] a) {
        reverse(a, a.length);
    }

    public static void reverse(Object[] a) {
        reverse(a, a.length);
    }

    public static void removeElement(final int array[], int iDeleted, int n) {

        for (int i = iDeleted + 1; i < n; ++i) {
            array[i - 1] = array[i];
        }
    }

    public static void removeElement(final Object array[], int iDeleted, int n) {

        for (int i = iDeleted + 1; i < n; ++i) {
            array[i - 1] = array[i];
        }
    }

    public static void insertElement(final int array[], int iInserted, int nBeforeInsertion) {

        for (int i = nBeforeInsertion; i > iInserted; --i) {
            array[i] = array[i - 1];
        }
    }

    public static void insertElement(final Object array[], int iInserted, int nBeforeInsertion) {

        for (int i = nBeforeInsertion; i > iInserted; --i) {
            array[i] = array[i - 1];
        }
    }
}
