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

import deltix.vtype.interpreter.*;
import deltix.vtype.mapping.ClassDef;
import deltix.vtype.mapping.Mapping;
import deltix.vtype.type.FrameMap;
import deltix.vtype.type.JvmStack;
import deltix.vtype.type.TypeId;
import deltix.vtype.common.CrudeLogger;
import deltix.vtype.type.VariableMap;

import java.util.Arrays;

import static deltix.vtype.transformer.AsmUtil.initializeMethodArguments;

/**
 * Contains translation state that persists across method visitors working on the same class file,
 * will be possibly reused to avoid memory reallocations and reduce GC pressure
 * It also simplifies interfaces reducing the number of parameters that need to be passed across multiple calls.
 * In exchange, we now need to properly reset the transient state before processing every method or making a pass over a method's code
 */
class TranslationState {
    final CrudeLogger logger;
    final Mapping mapping;
    final ClassDef classDef;

    final MethodNameTransformer methodNameConverter = new MethodNameTransformer();

    final String classPath;
    //final int classAccess;
    boolean classWasTransformed = false;

    /**
     * Linked list of ValueType fields found in the class
     * Built once per class
     */

    // Collect all scalar VType fields in the order of appereance
    int nScalarVtFields, nScalarVtFieldsAllocated;
    String[] scalarVtFieldNames;
    int[] prevVtField;

    // [isStatic][numTransformedClasses]
    int[][] firstVtField;
    int[][] nVtFields;

    /**
     * There structures are reset and reused for each method
     */
    final JvmStack stack;
    final InstructionIterator instructionIterator;
    final VTypeScanHandler vTypeScanOpcodeProcessor;
    final StackWalkHandler basicOpcodeProcessor;
    final SinglePassCodeTransformer singlePassCodeTransformer;
    final FrameMap frameMap;
    final VariableMap variableMap;
    final VarListTransformer debugVarsListTransformer;

    final Warnings warnings;

    /**
     * Prepare main entities that will be reused for the each processed method
     */
    TranslationState(String classPath, ClassDef classDef, Mapping mapping) {

        this.classPath = classPath;
        this.mapping = mapping;
        this.classDef = classDef;
        this.logger = new CrudeLogger();

        VariableNameDefaultFormatter varFormatter = VariableNameDefaultFormatter.get();
        stack = new JvmStack(logger, varFormatter);
        instructionIterator = new InstructionIterator(logger);
        warnings = new Warnings();

// = new VariableMapV2(varFormatter);
        this.variableMap = new VariableMapV2(varFormatter);
        frameMap = new FrameMap(variableMap, stack, mapping, varFormatter, logger);
        debugVarsListTransformer = new VarListTransformer(mapping.noDebugData, mapping, logger);

        vTypeScanOpcodeProcessor = new VTypeScanHandler(mapping);
        basicOpcodeProcessor = new StackWalkHandler(stack, instructionIterator, logger);
        singlePassCodeTransformer = new SinglePassCodeTransformer(this);
        variableMap.setFormatter(singlePassCodeTransformer);

        // Linked list of ValueType fields found in the class
        firstVtField = new int[2][mapping.numClasses()];
        nVtFields = new int[2][mapping.numClasses()];
        Arrays.fill(firstVtField[0], -1);
        Arrays.fill(firstVtField[1], -1);
    }


    void onNewMethod(MethodNode methodNode) {

        // Prepare for building Frame map
        warnings.clear();
        warnings.setIgnoreMask(mapping.ignoredWarnings);
        stack.resetStack();
        frameMap.clear();
        initializeMethodArguments(variableMap, methodNode, mapping);
        frameMap.saveVarFrame();

        debugVarsListTransformer.init(methodNode.localVariables);

        // Opcode transformation will be reinitialized in another place
        //singlePassCodeTransformer.init(methodNode);
    }


    void setLogLevel(int logLevel) {

        // Logging level may change from method to method, so we allow updating it
        this.logger.setLogLevel(logLevel);
        stack.setLogLevel(logLevel);
        singlePassCodeTransformer.setLogLevel(logLevel);
        debugVarsListTransformer.setLogLevel(logLevel);
    }


    void registerScalarVtField(int typeId, String name, int isStatic) {

        // Grow
        int i = nScalarVtFields++;
        if (nScalarVtFields >= nScalarVtFieldsAllocated) {
            if (0 == nScalarVtFieldsAllocated) {
                nScalarVtFieldsAllocated = 8;
                scalarVtFieldNames = new String[nScalarVtFieldsAllocated];
                prevVtField = new int[nScalarVtFieldsAllocated];
            } else {
                nScalarVtFieldsAllocated = nScalarVtFieldsAllocated * 2;
                scalarVtFieldNames = Arrays.copyOf(scalarVtFieldNames, nScalarVtFieldsAllocated);
                prevVtField = Arrays.copyOf(prevVtField, nScalarVtFieldsAllocated);
            }
        }

        int index = TypeId.getVtClassIndex(typeId);
        scalarVtFieldNames[i] = name;
        prevVtField[i] = firstVtField[isStatic][index];
        firstVtField[isStatic][index] = i;
        ++nVtFields[isStatic][index];
    }


    void logScalarVtFieldCounts() {

        if (0 == nScalarVtFields)
            return;

        for (int static_ = 1; static_ >= 0; --static_) {
            for (int i = 0, n = mapping.numClasses(); i < n; ++i) {
                int m = nVtFields[static_][i];
                if (0 != m) {
                    System.out.printf("VT Agent: %sfield: %s x %d%n", 0 == static_ ? "" : "static ", mapping.getClassDef(i).getSrcClassPath(), m);
                }
            }
        }
    }
}
