/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package flex2.compiler.mxml.rep.decl;

import flex2.compiler.util.NameFormatter;

/**
 * This class represents a property, without an initializer.
 */
public class UninitializedPropertyDeclaration implements PropertyDeclaration {
    private final String name;
    private final String typeName;
    private final int lineRef;
    private final boolean inspectable;
    private final boolean topLevel;
    private final boolean idIsAutogenerated;
    private final boolean bindabilityEnsured;
    private final String comment; // primitive type can be declared in mxml and can be uninitialized. we don't do a registerModel, so storing the comment here

    public UninitializedPropertyDeclaration(String name, String typeName, int lineRef, boolean inspectable, boolean topLevel, boolean idIsAutogenerated, boolean isBindable, String comment) {
        this.name = name;
        this.typeName = typeName;
        this.lineRef = lineRef;
        this.inspectable = inspectable;
        this.topLevel = topLevel;
        this.idIsAutogenerated = idIsAutogenerated;
        this.bindabilityEnsured = isBindable;
        this.comment = comment;
    }

    public int getLineRef() {
        return lineRef;
    }

    public String getName() {
        return name;
    }

    public String getTypeExpr() {
        return NameFormatter.toDot(typeName);
    }

    public boolean getInspectable() {
        return inspectable;
    }

    public boolean getTopLevel() {
        return topLevel;
    }

    public boolean getIdIsAutogenerated() {
        return idIsAutogenerated;
    }

    public boolean getBindabilityEnsured() {
        return bindabilityEnsured;
    }

    public String getComment() {
        return comment;
    }
}
