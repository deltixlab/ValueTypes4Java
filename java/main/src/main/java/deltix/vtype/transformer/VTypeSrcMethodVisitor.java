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
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ASM6;

public class VTypeSrcMethodVisitor extends MethodVisitor {
    private final ClassDef classDef;
    private final String name;
    private final String desc;
    private final int access;
    private String impl;

    public VTypeSrcMethodVisitor(int access, String name, String desc, ClassDef classDef, MethodVisitor mv) {
        super(ASM6, mv);
        this.impl = null;
        this.access = access;
        this.name = name;
        this.desc = desc;
        this.classDef = classDef;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {

        if (desc.endsWith("/ValueType;")) {
            return new AnnotationVisitor(api, super.visitAnnotation(desc,visible)) {
                @Override
                public void visit(String name, Object value) {
                    if (name.equals("impl")) {
                        impl = String.valueOf(value);
                    }

                    super.visit(name, value);
                }
            };
        }

        return super.visitAnnotation(desc, visible);
    }

    @Override
    public void visitEnd() {
        try {
            if (-1 == name.indexOf('<')) {
                classDef.addPartialMethod(name, desc, 0 != (ACC_STATIC & access), impl);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.visitEnd();
    }

}
