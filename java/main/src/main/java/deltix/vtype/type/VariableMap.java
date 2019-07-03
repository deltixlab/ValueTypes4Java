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

public interface VariableMap {

    int topSrcAddr();

    int topDstAddr();

    int typeBySrcAddr(int aSrc);

    String nameBySrcAddr(int aSrc);

    String dbgNameBySrcAddr(int aSrc);

    int src2dstAddr(int aSrc);

    int nextSrcAddr(int varAddr);

    /**
     * @return number of variables excluding empty spaces!
     */
    int numVars();

    void clear();

    /**
     * Allocate a variable in FIFO fashion
     * @param typeId TypeId of the variable. dst part of the TypeId is analysed.
     * @return src address of the added var (address _before_ the transformation)
     */
    int add(int typeId, String name);

    /**
     * Add array of variable types
     * @param args array of TypeId ints (see TypeId class)
     */
    void add(int args[], String names[], int offset, int nArgs);

    int put(int aSrc, int typeId, String name);

    boolean replaceVarBySrcAddr(int aSrc, int typeId, String name);

    /**
     * Remove last registered variable(s) and incrementally adjust index tables accordingly,
     * @param n number of variables to remove
     * @return typeId of the last removed variable (variable with lowest index, if n &gt; 1)
     */
    int removeLast(int n);

    /**
     * Return src address of the last allocated variable, including empty ones. Return -1 if there is none
     */
    int getLastVarSrcAddr();

    int getAvailableDstSlotsAt(int aSrc);

    boolean isNewSrcAddr(int srcAddr);

    int adjustAddressSrc2Dst(int srcAddr);

    void dbgAddHole(int srcHoleSize, int dstHoleSize);

    /**
     *
     * @param begin start variable src address
     * @param end end variable src address
     * @return
     */
    TypeArray copyOfRange(int begin, int end);

    TypeArray copyAll();

    void saveTo(TypeArray dst);

    void restoreFrom(TypeArray src);

    void setFormatter(VariableNameFormatter fmt);

    @Override
    String toString();
}
