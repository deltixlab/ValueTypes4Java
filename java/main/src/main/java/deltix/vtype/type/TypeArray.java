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

package deltix.vtype.type;

import deltix.vtype.transformer.VariableNameDefaultFormatter;

public class TypeArray {
    private final VariableNameFormatter formatter;  // For debug string generation and logging etc.
    private int n;
    private int numAllocated;

    // These are package private for tests
    int[] types;
    String[] names;


    public TypeArray(int numReservedEntries, VariableNameFormatter formatter) {

        this.formatter = formatter;
        this.numAllocated = numReservedEntries;
        this.n = 0;
        this.types = new int[numReservedEntries];
        this.names = new String[numReservedEntries];
    }


    TypeArray(int typeId, String entryName) {

        this(1, VariableNameDefaultFormatter.get());
        this.n = 1;
        types[0] = typeId;
        names[0] = entryName;
    }


    TypeArray(int[] srcIds, String srcNames[], int offset, int count) {
        this(count, VariableNameDefaultFormatter.get());
        setInternal(0, srcIds, srcNames, offset, count);
        n = count;
    }


    void assign(int[] srcIds, String srcNames[], int offset, int count) {

        setElements(0, srcIds, srcNames, offset, count);
        n = count;
    }


    void setElements(int[] srcIds, String srcNames[], int offset, int count) {

        reserveEmpty(count);
        setInternal(0, srcIds, srcNames, offset, count);
    }


    void setElements(int dstOffset, int[] srcIds, String srcNames[], int offset, int count) {

        reserveEx(dstOffset + count, Math.min(n, dstOffset));
        n = Math.max(n , dstOffset + count);
        setInternal(dstOffset, srcIds, srcNames, offset, count);
    }

    void getElements(int srcOffset, int[] dstIds, String dstNames[], int dstOffset, int count) {

        getInternal(srcOffset, dstIds, dstNames, dstOffset, count);
    }


    private void realloc(int numReserve) {

        this.numAllocated = numReserve;
        this.types = new int[numReserve];
        this.names = new String[numReserve];
    }


    private void realloc(int numReserve, int numPreserve) {

        this.numAllocated = numReserve;
        int[] typesPrev = this.types;
        String[] namesPrev = this.names;
        int[] typesNew = this.types = new int[numReserve];
        String[] namesNew = this.names = new String[numReserve];

        System.arraycopy(typesPrev, 0, typesNew, 0, numPreserve);
        System.arraycopy(namesPrev, 0, namesNew, 0, numPreserve);
    }


    public int length() {
        return n;
    }

    /**
     * Reserve space for full overwrite
     * Reserve at least n entries, does not change size, does not guarantee that contents remain unchanged
     * @param numReserve
     */
    private void reserveEmpty(int numReserve) {

        if (numReserve > numAllocated) {
            // When growing, reserve more
            realloc(numReserve);
        }
    }


    private void reserveEx(int numReserve, int numPreserve) {

        assert(numPreserve <= numReserve);
        if (numReserve > numAllocated) {
            // When growing, reserve more
            realloc(numReserve, numPreserve);
        }
    }

    /**
     * Just like reserveEx, but with implied growth increment factor of 3/2 over the specified count
     * @param numReserve
     */
    private void reserve(int numReserve) {

        int numPreserve = n;
        if (numReserve > numAllocated) {
            // When growing, reserve more
            realloc(numReserve * 3 / 2, numPreserve);
        }
    }


    private void setInternal(int dstOffset, int[] srcIds, String srcNames[], int offset, int count) {
        // Set entries without resize/bounds checks and without changing of internal counts
        System.arraycopy(srcIds, offset, this.types, dstOffset, count);
        System.arraycopy(srcNames, offset, this.names, dstOffset, count);
    }

    /**
     * copy contents to external arrays
     * @param srcOffset
     * @param dstIds
     * @param dstNames
     * @param dstOffset
     * @param count
     */
    private void getInternal(int srcOffset, int[] dstIds, String dstNames[], int dstOffset, int count) {
        System.arraycopy(this.types, srcOffset, dstIds, dstOffset, count);
        System.arraycopy(this.names, srcOffset, dstNames, dstOffset, count);
    }

    public int getId(int i) {
        return types[i];
    }

    public String getName(int i) {
        return names[i];
    }

    private String getName(int typeId, String name) {

        return formatter.format(typeId, name, -1);
    }

    @Override
    public String toString() {
        StringBuffer o = new StringBuffer();
        o.append("{");
        for (int i = 0; i < n; i++) {
            o.append(String.format("%s%x:%s", i != 0 ? " " : "", types[i], getName(types[i], names[i])));
        }

        return o.append("}").toString();
    }


    public boolean equalTypes(TypeArray o) {
        if (n != o.length())
            return false;

        for (int i = 0, n = this.n; i < n; i++) {
            if (types[i] != o.types[i])
                return false;
        }

        return true;
    }


    public void clear() {
        n = 0;
    }


    public void add(int typeId, String name) {

        int i = this.n;
        reserve(i + 1);
        this.n = i + 1;
        types[i] = typeId;
        names[i] = name;
    }

    public int[] getTypes() {
        return types;
    }

    public String[] getNames() {
        return names;
    }
}
