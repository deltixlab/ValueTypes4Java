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
import deltix.vtype.type.DescriptorParser;
import deltix.vtype.common.CrudeLogger;
import deltix.vtype.type.FrameMap;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;

import static org.objectweb.asm.Opcodes.*;


public class MethodNode extends org.objectweb.asm.tree.MethodNode {

    private static final int SKIPPED    = 0;
    private static final int NORMAL     = 1;
//    private static final int SRC_CLASS  = 2;
//    private static final int DST_CLASS  = 3;

    private static final String suppressWarningsAnnotation = "ValueTypeSuppressWarnings";

    private final CrudeLogger log;
    private final Mapping mapping;

    final String className;
    //private final int classAccess;

    private final ClassVisitor outerCv;

    private final boolean vtFieldInitializationRequired;
    private final String shortClassName;
    private final TranslationState state;

    //private final boolean translateReturnValue;
    int logLevel = CrudeLogger.ERR;

    int processingMode;
    public final String originalName;
    public final String originalDesc;
    private boolean shouldLogMethodName;

    final Warnings warnings;
    private int currentLine;


    MethodNode(final TranslationState state, int access, String name, String desc, String signature, String[] exceptions,
               ClassVisitor outerCv) {

        super(ASM6, access, name, desc, signature, exceptions);

        this.state          = state;
        this.outerCv        = outerCv;
        this.log            = state.logger;
        this.mapping        = state.mapping;
        this.className      = state.classPath;
        this.shortClassName = AsmUtil.extractClassName(className);
        this.warnings = state.warnings;

        boolean isClInit = name.equals("<clinit>");
        if ((isClInit || name.equals("<init>")) && null != (state.firstVtField[isClInit ? 1 : 0])) {
            this.vtFieldInitializationRequired = true;
        } else {
            this.vtFieldInitializationRequired = false;
        }

        this.originalName = name;
        this.originalDesc = desc;

        if (DescriptorParser.findVtInMethodDesc(desc, mapping)) {
            this.name = name = state.methodNameConverter.transform(name, desc);
            this.desc = desc = DescriptorParser.getTransformedDesc(desc, false, mapping);
        }

        ClassDef classDef = mapping.getClassDef(className);
        if (null != classDef) {
            if (classDef.isInitialized()) {
                processingMode = SKIPPED;
            } else
                throw new IllegalStateException("Class " + originalName + "is in unexpected state(uninitialized?): " + classDef.getFlags());
        } else {
            processingMode = NORMAL;
        }

        if (0 != mapping.getNumLoggedMethods() &&
                (mapping.isMetodLogged(className + '.' + originalName) || mapping.isMetodLogged(originalName))) {
            logLevel = CrudeLogger.TRACE;
        }

        log.setLogLevel(logLevel);
        if (NORMAL == processingMode) {
            state.instructionIterator.setLogLevel(logLevel);
            //VariableNameFormatter formatter = VariableNameDefaultFormatter.get();
        }

        currentLine = -1;
        // Actually we still do 2 scans, 1st via Visitor interface just to build MethodNode contents and analyse Stack Frames
        // 2nd is to transform instruction list
        state.onNewMethod(this);
    }


    protected void dbgBreak() {

        log.debuggerBreak();
    }


    protected void dbgBreakAt(int lineNum) {
        if (currentLine == lineNum) {
            dbgBreak();
        }
    }


    private void dbgBreakAt(String methodName, int line) {
        if (name.equals(methodName) && currentLine == line) {
            dbgBreak();
        }
    }


    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (desc.endsWith("/ValueTypeTrace;")) {
            logLevel = CrudeLogger.TRACE;
        } else if (desc.endsWith("/ValueTypeDebug;") && logLevel > CrudeLogger.DBG) {
            logLevel = CrudeLogger.DBG;
        } else if (desc.endsWith("/ValueTypeIgnore;")) {
            processingMode = SKIPPED;
        } else if (desc.endsWith("/" + suppressWarningsAnnotation + ";")) {
            return new AnnotationVisitor(api, super.visitAnnotation(desc,visible)) {
                @Override
                public AnnotationVisitor visitArray(String name) {
                    return new AnnotationVisitor(api, super.visitArray(name)) {
                        @Override
                        public void visit(String name, Object value) {
                            if (null == name && value instanceof String) {
                                warnings.suppress((String)value);
                            }
                            super.visit(name, value);
                        }
                    };
                }
            };
        }

        return super.visitAnnotation(desc, visible);
    }

    @Override
    public void visitEnd() {
        switch (processingMode) {
            case SKIPPED:
                break;

            case NORMAL:
                transformMethod();
                break;
        }

        if (null != outerCv) {
            super.accept(outerCv);
        }
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {

        FrameMap frameMap = state.frameMap;
        if (null != frameMap) {
            frameMap.processFrame(type, nLocal, local, nStack, stack);
            frameMap.addFrameNode(instructions.getLast());
        }

        super.visitFrame(type, nLocal, local, nStack, stack);
    }


    @Override
    public void visitLabel(Label label) {

        VarListTransformer varListTransformer = state.debugVarsListTransformer;
        if (null != varListTransformer) {
            varListTransformer.registerLabel(getLabelNode(label));
        }

        super.visitLabel(label);
    }

    @Override
    public void visitLineNumber(int line, Label start) {

        currentLine = line;
        super.visitLineNumber(line, start);
    }

    void transformMethod() {


        try {
            InstructionIterator iter = state.instructionIterator;

            // Pass 1: Scan and skip methods that don't contain relevant code
            // TODO: It is outdated as we are are expecting a list of relevant methods from the new QuickScanClassVisitor class
            boolean hasVType = false;
            if (!desc.equals(originalDesc)) {
                hasVType = true;
            } else {
                hasVType = false;
                iter.processMethod(this, state.vTypeScanOpcodeProcessor);
            }

            //System.out.printf("checking method: %s.%s / %s%n", classPath, name, desc);

            if (!(hasVType || state.vTypeScanOpcodeProcessor.hasVType())) {
                log.dbg("No Value Types found in method: %s.%s", className, name);

                if (mapping.verifyAllMethods) {
                    System.out.printf("VT Agent: VERIFYING method: %s.%s%n", className, name);
                    // TODO: This is deprecated and untested, remove or test
                    iter.processMethod(this, state.basicOpcodeProcessor);
                    log.print("VT Agent: VERIFIED method: %s.%s / %s%n", className, name, desc);
                }

                if (!vtFieldInitializationRequired)
                    return;
            }

            //System.out.printf("converting method: %s.%s / %s%n", classPath, name, desc);
            // Pass 2: Convert method code
            if (mapping.logAllMethods) {
                log.print("VT Agent: PROCESSING method: %s.%s%n", className, name);
            }

            state.setLogLevel(logLevel);

            if (log.on(log.TRACE)) {
                log.print("--------------------------------------------------------------------------"
                                + "\n-  Trace logger for: %s.%s\n"
                                + "--------------------------------------------------------------------------\n",
                         className, name);
                //logger.print("/////////////////////////////////////////////////////////\n// Trace logger for %s  - %s");
                iter.setFirstLineLogPrefix(shortClassName +  ":");
            }

            state.debugVarsListTransformer.startTransformation();
            iter.processMethod(this, state.singlePassCodeTransformer);
            this.localVariables = state.debugVarsListTransformer.getResult();

            if (mapping.logSuccesses || shouldLogMethodName) {
                log.print("VT Agent: UPDATED method: %s.%s / %s%n",
                        className.replace('/', '.'), originalName, desc);

            }

            if (0 != warnings.numTotal()) {
                System.err.printf("VT Agent warnings for method: %s.%s:%n",
                        className.replace('/', '.'), originalName);

                int[] order = new int[warnings.WARNINGS_COUNT];

                int n = 0;
                for (int i = 0; i < warnings.WARNINGS_COUNT; i++) {
                    if (warnings.numOf(i) != 0) {
                        order[n++] = i;
                    }
                }

                for (int i = 0; i < n - 1; ++i) {
                    for (int j = n - 2; j >= i; --j) {
                        if (warnings.lines[warnings.first[order[j]]] > warnings.lines[warnings.first[order[j + 1]]]) {
                            int tmp = order[j];
                            order[j] = order[j + 1];
                            order[j + 1] = tmp;
                        }
                    }
                }

                for (int ii = 0; ii < n; ++ii) {
                    int i = order[ii];
                    System.err.printf("    * %s in line(s): ", Warnings.descs[i]);
                    String separator = "";
                    Object prevData = null;
                    for (int j = 0, k = warnings.first[i]; k != 0; ++j, k = warnings.next[k]) {
                        Object data = warnings.data[k];
                        System.err.printf(null != data && !data.equals(prevData) ? "%s%d(%s)" : "%s%d", separator, warnings.lines[k],
                                data);
                        prevData = data;
                        separator = ", ";
                    }

                    System.err.printf("%n    %s or suppress with @%s({\"%s\"})%n",
                            Warnings.hints[i], suppressWarningsAnnotation, Warnings.names[i]);
                }
            }

            state.classWasTransformed = true;
        }
        catch (Throwable e) {
            //System.out.printf("Exception occured while processing method: %s.%s%s%n", classPath, name, desc);
            int currentLine = state.instructionIterator.currentLine();
//            if (currentLine > 0) {
//                logger.err("while translating method %s source code line: %d", name, currentLine);
            //}

            throw new MethodException(this, "", currentLine, e);
        }
    }

    public void setShouldLogMethodName(boolean shouldLogMethodName) {
        this.shouldLogMethodName = shouldLogMethodName;
    }
}
