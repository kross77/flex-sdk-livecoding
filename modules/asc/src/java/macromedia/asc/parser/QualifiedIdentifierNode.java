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
public class QualifiedIdentifierNode extends IdentifierNode
{
	public Node qualifier;
	public boolean is_config_name;

	public QualifiedIdentifierNode(Node qualifier, String name, int pos)
	{
		super(name != null ? name : "", pos);
		if (name != null)
		{
			assert name.intern() == name;
		}
		this.qualifier = qualifier;
		this.is_config_name =false;
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

	public String toString()
	{
      if(Node.useDebugToStrings)
         return "QualifiedIdentifier@" + pos() + ": " + (name != null ? name : "");
      else
         return "QualifiedIdentifier";
	}

	public boolean isConfigurationName()
	{
		return this.is_config_name;
	}

    public QualifiedIdentifierNode clone() throws CloneNotSupportedException
    {
        QualifiedIdentifierNode result = (QualifiedIdentifierNode) super.clone();

        if (qualifier != null) result.qualifier = qualifier.clone();

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        QualifiedIdentifierNode that = (QualifiedIdentifierNode) o;

        if (is_config_name != that.is_config_name) return false;
        if (qualifier != null ? !qualifier.equals(that.qualifier) : that.qualifier != null) return false;

        return true;
    }

//    @Override
//    public int hashCode() {
//        int result = super.hashCode();
//        result = 31 * result + (qualifier != null ? qualifier.hashCode() : 0);
//        result = 31 * result + (is_config_name ? 1 : 0);
//        return result;
//    }
}
