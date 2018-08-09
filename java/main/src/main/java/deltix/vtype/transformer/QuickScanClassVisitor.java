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

import deltix.vtype.type.DescriptorParser;
import deltix.vtype.type.TypeId;
import deltix.vtype.mapping.ClassDef;
import deltix.vtype.mapping.Mapping;
import org.objectweb.asm.*;

import java.util.HashSet;

import static deltix.vtype.transformer.AsmUtil.parseMethod;
import static deltix.vtype.type.TypeId.isVt;
import static org.objectweb.asm.Opcodes.ASM6;

public class QuickScanClassVisitor extends org.objectweb.asm.ClassVisitor {
    private final Mapping mapping;
    private boolean found;
    private HashSet<String> transformedMethods;
    private MethodVisitor cachedMethodVisitor;


    public QuickScanClassVisitor(int api, ClassVisitor cv, Mapping mapping) {
        super(api, cv);
        this.mapping = mapping;
        this.found = false;
    }

    boolean foundVType() {
        return this.found;
    }


    private void addMethod(final String name, final String desc) {
        if (null == transformedMethods) {
            transformedMethods = new HashSet<>(8);
        }

        transformedMethods.add(name + desc);
    }


    public static QuickScanClassVisitor findVt(ClassReader cr, Mapping mapping) {
        QuickScanClassVisitor vTypeScan = new QuickScanClassVisitor(ASM6, null, mapping);
        cr.accept(vTypeScan, 0);
        return vTypeScan;
    }

    private int getDescTypeId(final String desc) {
        return DescriptorParser.getDescTypeId(desc, mapping);
    }

    private int getClassTypeId(final String desc) {
        return DescriptorParser.getClassTypeId(desc, mapping);
    }

    int checkTypeId(int typeId) {

        if (!isVt(typeId))
            throw new IllegalStateException("Not a Value Type Id");

        ClassDef classDef = mapping.getClassDefById(typeId);
        if (null == classDef || !classDef.isInitialized())
            throw new IllegalStateException("Class Definition object is not initialized (unable to load transformed classes?)");

        return typeId;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        int typeId = getDescTypeId(desc);
        if (isVt(typeId)) {
            found = true;
        }

        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, final String name, final String desc, String signature, String[] exceptions) {

        // Already found in the class? TODO: still scan method if we start using this class as the main detector
        if (found)
            return null;

        if (DescriptorParser.findVtInMethodDesc(desc, mapping)) {
            found = true;
            // No further processing if the type is found in the signature
            return null;
        }

        // Will not be created for classes without methods
        if (null == cachedMethodVisitor) {
            cachedMethodVisitor = new MethodVisitor(this);
        }

        return cachedMethodVisitor.init(name, desc);
    }

    public HashSet<String> getTransformedMethods() {
        return transformedMethods;
    }


    static class MethodVisitor extends org.objectweb.asm.MethodVisitor {
        final QuickScanClassVisitor parent;
        final Mapping mapping;
        final protected int[] methodArgs = new int[0x100];

        boolean isVTypeMethod = false;
        private String name;
        private String desc;

        MethodVisitor init(String name, String desc) {
            this.name = name;
            this.desc = desc;
            this.isVTypeMethod = false;
            return this;
        }


        private int getTypeId(final String desc) {
            return desc.charAt(0) == '/' ? mapping.getClassTypeId(desc) : DescriptorParser.getDescTypeId(desc, mapping);
        }


        MethodVisitor(QuickScanClassVisitor parent) {
            super(parent.api);
            this.parent = parent;
            this.mapping = parent.mapping;
        }


        private boolean found() {
            if (!isVTypeMethod) {
                parent.addMethod(name, desc);
                isVTypeMethod = true;
                parent.found = true;
            }

            return true;
        }


//        private void onFound(int typeId) {
//            parent.checkTypeId(typeId);
//            if (!isVTypeMethod) {
//                parent.addMethod(name, desc);
//                isVTypeMethod = true;
//                parent.found = true;
//            }
//        }

        /**
         * Check if the type signature refers to Value Type
         * @param typeId - already parsed class type id
         * @return
         */
        private boolean tryFindVt(int typeId) {
            return TypeId.isVt(typeId) && found();
        }

        /**
         * Check if the type signature refers to Value Type
         * @param desc
         * @return
         */
        private boolean tryFindVt(String desc) {
            return DescriptorParser.isVt(desc, mapping) && found();
        }

        private boolean tryFindVtInMethodDesc(String desc) {
            return DescriptorParser.findVtInMethodDesc(desc, mapping) && found();
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (desc.endsWith("/ValueTypeTrace;") || desc.endsWith("/ValueTypeDebug;") || desc.endsWith("/ValueTypeIgnore;")) {
                parent.found = true;
            }

            return null;
        }

        @Override
        public void visitTypeInsn(int opcode, String desc) {
            tryFindVt(desc);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            boolean dummy = tryFindVt(desc) || tryFindVt(owner);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            boolean dummy = tryFindVt(owner) || tryFindVtInMethodDesc(desc);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {

            if (tryFindVtInMethodDesc(desc) || tryFindVtInMethodDesc(bsm.getDesc()))
                return;

            final int n = bsmArgs.length;
            for (int i = 0; i < n; i++) {
                String bsmDesc;
                Object arg = bsmArgs[i];
                if (arg instanceof Type) {
                    bsmDesc = arg.toString();
                } else if (arg instanceof Handle) {
                    bsmDesc = ((Handle) arg).getDesc();
                } else
                    continue;

                if (null == bsmDesc || bsmDesc.charAt(0) != '(')
                    continue;

                if (tryFindVtInMethodDesc(bsmDesc))
                    break;
            }
        }


        // Visit LDC??
//            @Override
//            public void visitLdcInsn(Object cst) {
//                super.visitLdcInsn(cst);
//            }


        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
            tryFindVt(desc);
        }

//            @Override
//            public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
//
//            }

        @Override
        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
            boolean dummy = tryFindVt(desc) || signature != null && tryFindVt(signature);
        }
    }
}
