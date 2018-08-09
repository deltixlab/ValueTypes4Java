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

package deltix.vtype.interpreter;

import deltix.vtype.common.CrudeLogger;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.JSR;

public class InstructionIterator {

    private final CrudeLogger log;
    private boolean logDbg;
    private boolean logTrace;

    private OpcodeProcessor handler;
    private LineNumberNode currentLineNode = null;

    /**
     * This prefix is printed before the first logged line/label
     */
    public void setFirstLineLogPrefix(Object firstLineLogPrefix) {
        this.firstLineLogPrefix = firstLineLogPrefix;
    }

    private Object firstLineLogPrefix = "";

    public InstructionIterator(CrudeLogger logger) {
        this.log = logger;
    }


    void processNode(AbstractInsnNode node0) {

        switch (node0.getType()) {
            case AbstractInsnNode.INSN: {
                // http://asm.ow2.org/asm50/javadoc/user/org/objectweb/asm/tree/InsnNode.html
                InsnNode node = (InsnNode) node0;
                int op = node.getOpcode();
                if (logTrace) {
                    log.trace("\t%3s BasicInsn : line %d", node0.getOpcode(), currentLine());
                }

                switch (op) {
                    case NOP:
                        break;

                    case ATHROW:
                        handler.onAThrow(node);
                        break;

                    case IRETURN: case FRETURN:
                        handler.onReturn32(node);
                        break;

                    case LRETURN: case DRETURN:
                        handler.onReturn64(node);
                        break;

                    case RETURN:
                        handler.onReturn(node);
                        break;

                    case ARETURN:
                        handler.onAReturn(node);
                        break;

                    case POP:
                        handler.onPop32(node);
                        break;

                    case POP2:
                        handler.onPop64(node);
                        break;

                    case DUP:
                        handler.onDup32(node);
                        break;

                    case DUP_X1:
                        handler.onDup32x(node, 1);
                        break;

                    case DUP_X2:
                        handler.onDup32x(node, 2);
                        break;

                    case DUP2:
                        handler.onDup64(node);
                        break;

                    case DUP2_X1:
                        handler.onDup64x(node, 1);
                        break;

                    case DUP2_X2:
                        handler.onDup64x(node, 2);
                        break;

                    case SWAP:
                        handler.onSwap32(node);

                    case ACONST_NULL:
                        handler.onAConstNull(node);
                        break;

                    case LCONST_0: case LCONST_1:
                        handler.onConstI64(node);
                        break;

                    case DCONST_0: case DCONST_1:
                        handler.onConstF64(node);
                        break;

                    // Load numeric values onto the stack
                    case ICONST_M1:
                    case ICONST_0: case ICONST_1: case ICONST_2: case ICONST_3: case ICONST_4: case ICONST_5:
                        handler.onConstI32(node);
                        break;

                    case FCONST_0: case FCONST_1: case FCONST_2:
                        //case BIPUSH: case SIPUSH: // -> IntInstr

                        handler.onConstF32(node);
                        break;

                    case IALOAD: case BALOAD: case CALOAD: case SALOAD:
                        handler.onAI32Load(node);
                        break;

                    case FALOAD:
                        handler.onAF32Load(node);
                        break;

                    case LALOAD:
                        handler.onAI64Load(node);
                        break;

                    case DALOAD:
                        handler.onAF64Load(node);
                        break;

                    case AALOAD:
                        handler.onAALoad(node);
                        break;

                    case IASTORE: case FASTORE: case BASTORE: case CASTORE: case SASTORE:
                        handler.onA32Store(node);
                        break;

                    case LASTORE: case DASTORE:
                        handler.onA64Store(node);
                        break;

                    case AASTORE:
                        handler.onAAStore(node);
                        break;

                    case LCMP:
                        handler.onLCmp(node);
                        break;

                    case FCMPL: case FCMPG:
                        handler.onFCmp(node);
                        break;

                    case DCMPL: case DCMPG:
                        handler.onDCmp(node);
                        break;

                    case ARRAYLENGTH:
                        handler.onArrayLength(node);
                        break;

                    // 64-bit 2-operand ALU operations
                    case IADD: case ISUB: case IMUL: case IDIV: case IREM: case IAND: case IOR: case IXOR:
                    case ISHL: case ISHR: case IUSHR:
                        handler.onAluI32(node);
                        break;

                    case FADD: case FSUB: case FMUL: case FDIV: case FREM:
                        handler.onAluF32(node);
                        break;

                    // 64-bit 2-operand ALU operations
                    case LADD: case LSUB: case LMUL: case LDIV: case LREM: case LAND: case LOR: case LXOR:
                        handler.onAluI64(node);
                        break;

                    case DADD: case DSUB: case DMUL: case DDIV: case DREM:
                        handler.onAluF64(node);
                        break;

                    // 64-bit shifts
                    case LSHL: case LSHR: case LUSHR:
                        handler.onShift64(node);
                        break;

                    // Unary 32-bit operations
                    case INEG: // -int
                    case I2B: case I2C: case I2S:  // Cast integer to smaller types. We don't track these.
                        handler.onAluToI32Unary(node);
                        break;

                    // Unary 32-bit operations
                    case FNEG: // -float

                        handler.onAluToF32Unary(node);
                        break;

                    // Unary 64-bit operations
                    case LNEG:   // -long
                        handler.onAluToI64Unary(node);
                        break;

                    // Unary 64-bit operations
                    case DNEG:   // -double
                        handler.onAluToF64Unary(node);
                        break;

                    // Same size casts
                    case F2I:  // cast: float -> int
                        handler.onCastF32I32(node);
                        break;

                    case I2F:  // cast: int -> float
                        handler.onCastI32F32(node);
                        break;

                    case D2L:   // cast: double -> long
                        handler.onCastF64I64(node);
                        break;

                    case L2D:   // cast: long -> double
                        handler.onCastI64F64(node);
                        break;

                    // 32 to 64 casts
                    case I2L:     // cast: 32 bit type to long
                    case F2L:
                        handler.onCast32toI64(node);
                        break;

                    case I2D:
                    case F2D:     // casts 32 bit type to double
                        handler.onCast32toF64(node);
                        break;

                    // 64 to 32 casts
                    case D2I:     // casts long, double -> int
                    case L2I:
                        handler.onCast64toI32(node);
                        break;

                    case D2F:
                    case L2F:     // casts long, double -> float
                        handler.onCast64toF32(node);
                        break;

                    // TODO: Maybe left out some arithmetic instructions

                    case MONITORENTER:
                        handler.onMonitorEnter(node);
                        break;

                    case MONITOREXIT:
                        handler.onMonitorExit(node);
                        break;

                    default:
                        log.err("Unsupported basic instruction: %d", op);
                }

                break;
            }
            case AbstractInsnNode.INT_INSN: {
                // http://asm.ow2.org/asm50/javadoc/user/org/objectweb/asm/tree/IntInsnNode.html
                IntInsnNode node = (IntInsnNode) node0;
                if (logTrace) {
                    log.trace("\t%3s  IntInsn: %s : line %d", node.getOpcode(), node.operand, currentLine());
                }

                int op = node.getOpcode();
                switch (op) {
                    // Load numeric values onto the stack
                    case BIPUSH: case SIPUSH:
                        handler.onIPush(node);
                        break;

                    case NEWARRAY:
                        handler.onNewArray(node);
                        break;

                    default:
                        log.err("Unexpected IntInsnNode instruction: %d", op);
                }
                break;
            }

            case AbstractInsnNode.VAR_INSN: {
                // http://asm.ow2.org/asm50/javadoc/user/org/objectweb/asm/tree/VarInsnNode.html
                // Compact forms of *LOAD/*STORE 0..3 are handled by ASM

                VarInsnNode node = (VarInsnNode) node0;
                if (logTrace) {
                    log.trace("\t%3s VarInsn Var: %s : line %d", node.getOpcode(), node.var, currentLine());
                }

                int op = node.getOpcode();
                switch (op) {
                    case ALOAD:
                        handler.onALoad(node);
                        break;

                    case ILOAD:
                        handler.onLoadI32(node);
                        break;

                    case LLOAD:
                        handler.onLoadI64(node);
                        break;

                    case FLOAD:
                        handler.onLoadF32(node);
                        break;

                    case DLOAD:
                        handler.onLoadF64(node);
                        break;

                    case ASTORE:
                        handler.onAStore(node);

                        break;

                    case ISTORE:
                    case FSTORE:
                        handler.onStore32(node);
                        break;

                    case DSTORE:
                    case LSTORE:
                        handler.onStore64(node);
                        break;

                    case RET:
                        handler.onRet(node);
                        break;

                    default:
                        log.err("Unexpected VarInsnNode instruction: %d", op);
                }

            }

            break;

            case AbstractInsnNode.TYPE_INSN: {
                // http://asm.ow2.org/asm50/javadoc/user/org/objectweb/asm/tree/TypeInsnNode.html

                TypeInsnNode node = (TypeInsnNode) node0;
                log.trace("\t%3s TypeInsn: %s : line %d", node.getOpcode(), node.desc, currentLine());
                int op = node.getOpcode();
                // TODO: Check for new or newarray of replaced values
                switch (node.getOpcode()) {
                    case ANEWARRAY:
                        handler.onANewArray(node);
                        break;

                    case NEW:
                        handler.onNew(node);
                        break;

                    case CHECKCAST:
                        handler.onCheckCast(node);
                        break;

                    case INSTANCEOF:
                        handler.onInstanceOf(node);
                        break;

                    default:
                        log.err("Unexpected TypeInsnNode instruction: %d", op);
                }
                break;
            }

            case AbstractInsnNode.FIELD_INSN: {
                // http://asm.ow2.org/asm50/javadoc/user/org/objectweb/asm/tree/FieldInsnNode.html
                FieldInsnNode node = (FieldInsnNode) node0;
                if (logTrace) {
                    log.trace("\t%3s FieldInsn: %s : %s : line %d", node.getOpcode(),
                            node.owner + '.' + node.name, node.desc, currentLine());
                }

                int op = node.getOpcode();
                switch (op) {
                    case GETFIELD:
                        handler.onGetField(node);
                        break;

                    case GETSTATIC:
                        handler.onGetStatic(node);
                        break;

                    case PUTSTATIC:
                        handler.onPutStatic(node);
                        break;

                    case PUTFIELD:
                        handler.onPutField(node);
                        break;

                    default:
                        log.err("Unexpected FieldInsnNode instruction: %d", op);
                }

                break;
            }

            case AbstractInsnNode.METHOD_INSN: {

                // INVOKEVIRTUAL, INVOKERSPECIAL, INVOKESTATIC, INVOKEINTERFACE, INVOKEDYNAMIC
                MethodInsnNode node = (MethodInsnNode) node0;
                if (logTrace) {
                    log.trace("\t%3s (%s) : line %d", node.getOpcode(),
                            node.owner + '.' + node.name + node.desc, currentLine());
                }

                handler.onMethod(node);
                break;
            }

            case AbstractInsnNode.INVOKE_DYNAMIC_INSN: {
                InvokeDynamicInsnNode node = (InvokeDynamicInsnNode) node0;
                if (logTrace) {
                    log.trace("\t%3s InvokeDynamic: %s : line %d", node.getOpcode(), node.name, currentLine());
                }

                handler.onInvokeDynamic(node);
                break;
            }

            case AbstractInsnNode.JUMP_INSN: {
                JumpInsnNode node = (JumpInsnNode) node0;
                int op = node.getOpcode();
                if (logTrace) {
                    log.trace("\t%3s JumpInsn -> %s : line %d", node.getOpcode(),
                            node.label.getLabel().toString(), currentLine());
                }

                switch (op) {
                    case IF_ICMPEQ:
                    case IF_ICMPNE:
                    case IF_ICMPLT:
                    case IF_ICMPGE:
                    case IF_ICMPGT:
                    case IF_ICMPLE:
                        handler.onIfICmpXX(node);
                        break;

                    case IFEQ: case IFNE: case IFLT: case IFGE: case IFGT: case IFLE:
                        handler.onIfXX(node);
                        break;

                    case IFNULL:
                        handler.onIfNull(node, true);
                        break;

                    case IFNONNULL:
                        handler.onIfNull(node, false);
                        break;

                    case IF_ACMPEQ:
                        handler.onIfACmpEq(node, true);
                        break;

                    case IF_ACMPNE:
                        handler.onIfACmpEq(node, false);
                        break;

                    case GOTO:
                        handler.onGoto(node);
                        break;

                    case JSR:
                        handler.onJsr(node);
                        break;

                    default:
                        log.err("Unexpected Jump instruction: %s", op);
                }
                break;
            }

            case AbstractInsnNode.LABEL: {
                LabelNode node = (LabelNode) node0;
                log.trace("%s:",  node.getLabel().toString());
                handler.onLabel(node);
                break;
            }

            // Covers all 3 variants of LDC
            case AbstractInsnNode.LDC_INSN: {
                LdcInsnNode node = (LdcInsnNode) node0;
                log.trace("\t%3s LDC(%s)", node.getOpcode(), node.cst.toString());
                handler.onLdc(node);
                break;
            }

            case AbstractInsnNode.IINC_INSN: {
                IincInsnNode node = (IincInsnNode) node0;
                log.trace("\t%3s IINC: var[%s] += %s", node.getOpcode(), node.var, node.incr);
                handler.onIInc(node);
                break;
            }

            case AbstractInsnNode.TABLESWITCH_INSN: {
                TableSwitchInsnNode node = (TableSwitchInsnNode) node0;
                log.trace("\t%3s TableSwitch", node.getOpcode());
                handler.onTableSwitch(node);
                break;
            }

            case AbstractInsnNode.LOOKUPSWITCH_INSN: {
                LookupSwitchInsnNode node = (LookupSwitchInsnNode) node0;
                log.trace("\t%3s LookupSwitch", node.getOpcode());
                handler.onLookupSwitch(node);
                break;
            }

            case AbstractInsnNode.MULTIANEWARRAY_INSN: {
                MultiANewArrayInsnNode node = (MultiANewArrayInsnNode) node0;
                log.trace("\t%3s MultiANewArray", node.getOpcode());
                handler.onMultiANewArray(node);
                //log.err("MULTIANEWARRAY"); // TODO: MULTIANEWARRAY
                break;
            }

            case AbstractInsnNode.FRAME: {
                FrameNode node = (FrameNode) node0;
                log.trace("\t%3s Frame", node.getOpcode());
                handler.onFrame(node);
                break;
            }

            case AbstractInsnNode.LINE: {
                LineNumberNode node = (LineNumberNode) node0;
                if (log.on(log.TRACE)) {
                    if (null == currentLineNode) {
                        log.trace("%s%d:", firstLineLogPrefix, node.line);
                    } else {
                        log.trace("%d:", node.line);
                    }
                }

                currentLineNode = node;
                handler.onLine(node);
                break;
            }

        }

    }


    private void setHandler(OpcodeProcessor handler) {
        this.handler = handler;
    }

    public void processMethodCode(InsnList instructions, OpcodeProcessor handler) {
        setHandler(handler);
        currentLineNode = null;

        for (AbstractInsnNode node = instructions.getFirst(), nextNode; null != node; node = nextNode) {
            // Note that we save the next node after the current one
            // so if new nodes are added, before or after current node, they are not processed
            nextNode = node.getNext();
            processNode(node);
        }

        currentLineNode = null;
    }


    public void processMethodVars(List vars, OpcodeProcessor handler) {

        setHandler(handler);
        for (Object i : vars) {
            LocalVariableNode variable = (LocalVariableNode) i;
            handler.onVar(variable);
        }

        handler.onVarProcessingEnd(vars);
    }


    public void processMethod(MethodNode methodNode, OpcodeProcessor handler) {

        setHandler(handler);
        setLogLevel(log.getLevel());
        List<LocalVariableNode> vars = methodNode.localVariables;
        if (null == vars) {
            vars = new ArrayList<>();
        }

        handler.onStart(methodNode);
        processMethodVars(vars, handler);
        processMethodCode(methodNode.instructions, handler);
        handler.onEnd();
    }


    public int currentLine() {

        return currentLineNode != null ? currentLineNode.line : -1;
    }

    public void setLogLevel(int logLevel) {

        log.setLogLevel(logLevel);
        logDbg = log.on(log.DBG);
        logTrace = log.on(log.TRACE);
    }
}
