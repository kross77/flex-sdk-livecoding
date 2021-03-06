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

import macromedia.asc.parser.util.CloneUtil;
import macromedia.asc.util.Context;
import macromedia.asc.semantics.Value;
import macromedia.asc.semantics.QName;

import java.util.List;
import java.util.ArrayList;

/**
 * Node
 */
public class BinaryProgramNode extends ProgramNode
{
	public BinaryProgramNode(Context cx, StatementListNode statements)
	{
        super( cx, statements );
	}

    // This is used by Flash Authoring - don't remove without checking with them
	public List<QName> toplevelDefinitions = new ArrayList<QName>();
	
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

	public String toString()
	{
		return "BinaryProgram";
	}

    public BinaryProgramNode clone() throws CloneNotSupportedException
    {
        BinaryProgramNode result = (BinaryProgramNode) super.clone();

        if (toplevelDefinitions != null) result.toplevelDefinitions = CloneUtil.cloneListQName(toplevelDefinitions);

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BinaryProgramNode that = (BinaryProgramNode) o;

        if (toplevelDefinitions != null ? !toplevelDefinitions.equals(that.toplevelDefinitions) : that.toplevelDefinitions != null)
            return false;

        return true;
    }

//    @Override
//    public int hashCode() {
//        int result = super.hashCode();
//        result = 31 * result + (toplevelDefinitions != null ? toplevelDefinitions.hashCode() : 0);
//        return result;
//    }
}
