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

import org.objectweb.asm.tree.LabelNode;

import static deltix.vtype.common.Utils.reverse;

public class FrameEntry {

    private final VariableNameFormatter formatter; // For debug string generation and logging etc.
    public final LabelNode label;

    protected final String[] stackNames;
    protected final int[] stackTypes;

    protected final String[] varNames;
    protected final int[] varTypes;

    protected final int numVars;
    protected final int numStack;

    public FrameEntry(LabelNode label,
                      int numStackEntries, int[] stackTypes, String[] stackNames,
                      int numVarEntries, int[] varTypes, String[] varNames,
                      VariableNameFormatter formatter) {

        this.label = label;
        this.formatter = formatter;

        this.numStack = numStackEntries;
        this.stackTypes = new int[numStackEntries];
        this.stackNames = new String[numStackEntries];
        if (0 != numStackEntries) {
            System.arraycopy(stackTypes, 0, this.stackTypes, 0, numStackEntries);
            System.arraycopy(stackNames, 0, this.stackNames, 0, numStackEntries);
            // Will simplify iteration in the future
            reverse(this.stackTypes, numStackEntries);
            reverse(this.stackNames, numStackEntries);
        }

        this.numVars = numVarEntries;
        this.varTypes = new int[numVarEntries];
        this.varNames = new String[numVarEntries];
        if (0 != numVarEntries) {
            System.arraycopy(varTypes, 0, this.varTypes, 0, numVarEntries);
            System.arraycopy(varNames, 0, this.varNames, 0, numVarEntries);
        }
    }

    public int getNumVars() {
        return numVars;
    }

    public int[] getVarTypes() {
        return varTypes;
    }

    public String[] getVarNames() {
        return varNames;
    }

    public TypeArray copyOfVarRange(int startOffset, int endOffset) {
        return new TypeArray(varTypes, varNames, startOffset, endOffset - startOffset);
    }

    public TypeArray copyOfStackRange(int startOffset, int endOffset) {
        return new TypeArray(stackTypes, stackNames, startOffset, endOffset - startOffset);
    }

    public int getNumStackEntries() {
        return numStack;
    }

    public int[] getStackTypes() {
        return stackTypes;
    }

    public String[] getStackNames() {
        return stackNames;
    }

    private String getName(int typeId, String name) {

        return formatter.format(typeId, name, -1);
    }

    public String getVarDbgName(int index) {

        return getName(varTypes[index], varNames[index]);
    }

    public String getStackDbgName(int index) {

        return getName(stackTypes[index], stackNames[index]);
    }

    @Override
    public String toString() {

        StringBuffer o = new StringBuffer();
        o.append("V:{");
        for (int i = 0, n = numVars; i < n; i++) {
            o.append(String.format("%s%x:%s", i != 0 ? " " : "", varTypes[i], getVarDbgName(i)));
        }

        o.append("}, S:{");
        for (int i = 0, n = numStack; i < n; i++) {
            o.append(String.format("%s%x:%s", i != 0 ? " " : "", stackTypes[i], getStackDbgName(i)));
        }

        return o.append("}").toString();
    }
}
