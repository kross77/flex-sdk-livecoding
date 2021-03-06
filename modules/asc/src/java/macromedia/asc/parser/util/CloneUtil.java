package macromedia.asc.parser.util;

import macromedia.asc.parser.*;
import macromedia.asc.semantics.*;
import macromedia.asc.util.*;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: dimakruk
 * Date: 7/4/13
 * Time: 10:22 AM
 * To change this template use File | Settings | File Templates.
 */
public class CloneUtil {

    public static Namespaces cloneList(Namespaces list) throws CloneNotSupportedException
    {
        Namespaces clone = new Namespaces(list.size());
        for (ObjectValue item: list) clone.add(item != null ? item.clone() : null);
        return clone;
    }

    public static ObjectList<ReferenceValue> cloneReferenceValueList(ObjectList<ReferenceValue> list) throws CloneNotSupportedException
    {
        ObjectList<ReferenceValue> clone = new ObjectList<ReferenceValue>(list.size());
        for (ReferenceValue item: list) clone.add(item.clone());
        return clone;
    }

    public static ObjectList<ObjectValue> cloneObjectListList(ObjectList<ObjectValue> list) throws CloneNotSupportedException
    {
        ObjectList<ObjectValue> clone = new ObjectList<ObjectValue>(list.size());
        for (ObjectValue item: list) clone.add(item.clone());
        return clone;
    }

    public  static HashMap<TypeValue,ClassDefinitionNode> cloneHashMap(HashMap<TypeValue,ClassDefinitionNode> src) throws CloneNotSupportedException
    {
        HashMap<TypeValue, ClassDefinitionNode> dst = new HashMap<TypeValue, ClassDefinitionNode>();
        for (Map.Entry<TypeValue, ClassDefinitionNode> entry : src.entrySet()) {
            dst.put(entry.getKey(), entry.getValue().clone());
        }
        return dst;
    }

    public static Map<String, TypeValue> cloneMapS_TV(Map<String, TypeValue> src) throws CloneNotSupportedException
    {
        Map<String, TypeValue> dst = new HashMap<String, TypeValue> ();
        for (Map.Entry<String, TypeValue> entry : src.entrySet()) {
            //dst.put(new String(entry.getKey()), entry.getValue().clone());
            dst.put(new String(entry.getKey()), entry.getValue());
        }
        return dst;
    }

    public static Multinames cloneMultinames(Multinames src) throws CloneNotSupportedException
    {
        Multinames dst = new Multinames();
        for (Map.Entry<String, Namespaces> entry : src.entrySet()) {
            dst.put(entry.getKey(), cloneList(entry.getValue()));
        }
        return dst;
    }

    public static ObjectList<Node> cloneListNode(ObjectList<Node> src) throws CloneNotSupportedException
    {
        ObjectList<Node> dst = new ObjectList<Node>(src.size());
        for (Node item: src) dst.add(item.clone());
        return dst;
    }

    public static ObjectList<Value> cloneListValue(ObjectList<Value> src) throws CloneNotSupportedException
    {
        ObjectList<Value> dst = new ObjectList<Value>(src.size());
        for (Value item: src) dst.add(item.clone());
        return dst;
    }

    public static ObjectList<IdentifierNode> cloneListINode(ObjectList<IdentifierNode> src) throws CloneNotSupportedException
    {
        ObjectList<IdentifierNode> dst = new ObjectList<IdentifierNode>(src.size());
        for (IdentifierNode item: src) dst.add(item.clone());
        return dst;
    }

    public static ObjectList<String> cloneListString(ObjectList<String> src) throws CloneNotSupportedException
    {
        ObjectList<String> dst = new ObjectList<String>(src.size());
        for (String item: src) dst.add(new String(item));
        return dst;
    }

    public static ObjectList<ClassDefinitionNode> cloneListCDNode(ObjectList<ClassDefinitionNode> src) throws CloneNotSupportedException
    {
        ObjectList<ClassDefinitionNode> dst = new ObjectList<ClassDefinitionNode>(src.size());
        for (ClassDefinitionNode item: src) dst.add(item.clone());
        return dst;
    }

    public static ObjectList<Block> cloneListBlock(ObjectList<Block> src) throws CloneNotSupportedException
    {
        ObjectList<Block> dst = new ObjectList<Block>(src.size());
        for (Block item: src) dst.add(item.clone());
        return dst;
    }

    public static ObjectList<FunctionCommonNode> cloneListFCNode(ObjectList<FunctionCommonNode> src) throws CloneNotSupportedException
    {
        ObjectList<FunctionCommonNode> dst = new ObjectList<FunctionCommonNode>(src.size());
        if (src.size() != 0)
        {
            for (FunctionCommonNode item: src) dst.add(item.clone());
        }
        return dst;
    }

    public static ObjectList<ParameterNode> cloneListPNode(ObjectList<ParameterNode> src) throws CloneNotSupportedException
    {
        ObjectList<ParameterNode> dst = new ObjectList<ParameterNode>(src.size());
        for (ParameterNode item: src) dst.add(item.clone());
        return dst;
    }

    public static ObjectList<TypeInfo> cloneListTypeInfo(ObjectList<TypeInfo> src) throws CloneNotSupportedException
    {
        ObjectList<TypeInfo> dst = new ObjectList<TypeInfo>(src.size());
        for (TypeInfo item: src) dst.add(item.clone());
        return dst;
    }

    public static Set<ReferenceValue> cloneSet(Set<ReferenceValue> src) throws CloneNotSupportedException
    {
        HashSet<ReferenceValue> dst = new HashSet<ReferenceValue>(src.size());
        for (ReferenceValue item: src) {
            dst.add(item.clone());
        }
        return dst;
    }

    public static ObjectList<ImportNode> cloneListImNode(ObjectList<ImportNode> src) throws CloneNotSupportedException
    {
        ObjectList<ImportNode> dst = new ObjectList<ImportNode>(src.size());
        for (ImportNode item: src) dst.add(item.clone());
        return dst;
    }

    public static ObjectList<PackageDefinitionNode> cloneListPDNode(ObjectList<PackageDefinitionNode> src) throws CloneNotSupportedException
    {
        ObjectList<PackageDefinitionNode> dst = new ObjectList<PackageDefinitionNode>(src.size());
        for (PackageDefinitionNode item: src) dst.add(item.clone());
        return dst;
    }

    public static List<QName> cloneListQName(List<QName> src) throws CloneNotSupportedException
    {
        List<QName> dst = new ArrayList<QName>(src.size());
        for (QName item: src) dst.add(item.clone());
        return dst;
    }
}
