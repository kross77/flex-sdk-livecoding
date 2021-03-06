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
import macromedia.asc.util.*;
import macromedia.asc.semantics.*;

/**
 * Node
 */
public class PackageIdentifiersNode extends Node
{
	private static final int IS_DEFINITION_FLAG = 1;
	
	public ObjectList<IdentifierNode> list = new ObjectList<IdentifierNode>(5);
	public String pkg_part;
	public String def_part;

	public PackageIdentifiersNode(IdentifierNode item, int pos, boolean isDefinition)
	{
		super(pos);
		list.add(item);
		if (isDefinition)
		{
			flags |= IS_DEFINITION_FLAG;
		}
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

	int size()
	{
		return this.list.size();
	}

	public int pos()
	{
		return list.size() != 0 ? list.last().pos() : 0;
	}

	// Used externally by SyntaxTreeDumper
	public boolean isDefinition()
	{
		return (flags & IS_DEFINITION_FLAG) != 0;
	}

	public String toString()
	{
		return "PackageIdentifiers";
	}

    void clearIdentifierString()
    {
        if (pkg_part != null )
        {
            pkg_part = null;
        }
        if (def_part != null)
        {
            def_part = null;
        }
    }

    private static final String ASTERISK = "*".intern();

    public String toIdentifierString()
    {
        if( pkg_part == null )
        {
            StringBuilder buf = new StringBuilder();
            //ListIterator<IdentifierNode> it = list.listIterator();            
            //while( it.hasNext() )
			int len = list.size();
			for(int x=0; x < len; x++)
            {
				IdentifierNode item = list.get(x);
				if (x == len - 1 && isDefinition())
				{
					def_part = "";
					if (ASTERISK != item.name)
						def_part = item.name;
				}
				else
				{
					if( buf.length() > 0 )
					{
						buf.append(".");
					}
					buf.append(item.toIdentifierString());
				}
            }
            pkg_part = buf.toString();
        }

        return pkg_part;
    }

    public PackageIdentifiersNode clone() throws CloneNotSupportedException
    {
        PackageIdentifiersNode result = (PackageIdentifiersNode) super.clone();

        // def_part is String
        //if (def_part != null);
        // pkg_part is String
        //if (pkg_part != null);
        if (list != null) result.list = CloneUtil.cloneListINode(list);

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PackageIdentifiersNode that = (PackageIdentifiersNode) o;

        if (def_part != null ? !def_part.equals(that.def_part) : that.def_part != null) return false;
        if (list != null ? !list.equals(that.list) : that.list != null) return false;
        if (pkg_part != null ? !pkg_part.equals(that.pkg_part) : that.pkg_part != null) return false;

        return true;
    }

//    @Override
//    public int hashCode() {
//        int result = super.hashCode();
//        result = 31 * result + (list != null ? list.hashCode() : 0);
//        result = 31 * result + (pkg_part != null ? pkg_part.hashCode() : 0);
//        result = 31 * result + (def_part != null ? def_part.hashCode() : 0);
//        return result;
//    }
}
