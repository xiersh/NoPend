package nju.gist.Common;

public class MutableSchema extends Schema {
    public MutableSchema(int size) {
        super(size);
    }

    public MutableSchema(Schema schema) {
        super(schema);
    }

    public MutableSchema(int size, boolean flag) {
        super(size, flag);
    }

    public void set(int index, boolean value) {
        bitset.set(index, value);
    }

    public void setComplement() {
        for (int i=0; i<bitset.size(); i++) {
            bitset.set(i, !bitset.get(i));
        }
    }
}
