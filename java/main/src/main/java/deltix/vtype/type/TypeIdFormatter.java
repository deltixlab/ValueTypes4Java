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

public interface TypeIdFormatter {
    /**
     * Return short text name for the type. For debugging purposes.
     * @param typeId
     * @return readable short typename
     */
    String typeIdToShortPrintableString(int typeId);

    /**
     * Return Java type signature _before_ the transformation
     * @param typeId
     * @return full Java type signature
     */
    String typeIdToSrcTypeDesc(int typeId);

    /**
     * Return Java type signature _after_ the transformation
     * Different from typeIdToSrcTypeDesc if typeId belongs to transformed type.
     * May return different values for transformed type marked as value or as reference.
     * @param typeId
     * @return full Java type signature
     */
    String typeIdToDstTypeDesc(int typeId);


}
