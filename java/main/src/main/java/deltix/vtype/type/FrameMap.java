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

import deltix.vtype.transformer.AsmUtil;
import deltix.vtype.mapping.Mapping;
import deltix.vtype.common.CrudeLogger;
import org.objectweb.asm.tree.*;

import java.util.IdentityHashMap;

import static deltix.vtype.transformer.AsmUtil.stackFrameToString;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.F_FULL;
import static org.objectweb.asm.Opcodes.F_NEW;

public class FrameMap {
    private final Mapping mapping;
    protected final CrudeLogger log;
    private final JvmStack vm;
    private final VariableMap vars;
    private final VariableNameFormatter formatter;
    private TypeArray savedLocalVarFrame;
    private JvmStack.SavedStack savedStack;
    private final IdentityHashMap<LabelNode, FrameEntry> frames;
    private TypeArray tmpTypeArray;

    public FrameMap(final VariableMap vars, final JvmStack stack, final Mapping mapping,
                    final VariableNameFormatter formatter, final CrudeLogger log) {

        this.vm = stack;
        this.mapping = mapping;
        this.formatter = formatter;
        this.log = log;
        this.vars = vars;
        this.tmpTypeArray = new TypeArray(0x20, formatter);

        savedLocalVarFrame = new TypeArray(0x20, formatter);
        this.frames = new IdentityHashMap<>(0x20);

        savedStack = vm.saveStack(); // Only done once
    }


    public void clear() {

        frames.clear();
        vars.clear();
        saveVarFrame();
        resetAndSaveStackFrame();
    }


    void restoreVarFrame() {
        vars.restoreFrom(savedLocalVarFrame);
    }


    public void saveVarFrame() {
        vars.saveTo(savedLocalVarFrame);
    }


    private void addVarFromFrame(Object o) {
        AsmUtil.addVarFromFrame(vars, mapping, o);
    }


    protected void resetAndSaveStackFrame() {

        resetStackFrame();
        saveStackFrame();
    }


    protected void resetStackFrame() {
        vm.resetStack();
    }


    protected void saveStackFrame() {
        vm.saveStack(savedStack);
    }


    public void processFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {

        // TODO: Logging
        switch (type) {
            case F_SAME:
                restoreVarFrame();
                log.trace("Reset vars to previous frame.");
                resetAndSaveStackFrame();
                break;

            case F_APPEND:
                restoreVarFrame();
                for (int i = 0; i < nLocal; i++) {
                    addVarFromFrame(local[i]);
                }

                saveVarFrame();
                log.trace("Add %d vars.", nLocal);

                resetAndSaveStackFrame();
                break;

            case F_CHOP:
                restoreVarFrame();
                vars.removeLast(nLocal);
                saveVarFrame();
                log.trace("Chopped %d vars.", nLocal);
                resetAndSaveStackFrame();
                break;

            case F_SAME1:
                restoreVarFrame();
                //System.out.print(node.stack.get(0).toString());
                log.trace("Reset vars to previous frame.");
                resetStackFrame();
                vm.pushAsmFrameObject(stack[0], mapping);
                saveStackFrame();
                break;

            case F_NEW:
            case F_FULL:
                //System.out.print(node.stack.get(0).toString());
                vars.clear();
                for (int i = 0; i < nLocal; i++) {
                    addVarFromFrame(local[i]);
                }

                saveVarFrame();
                log.trace("Reloaded full vars/stack frame.");
                resetStackFrame();
                for (int i = 0; i < nStack; i++) {
                    vm.pushAsmFrameObject(stack[i], mapping);
                }

                saveStackFrame();
                break;
        }
    }


    public void addFrameNode(AbstractInsnNode prevNode) {

        // We take the snapshot of the current stack frame and attach the new entry to a label
        while (null != prevNode && AbstractInsnNode.LINE == prevNode.getType()) {
            prevNode = prevNode.getPrevious();
        }

        if (AbstractInsnNode.LABEL != prevNode.getType()) {
            log.err("Expected label node, found: %s", prevNode);
            return;
        }

        LabelNode label = (LabelNode)prevNode;

        TypeArray tmp = tmpTypeArray;
        vars.saveTo(tmp);
        FrameEntry entry = new FrameEntry(label,
                savedStack.top, savedStack.types, savedStack.names,
                tmp.length(), tmp.types, tmp.names, formatter);

        frames.put(label, entry);
        log.trace("Add StackFrame for label %s - %s", label.getLabel(), entry);
    }


    private void processNode(AbstractInsnNode node0) {
        if (AbstractInsnNode.FRAME == node0.getType()) {
            FrameNode node = (FrameNode) node0;

            log.dbg("Stack Frame: " + stackFrameToString(node));
            // Performance warning!
            processFrame(node.type, node.local.size(), node.local.toArray(), node.stack.size(), node.stack.toArray());
            addFrameNode(node0.getPrevious());
        }
    }


    public FrameEntry getFrames(LabelNode label) {
        return frames.get(label);
    }
}
