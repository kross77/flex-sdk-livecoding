/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package macromedia.asc.parser;

import macromedia.asc.util.*;
import macromedia.asc.semantics.*;

/**
 * Node
 */
public class CatchClauseNode extends Node
{
	public Node parameter;
	public StatementListNode statements;
	public ReferenceValue typeref;
	public boolean finallyInserted;
    public ObjectValue default_namespace;
    public ObjectValue activation;
    
	public CatchClauseNode(Node parameter, StatementListNode statements)
	{
		this.parameter = parameter;
		this.statements = statements;
		this.typeref = null;
		this.finallyInserted = false;
	}

	public Value evaluate(Context cx, Evaluator evaluator)
	{
		if (evaluator.checkFeature(cx, this))
		{
			return evaluator.evaluate(cx, this);
		}
		else
		{
			return null;
		}
	}

	public int countVars()
	{
		// Add 1 for the catch variable
		return 1 + (statements != null ? statements.countVars() : 0);
	}
	
	public String toString()
	{
		return "CatchClause";
	}

    public CatchClauseNode clone() throws CloneNotSupportedException
    {
        CatchClauseNode result = (CatchClauseNode) super.clone();

        if (activation != null) result.activation = activation.clone();
        if (default_namespace != null) result.default_namespace = default_namespace.clone();
        if (parameter != null) result.parameter = parameter.clone();
        if (statements != null) result.statements = statements.clone();
        if (typeref != null) result.typeref = typeref.clone();

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CatchClauseNode that = (CatchClauseNode) o;

        if (finallyInserted != that.finallyInserted) return false;
        if (activation != null ? !activation.equals(that.activation) : that.activation != null) return false;
        if (default_namespace != null ? !default_namespace.equals(that.default_namespace) : that.default_namespace != null)
            return false;
        if (parameter != null ? !parameter.equals(that.parameter) : that.parameter != null) return false;
        if (statements != null ? !statements.equals(that.statements) : that.statements != null) return false;
        if (typeref != null ? !typeref.equals(that.typeref) : that.typeref != null) return false;

        return true;
    }

//    @Override
//    public int hashCode() {
//        int result = super.hashCode();
//        result = 31 * result + (parameter != null ? parameter.hashCode() : 0);
//        result = 31 * result + (statements != null ? statements.hashCode() : 0);
//        result = 31 * result + (typeref != null ? typeref.hashCode() : 0);
//        result = 31 * result + (finallyInserted ? 1 : 0);
//        result = 31 * result + (default_namespace != null ? default_namespace.hashCode() : 0);
//        result = 31 * result + (activation != null ? activation.hashCode() : 0);
//        return result;
//    }
}
