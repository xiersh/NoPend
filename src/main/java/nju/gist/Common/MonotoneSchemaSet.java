package nju.gist.Common;

import java.util.*;

public class MonotoneSchemaSet {
    private final List<Schema> schemas;
    private final boolean isMinimal;

    public MonotoneSchemaSet(boolean isMinimal) {
        this.schemas = new LinkedList<>();
        this.isMinimal = isMinimal;
    }

    public boolean add(Schema newschema) {
        Iterator<Schema> iter = schemas.iterator();
        Schema cur = null;
        if (isMinimal) { // minimal schema set
            while (iter.hasNext()) {
                cur = iter.next();
                if (cur.equals(newschema)) { // already exist
                    return false;
                } else if (cur.isSubsetOf(newschema)) { // should not add newscema
                    return false;
                } else if (cur.isSupersetOf(newschema)) { // remove current schema
                    iter.remove();
                }
            }
            schemas.add(newschema);
            return true;
        } else { // maximal schema set
            while (iter.hasNext()) {
                cur = iter.next();
                if (cur.equals(newschema)) { // already exist
                    return false;
                } else if (cur.isSupersetOf(newschema)) { // should not add
                    return false;
                } else if (cur.isSubsetOf(newschema)) { // remove current schema
                    iter.remove();
                }
            }
            schemas.add(newschema);
            return true;
        }
    }

    public boolean remove(Schema removedSchema) {
        return schemas.remove(removedSchema);
    }

    public int size() {
        return schemas.size();
    }

    public List<Schema> getSchemas() { // immutable
        return Collections.unmodifiableList(schemas);
    }

    public MonotoneSchemaSet complement() {
        MonotoneSchemaSet result = new MonotoneSchemaSet(!isMinimal);
        for (Schema schema : schemas) {
            result.add(schema.complement());
        }
        return result;
    }

    public MonotoneSchemaSet complement(Schema universe) {
        MonotoneSchemaSet result = new MonotoneSchemaSet(!isMinimal);
        for (Schema schema : schemas) {
            result.add(schema.complement(universe));
        }
        return result;
    }

    static public MonotoneSchemaSet getMHS(MonotoneSchemaSet schemaSet) {
        if (schemaSet.size() == 0) {
            throw new IllegalArgumentException("schemaSet cannot be empty");
        } else if (schemaSet.size() == 1 && schemaSet.getSchemas().getFirst().size() == 0) {
            throw new IllegalArgumentException("schemaSet cannot just have a single empty set");
        }

        MonotoneSchemaSet MHS = new MonotoneSchemaSet(true); // minimal
        MutableSchema ans = new MutableSchema(schemaSet.getSchemas().getFirst().getLogicSize());
        getMHS_recursive(ans, MHS, Collections.unmodifiableList(schemaSet.schemas), 0);

        return MHS;
    }

    static private void getMHS_recursive(MutableSchema ans, MonotoneSchemaSet MHS, List<Schema> schemaSet, int index) {
        if (index == schemaSet.size()) {
            MHS.add(new Schema(ans)); // ans is already a hitting set
            return;
        }
        Schema cur = schemaSet.get(index); // iter now points to next
        if (ans.overlapsWith(cur)) {
            getMHS_recursive(ans, MHS, schemaSet, index + 1);
        } else {
            List<Integer> indices = cur.getIndices();
            for (int i : indices) {
                ans.set(i, true);
                getMHS_recursive(ans, MHS, schemaSet, index + 1);
                ans.set(i, false);
            }
        }
    }

    public Iterator<Schema> iterator() { // immutable
        return Collections.unmodifiableList(schemas).iterator();
    }

    public boolean isEmpty() {
        return schemas.isEmpty();
    }
}
