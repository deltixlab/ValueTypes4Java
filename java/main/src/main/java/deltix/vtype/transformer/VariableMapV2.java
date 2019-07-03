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

package deltix.vtype.transformer;

import deltix.vtype.type.TypeId;
import deltix.vtype.type.VariableMap;
import deltix.vtype.type.VariableNameFormatter;
import deltix.vtype.type.TypeArray;

import static java.util.Arrays.copyOf;

public class VariableMapV2 implements VariableMap, VariableMapDbg {

    private static final int E              = Integer.MIN_VALUE;
    private static final int EMPTY          = -1;
    private static final int INVALID_ADDR   = -1;
    private static final int MAX_VT_SLOTS   = 0x7FFF;


    private VariableNameFormatter formatter;
    protected final int INCREMENT32;
    protected static final int INCREMENT64 = 2;

    // If true, allocate 2 slots for each variable after translation, disregarding its type
    protected final boolean use2x;

    // List of currently allocated variables, accessed by original src addr
    protected int types[];
    protected String names[];

    // Tables for converting JVM variable index(slot address) to/from logical index
    // Incrementally updated when adding variables
    // Can be regenerated from scratch by calling reindex()

    int src2dstAddr[];  // Used to determine actual var dst addr after transformation, OR its "head" src addr if < 0
    int dst2srcAddr[];  // Used to determine what var is is occupying a given slot in transformed var frame

    // Number of used variables
    int numUsedVars;
    int numEmptyVars;

    // Number of allocated variable slots
    int numSrcAllocated, numDstAllocated;

    // Number of used index records
    int topSrcAddr, topDstAddr;

    VariableMapV2(VariableNameFormatter formatter, boolean use2x, int numVarsToReserve) {

        this.formatter = formatter;
        this.use2x = use2x;
        this.INCREMENT32 = use2x ? 2 : 1;
        numSrcAllocated = numVarsToReserve > 4 ? numVarsToReserve : 4;
        numDstAllocated = numVarsToReserve > 4 ? numVarsToReserve : 4;
        types = new int[numSrcAllocated * 2];
        names = new String[numSrcAllocated * 2];
        src2dstAddr = new int[numSrcAllocated * 2];
        dst2srcAddr = new int[numSrcAllocated * 2];
        clear();
    }


    public VariableMapV2(VariableNameFormatter formatter) {
        this(formatter, false, 32);
    }


    /**
     *
     * @param addr value from src2dst or dst2src array entry
     * @return true if the src/dst slot is allocated for the tail part of a 64 bit (or bigger, if VT) variable
     */
    private boolean isTail(int addr) {
        return addr < EMPTY;
    }

    /**
     * @param addr value from src2dst or dst2src array entry
     * @return true if the src/dst slot is allocated for the 32-bit var, or head part of a bigger one
     * note: it equals its src address
     */
    private boolean isHead(int addr) {
        return addr >= 0;
    }

    /**
     * @param addr value from src2dst or dst2src array
     * @return true == EMPTY.
     * EMPTY is a special state only available for dst slots that are not being directly referenced by any src slot
     * EMPTY slots only exist to provide extra var frame space for transformed variables
     */
    private boolean isEmpty(int addr) {
        return EMPTY == addr;
    }

    /**
     * return Head src address of the owner of the specified tail cell
     * @param tail value from src2dst or dst2src array entry
     * @return src address of the var owning this slot
     */
    private int head(int tail) {
        return tail & ~E;
    }

    @Override
    public int numVars() {
        return numUsedVars + numEmptyVars;
    }

    @Override
    public int topSrcAddr() {
        return this.topSrcAddr;
    }

    @Override
    public int topDstAddr() {
        return this.topDstAddr;
    }

    @Override
    public int typeBySrcAddr(int aSrc) {

        return types[validateSrcAddr(aSrc)];
    }

    @Override
    public String nameBySrcAddr(int aSrc) {

        return names[validateSrcAddr(aSrc)];
    }

    @Override
    public String dbgNameBySrcAddr(int aSrc) {

        validateSrcAddr(aSrc);
        return getName(types[aSrc], names[aSrc], aSrc);
    }

    @Override
    public boolean isNewSrcAddr(int srcAddr) {

        return srcAddr >= topSrcAddr;
    }

    @Override
    public int src2dstAddr(int aSrc) {
        return src2dstAddr[validateSrcAddr(aSrc)];
    }

    @Override
    public int nextSrcAddr(int varAddr) {
        return varAddr + TypeId.size32Src(types[varAddr]);
    }

    @Override
    public int getLastVarSrcAddr() {

        int topSrcAddr = this.topSrcAddr;
        if (0 == topSrcAddr)
            return INVALID_ADDR;

        int aSrc = topSrcAddr - 1;
        int aDst = src2dstAddr[aSrc];

        // If this code gives out of bounds exception, it is not a bug but a logic error in another place
        if (isEmpty(aDst))
            throw new IllegalStateException("src2dst table can't return EMPTY dst slot");

        // TODO: Remove after debugging
        //if (!isHead(src2dstAddr[aSrc])) {
        //throw new IllegalStateException("Should be a HEAD entry");
        //}

        assert (isHead(aDst) || (isHead(src2dstAddr[aSrc - 1])));
        return isHead(aDst) ? aSrc : aSrc - 1;
    }

    @Override
    public void clear() {
        numUsedVars = numEmptyVars = topSrcAddr = topDstAddr = 0;
    }

    private void checkAllocSrc(int topSrc) {

        if (topSrc > numSrcAllocated) {
            do {
                numSrcAllocated = numSrcAllocated * 5 / 2;
            } while (topSrc > numSrcAllocated);

            types = copyOf(types, numSrcAllocated);
            names = copyOf(names, numSrcAllocated);
            src2dstAddr = copyOf(src2dstAddr, numSrcAllocated);
        }
    }


    private void checkAllocDst(int topDst) {

        if (topDst > numDstAllocated) {
            do {
                numDstAllocated = numDstAllocated * 5 / 2;
            } while (topDst > numDstAllocated);

            dst2srcAddr = copyOf(dst2srcAddr, numDstAllocated);
        }
    }

    /**
     * Check allocation (grow all arrays, if needed)
     * @param topSrc number of requested src2dst entries
     * @param topDst number of requested dst2src entries
     */
    private void checkAlloc(int topSrc, int topDst) {

        checkAllocSrc(topSrc);
        checkAllocDst(topDst);
    }

    /**
     *
     * @param typeId
     * @return number of 32-bit words taken by the type described by typeId after transformation
     */
    protected int dstTypeSize(int typeId) {

        return TypeId.isDst64(typeId) ? INCREMENT64 : INCREMENT32;
        // NOTE: Change here for support of arbitrary size Value Types
    }

    /**
     * Update indices for a type that is longer than 1 slot
     * @param typeId
     * @param aSrc
     * @param aDst
     * @param tailValue
     * @return number of slots that changed state
     */
    private int updateTail(int aSrc, int aDst, int typeId, int tailValue) {

        if (TypeId.isSrc64(typeId)) {
            assert(TypeId.isDst64(typeId));
            src2dstAddr[aSrc + 1] = aSrc + E;
            dst2srcAddr[aDst + 1] = aSrc + E + 1;
            types[aSrc + 1] = TypeId.VOID;
            names[aSrc + 1] = null;
            return 1;
        } else if (TypeId.isVtValue(typeId)) {
            int structSize = dstTypeSize(typeId);
            for (int i = 1; i < structSize; i++) {
                dst2srcAddr[aDst + i] = tailValue;
            }

        } else if (use2x) {
            // 1 empty var slot after every 1-slot var
            dst2srcAddr[aDst + 1] = tailValue;
        }

        return 0;
    }

    /**
     * remove variable starting at address
     * does not modify top src/dst addresses, but may modify used/free var counters
     * @param aSrc source address of the variable
     */
    private void eraseNonEmptyVar(int aSrc) {

        assert(aSrc >= 0 && aSrc < topSrcAddr);
        int aDst = src2dstAddr[aSrc];
        // Empty?
        if (!isHead(aDst)) {
            throw new IllegalArgumentException("Trying to erase variable at wrong index");
        }

        // Total number of vars is not changed or actually grows due to bigger types being split into multiple 32-bit slots
        --numUsedVars;

        int typeId = types[aSrc];
        types[aSrc] = TypeId.VOID;
        names[aSrc] = null;
        // src2dstAddr[aSrc] already has the expected value
        dst2srcAddr[aDst] = aSrc;

        ++numEmptyVars;
        if (TypeId.isSrc64(typeId)) {
            ++numEmptyVars;
            types[aSrc + 1] = TypeId.VOID;
            names[aSrc + 1] = null;
            assert(head(dst2srcAddr[aDst + 1]) == aSrc + 1);
            src2dstAddr[aSrc + 1] = aDst + 1;
            dst2srcAddr[aDst + 1] = aSrc + 1;
        } else if (TypeId.isVtValue(typeId)) {
            int structSize = dstTypeSize(typeId);
            for (int i = 1; i < structSize; i++) {
                dst2srcAddr[aDst + i] = EMPTY;
            }

        } else if (use2x) {
            // 1 empty var slot after every 1-slot var
            assert(head(dst2srcAddr[aDst + 1]) == aSrc || isEmpty(dst2srcAddr[aDst + 1]));
            dst2srcAddr[aDst + 1] = EMPTY;
        }
    }

    /**
     * check, if a variable occupies the specified address, erase, if needed
     * does not modify top src/dst addresses, but may modify used/free var counters
     * @param aSrc source address of the variable
     */
    private void eraseAddress(int aSrc) {

        int aDst = src2dstAddr[aSrc];
        if (EMPTY == aDst)
            return;

        // Tail?
        if (!isHead(aDst)) {
            assert (aSrc > 0 && aDst != EMPTY);
            aSrc = aDst & ~E; // Obtain head address of the var being erased
        } else  {
            if (TypeId.VOID == types[aSrc])
                return;
        }

        assert(src2dstAddr[aSrc] >= 0);
        eraseNonEmptyVar(aSrc);
    }


//    private void tryEraseVar(int addr) {
//
//        // TODO: Incomplete
//        int aDst;
//        if (EMPTY == (aDst = src2dstAddr[addr]))
//            return;
//
//        while(aDst < 0) {
//            assert (addr > 0 && aDst != EMPTY);
//            aDst = src2dstAddr[--addr];
//        }
//
//        eraseNonEmptyVar(addr);
//    }

    protected int writeVarData(int aSrc, int aDst, int typeId, String name) {
        types[aSrc] = typeId;
        names[aSrc] = name;
        src2dstAddr[aSrc] = aDst;
        dst2srcAddr[aDst] = aSrc;
        // Fill remaining slots, if necessary, update number of set/cleared vars
        return updateTail(aSrc, aDst, typeId, aSrc + E);
    }

    /**
     * Write a variable data. Will overwrite one or more preceding vars
     * but aDst address must be valid and there should be enough space (can't require shifting of dst address of existing vars)
     * @param aSrc
     * @param aDst
     * @param typeId
     * @param name
     * @return
     */
    //@Override

    protected int putVar(int aSrc, int aDst, int typeId, String name) {

        assert(aSrc >= 0 && aSrc <= topSrcAddr);
        assert(aDst >= 0 && aDst <= topDstAddr);
        assert(aDst >= aSrc); // We can't allocate less dst entries than src entries

        // We can overwrite/erase 0..2 existing variables, depending on store position,
        // maximum of 4 src cells and max(4, vtSizeA + vtSizeB) dst cells

        int dstSize = dstTypeSize(typeId);
        int srcSize = TypeId.size32Src(typeId);

        // Grow top?
        if (aSrc + srcSize > topSrcAddr) {
            if (aSrc < topSrcAddr) {
                eraseAddress(aSrc); // Note that this should have adjusted topDstAddr
                aDst = src2dstAddr[aSrc];
                assert(aDst >= 0);
                assert(aDst < topDstAddr);
            } else
                assert(topDstAddr == aDst);

            // new variable, grow the top
            int newTopSrc = aSrc + srcSize;
            numEmptyVars += (newTopSrc - topSrcAddr);
            topSrcAddr = newTopSrc;
            topDstAddr = aDst + dstSize;
            checkAlloc(topSrcAddr, topDstAddr);
        } else {
            eraseAddress(aSrc);
            if (srcSize == 2) {
                eraseAddress(aSrc + 1);
            }

            if (aDst + dstSize > topDstAddr) {
                topDstAddr = aDst + dstSize;
                checkAlloc(topSrcAddr, topDstAddr);
            }
        }

        ++numUsedVars;
        numEmptyVars -= 1 + writeVarData(aSrc, aDst, typeId, name);
        return aSrc;
    }


    @Override
    public int getAvailableDstSlotsAt(int aSrc) {
        if (aSrc < 0)
            return -1;

        // Last var can be expanded as much as necessary
        if (aSrc >= topSrcAddr - 1)
            return MAX_VT_SLOTS;

        int n = 0;
        int aDst0 = src2dstAddr[aSrc];
        int aSrc1 = aSrc + 1;
        if (!isHead(aDst0)) {
            // tail of 64-bit var
            aDst0 = src2dstAddr[--aSrc];
            n = INCREMENT32;
        }

        assert(isHead(aDst0));
        int aDst1 = src2dstAddr[aSrc1];
        if (!isHead(aDst1)) {
            // head of 64-bit var
            aDst1 = src2dstAddr[++aSrc1];
            n += INCREMENT32;
        }

        // TODO: possible bug whan use2x == true. Recheck.
        assert(isHead(aDst1));
        assert(aDst1 - aDst0 - n > 0);
        return aDst1 - aDst0 - n;
    }

    /**
     * Allocate a variable in FIFO fashion
     * @param typeId TypeId of the variable. dst part of the TypeId is analysed.
     * @return Src Addr of the added variable
     */
    @Override
    public int add(int typeId, String name) {

        return putVar(this.topSrcAddr, this.topDstAddr, typeId, name);
    }

    /**
     * Add an array of variable types
     * @param args array of TypeId ints (see TypeId class)
     */
    public void add(int args[], String names[], int offset, int nArgs) {
        for (int i = offset; i < offset + nArgs; i++) {
            add(args[i], null != names ? names[i] : null);
        }
    }

    /**
     * Remove last registered variable(s)
     * @param n number of variables to remove
     * @return typeId of the last removed variable (variable with lowest index, if n > 1)
     */
    public int removeLast(int n) {

        int typeId = TypeId.VOID;
        for (; n != 0; --n) {
            int aSrc = getLastVarSrcAddr();
            assert(aSrc >= 0);
            typeId = types[aSrc];
            if (TypeId.VOID == typeId) {
                --numEmptyVars;
            } else {
                --numUsedVars;
            }

            int aDst = src2dstAddr(aSrc);
            assert(isHead(aDst));
            topSrcAddr = aSrc;
            topDstAddr = aDst;
        }

        return typeId;
    }

    // TODO:
    public void dbgAddHole(int srcHoleSize, int dstHoleSize) {

        int aSrc = this.topSrcAddr;
        int aDst = this.topDstAddr;
        this.topSrcAddr = aSrc + srcHoleSize;
        this.topDstAddr = aDst + dstHoleSize;
        checkAlloc(aSrc + srcHoleSize, aDst + dstHoleSize);
        for (int i = aSrc, n = aSrc + srcHoleSize; i < n; ++i) {
            types[i] = TypeId.VOID;
            names[i] = null;
            src2dstAddr[i] = EMPTY;
        }

        for (int i = aDst, n = aDst + dstHoleSize; i < n; ++i) {
            dst2srcAddr[i] = EMPTY;
        }
    }

    private int validateSrcAddr(int aSrc) {

        if (aSrc < 0 || aSrc >= topSrcAddr)
            throw new IllegalArgumentException("Variable src slot index is out of bounds");

        return aSrc;
    }

    private int validateDstAddr(int aDst) {

        if (aDst < 0 || aDst >= topDstAddr)
            throw new IllegalArgumentException("Variable dst slot index is out of bounds");

        return aDst;
    }

    @Override
    public int adjustAddressSrc2Dst(int aSrc) {

        // New variable? Return Dst top for Src top
        if (aSrc == topSrcAddr)
            return topDstAddr;

        if (aSrc > topSrcAddr)
            throw new IllegalArgumentException("Unable to adjust variable index: slot not allocated yet");

        return validateDstAddr(src2dstAddr[aSrc]);
    }

    /**
     * Store a variable
     * @param aSrc source address of a variable being STOREd
     * @param typeId type of the var being STOREd
     * @param name name of the var beind STOREd
     * @return transformed address (aDst)
     */
    @Override
    public int put(int aSrc, int typeId, String name) {

        if (aSrc < topSrcAddr) {
            int aDst = src2dstAddr[aSrc];
            if (!isHead(aDst)) { // tail of 64-bit var?
                aDst = src2dstAddr[aSrc - 1] + 1;
            }

            validateDstAddr(aDst);
            putVar(aSrc, aDst, typeId, name);
            return aDst;
        }

        while (aSrc > topSrcAddr) {
            add(TypeId.VOID, null);
        }

        assert(aSrc == topSrcAddr);

        // New variable? Return Dst top for Src top
        int aDst = topDstAddr;
        add(typeId, name);
        return aDst;
    }


    /**
     * Replace variable after some checks
     * @param aSrc
     * @param typeId
     * @param name
     * @return
     */
    // TODO: Maybe remove this method
    public boolean replaceVarBySrcAddr(int aSrc, int typeId, String name) {

        int oldTypeId = types[aSrc];
        assert (src2dstAddr[aSrc] >= EMPTY);

        int aDst = src2dstAddr[aSrc];
        assert(aDst >= 0);
        if (aDst < 0)
            return false;

        putVar(aSrc, aDst, typeId, name);
        return true;
    }


    private String getName(int typeId, String name, int aSrc) {
        return formatter.format(typeId, name, aSrc);
    }


    @Override
    public String toString() {
        StringBuffer o = new StringBuffer();
        o.append("{");
        int iSrc = 0, iDst = 0;
        int numVars = this.numVars();
        for (int i = 0; i < numVars; i++) {
            for (; iSrc < topSrcAddr && src2dstAddr[iSrc] < EMPTY; ++iSrc);
            o.append(i != 0 ? " " : "");
            if (iSrc >= topSrcAddr) {
                o.append("ERR: aSrc");
                break;
            }

            iDst = src2dstAddr[iSrc];
            if (iDst >= topDstAddr) {
                o.append("ERR: aDst");
                break;
            }

            if (iSrc < 0 || iDst < 0 || iSrc != dst2srcAddr[iDst] || iDst != src2dstAddr[iSrc]) {
                o.append("ERR: aSrc:").append(iSrc).append("<-X-> aDst:").append(iDst);
                break;
            }

            o       .append(dbgNameBySrcAddr(iSrc))
                    .append(':')
                    .append(iSrc < topSrcAddr ? String.valueOf(iSrc) : "?")
                    .append("->")
                    .append(iDst < topDstAddr ? String.valueOf(iDst) : "?");
            ++iSrc;
        }

        return o.append("}").toString();
    }


    private TypeArray copyTo(TypeArray dst, int aSrc0, int aSrc1) {

        if (aSrc0 < 0 || aSrc1 > topSrcAddr || aSrc0 > aSrc1)
            throw new IllegalArgumentException("Variable src slot index is out of bounds");

        dst.clear();
        for (int i = aSrc0; i < aSrc1;) {
            int typeId = types[i];
            assert (src2dstAddr[i] >= EMPTY);
            dst.add(typeId, names[i]);
            i += TypeId.size32Src(typeId);
        }

        return dst;
    }

    @Override
    public TypeArray copyOfRange(int aSrc0, int aSrc1) {

        return copyTo(new TypeArray(numVars(), formatter), aSrc0, aSrc1);
    }

    @Override
    public TypeArray copyAll() {
        return copyOfRange(0, topSrcAddr);
    }


    public void restoreFrom(TypeArray src) {

        int n = src.length();
        clear();
        checkAlloc(n, n);
        add(src.getTypes(), src.getNames(), 0, n);
    }


    public void saveTo(TypeArray dst) {

        copyTo(dst, 0, topSrcAddr);
    }

    @Override
    public int[] getTypes() {
        return types;
    }

    @Override
    public String[] getNames() {
        return names;
    }

    @Override
    public int[] getSrc2dstAddr() {
        return src2dstAddr;
    }

    @Override
    public int[] getDst2SrcAddr() {
        return dst2srcAddr;
    }

    @Override
    public void setFormatter(VariableNameFormatter formatter) {
        this.formatter = formatter;
    }
}