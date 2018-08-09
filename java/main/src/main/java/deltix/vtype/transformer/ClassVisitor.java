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

import deltix.vtype.mapping.ClassDef;
import deltix.vtype.type.DescriptorParser;
import deltix.vtype.type.TypeId;
import deltix.vtype.mapping.Mapping;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.ACC_STATIC;

public class ClassVisitor extends org.objectweb.asm.ClassVisitor {

    TranslationState state;
    private final Mapping mapping;
    private final String className;
    private final ClassDef classDef;

    private boolean firstMethod = true;
    private final boolean isSrcClass;
    private final boolean isDstClass;
    boolean isClassTransformed   /*mapping.verifyAllMethods */;
    boolean printEndOfProcessingMessage = false;


    public ClassVisitor(final int api, final org.objectweb.asm.ClassVisitor cv, TranslationState state) {
        super(api, cv);

        this.state      = state;
        this.mapping    = state.mapping;
        this.className  = state.classPath;
        this.classDef   = state.classDef;

        isSrcClass      = mapping.isMappedSrcClass(this.className);
        isDstClass      = mapping.isMappedDstClass(this.className);

        isClassTransformed = !(isSrcClass || isDstClass);
    }


    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {

        if (desc.endsWith("/ValueTypeIgnore;")) {
            isClassTransformed = false; // Opt-out
        } else if (desc.endsWith("/ValueTypeTest;") && !isSrcClass) {
            isClassTransformed = true;  // Opt-in
        }

        return super.visitAnnotation(desc, visible);
    }


    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

        // We will allow processing of clinit and also fields from source VType class
        if (!isClassTransformed) while (true) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

            if (isSrcClass) {
//                if (name.equals("<clinit>"))
//                    break; // Still need to transform the static constructor
                return new VTypeSrcMethodVisitor(access, name, desc, classDef, mv);
            } else if (isDstClass) {
                return new VTypeDstMethodVisitor(access, name, desc, classDef, mv);
            }

            return mv;
        }


        checkIfJustStarted();
        return new MethodNode(state, access, name, desc, signature, exceptions, cv);
    }

    private void checkIfJustStarted() {
        if (firstMethod && mapping.logAllMethods) {
            System.out.println("VT Agent: BEGIN processing first method of: " + className);
            printEndOfProcessingMessage = true;
            firstMethod = false;
        }
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {

        //System.out.println("visitField: access="+access+" name="+name+" desc="+desc+" signature="+signature+" value="+value);
        if (isClassTransformed) {
            String oldDesc = desc;
            int typeId = DescriptorParser.getDescTypeId(desc, mapping);
            if (TypeId.isVt(typeId)) {
                checkIfJustStarted();
                // Field names are not modified anymore
                //name = nameConverter.transform(name, desc);
                desc = mapping.typeIdToDstTypeDesc(typeId);

                if (TypeId.isVtValue(typeId)) {
                    state.registerScalarVtField(typeId, name, 0 != (access & ACC_STATIC) ? 1 : 0);
                }

//                        if (isSrcClass) {
//                            String oldClassName = classPath;
//                            classPath = mapping.getClassDef(classPath).getDstClassPath();
//                            if ((ACC_STATIC | ACC_FINAL) == (access & (ACC_STATIC | ACC_FINAL))) {
//                                System.out.printf("UPDATED src const field: %s.%s / %s -> %s.%s %s%n",
//                                        oldClassName, name, oldDesc, classPath, name, desc);
//                            }
//                        }

                if (mapping.logSuccesses) {
                    System.out.printf("VT Agent: UPDATED field: %s.%s / %s -> %s%n", className, name, oldDesc, desc);
                }

                state.classWasTransformed = true;
            }
        }

        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        if (printEndOfProcessingMessage || mapping.logEveryClass) {
            System.out.println("VT Agent: END processing class: " + className);
        }

        if (mapping.logSuccesses || mapping.logEveryClass) {
            state.logScalarVtFieldCounts();
        }

        if (null != classDef) {
            if (isSrcClass) {
                classDef.setAllSrcMethodsScanned();
            } else {
                classDef.setAllDstMethodsScanned();
            }
        }
    }
}
