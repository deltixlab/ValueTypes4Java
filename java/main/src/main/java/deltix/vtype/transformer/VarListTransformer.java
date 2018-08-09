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

import deltix.vtype.mapping.Mapping;
import deltix.vtype.type.DescriptorParser;
import deltix.vtype.type.TypeId;
import deltix.vtype.common.CrudeLogger;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;

import java.util.*;

import static deltix.vtype.transformer.AsmUtil.dumpVarNodes;
import static deltix.vtype.type.TypeId.isRefSrc;
import static deltix.vtype.type.TypeId.isVt;
import static deltix.vtype.type.TypeId.isVtRef;

/**
 * Update debug information in accordance to changes made by Value Type transformation process.
 * May need to create new var entries.
 */
public class VarListTransformer {
    private final Mapping mapping;
    private boolean logDbg;
    CrudeLogger log;

    private List<LocalVariableNode> vars;
    private final PriorityQueue<VarNodeWrapper> in;
    private final PriorityQueue<VarNodeWrapper> active;
    private final List<LocalVariableNode> out;
    private final IdentityHashMap<LabelNode, Integer> labelMap;

    private int labelIndex;
    private boolean translated;


    int getLabelIndex(LabelNode label) {
        Integer idx;
        return null == labelMap || null == (idx = labelMap.get(label)) ? -1 : idx;
    }


    class VarNodeWrapper {
        private final LocalVariableNode varNode;
        private final int start;
        private final int end;
        boolean remapped;
        public int typeId;
        public int src;

        VarNodeWrapper(LocalVariableNode varNode) {
            this.varNode = varNode;
            this.src = varNode.index;
            this.start = getLabelIndex(varNode.start);
            this.end = getLabelIndex(varNode.end);
            this.typeId = TypeId.VOID;
        }

        VarNodeWrapper deepCopy() {
            return new VarNodeWrapper(new LocalVariableNode(varNode.name, varNode.desc, varNode.signature,
                    varNode.start, varNode.end, varNode.index ));
        }

        @Override
        public String toString() {
            return String.format("%s %s; @%d: %d .. %d", varNode.desc, varNode.name, varNode.index, start, end);
        }

        void updateType(int typeId) {
            if (typeId != this.typeId) {
                this.typeId = typeId;
                // We only change actual type for VType vars
                if (isVt(typeId)) {
                    varNode.desc = mapping.typeIdToDstTypeDesc(typeId);
                }
            }
        }

        void remap(int dst, int typeId) {

            remapped = true;
            varNode.index = dst;
            updateType(typeId);
        }

        public VarNodeWrapper split(LabelNode startLabel) {

            // there are no indices for the new node, but, at this point, v.end is never going to be accessed,
            // and this.start is never going to be accessed either
            VarNodeWrapper v = deepCopy();
            v.varNode.end = startLabel;
            this.varNode.start = startLabel;
            return v;
        }
    };


    public void setLogLevel(int logLevel) {

        log.setLogLevel(logLevel);
        logDbg = log.on(log.DBG);
    }


    public VarListTransformer(boolean noDebugInfo, Mapping mapping, CrudeLogger log) {

        this.vars = null;
        this.mapping = mapping;
        this.log = log;
        logDbg = log.on(log.DBG);

        if (noDebugInfo) {
            in = null;
            active = null;
            out = null;
            labelMap = null;
            return;
        }

        in = new PriorityQueue<>(16, new Comparator<VarNodeWrapper>() {
            @Override
            public int compare(VarNodeWrapper o1, VarNodeWrapper o2) {
                return Integer.compare(o1.start, o2.start);
            }
        });

        active = new PriorityQueue<>(16, new Comparator<VarNodeWrapper>() {
            @Override
            public int compare(VarNodeWrapper o1, VarNodeWrapper o2) {
                return Integer.compare(o1.end, o2.end);
            }
        });

        labelMap = new IdentityHashMap<>(32);
        labelIndex = 0;
        out = new ArrayList<>(32);
    }


    public void init(List<LocalVariableNode> localVariables) {

        this.vars = localVariables;
        if (null != in) {
            in.clear();
            active.clear();
            out.clear();
            labelMap.clear();
        }

        translated = false;
        if (logDbg) {
            dumpVarNodes(localVariables);
        }
    }


    public void registerLabel(LabelNode label) {

        if (null != labelMap) {
            labelMap.put(label, ++labelIndex);
        }
    }


    public void onLabel(LabelNode label) {
        int idx;
        VarNodeWrapper v;

        if (null == vars || -1 == (idx = getLabelIndex(label)))
            return;

        log.dbg("varTransformer: Label %d", idx);

        while ((v = active.peek()) != null && v.end <= idx) {

            if (TypeId.VOID == v.typeId) {
                log.wrn("Variable [%s %s] wasn't transformed until the end Label", v.varNode.desc, v.varNode.name);
                int typeId = DescriptorParser.getClassTypeId(v.varNode.desc, mapping);
                v.updateType(typeId);
            }

            out.add(active.remove().varNode);
        }

        while ((v = in.peek()) != null && v.start <= idx) {
            active.add(in.remove());
        }
    }


    public String getVarNameBySrcIndex(int aSrc) {

        if (null != active && null != vars) for (VarNodeWrapper v : active) {
            if (v.src == aSrc) {
                return v.varNode.name;
            }
        }

        return null;
    }


    public void remapIndex(LabelNode anchorLabel, int typeId, int aSrc, int aDst) {

        if (null == in || null == vars)
            return;

        if (null != anchorLabel) {
            onLabel(anchorLabel);
        }

        for (VarNodeWrapper v : active) {
            if (v.src == aSrc) {
                // If this the first occurence of this variable?
                if (v.remapped && ((isVt(typeId) && v.typeId != typeId) || v.varNode.index != aDst)) {
                    if (null == anchorLabel) {
                        log.wrn("Variable [%s %s]: attempting split without anchor LabelNode !", v.varNode.desc, v.varNode.name);
                    } else if (v.varNode.start != anchorLabel) {
                        if (isVtRef(typeId) && isRefSrc(v.typeId) && v.varNode.index == aDst) {
                            // TODO: What if var is actually Object?
                            // TODO: generate a specific warning?
                            log.dbg("VType ref variable [%s %s] type is updated: %s -> %s",
                                    v.varNode.desc, v.varNode.name,
                                    mapping.typeIdToShortPrintableString(v.typeId), mapping.typeIdToShortPrintableString(typeId)
                            );
                        } else {
                            log.dbg("Variable [%s %s] is split!", v.varNode.desc, v.varNode.name);
                            out.add(v.split(anchorLabel).varNode);
                        }
                    }
                }

                v.remap(aDst, typeId);
            }
        }
    }


    public void startTransformation() {

        if (null != in && null != vars) {
            in.clear();
            active.clear();
            translated = false;

            for (LocalVariableNode i : vars) {
                in.add(new VarNodeWrapper(i));
            }
        }
    }


    public List<LocalVariableNode> getResult() {

        if(null == in || null == vars)
            return null;

        if (!translated) {
            translated = true;
            assert(0 == in.size());
            assert(0 == active.size());
            if (logDbg) {
                dumpVarNodes(out);
            }

            return out;
        }

        return vars;
    }
}
