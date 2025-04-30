package nju.gist.Common;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;


public class Schema {
    protected final BitSet bitset;
    private final int logic_size;


    /* 1. Constructor */
    public Schema(int size) {
        bitset = new BitSet(size);
        logic_size = size;
    }

    public Schema(int size, boolean flag) {
        this(size);
        if (flag) {
            bitset.set(0, size);
        }
    }

    public Schema(Schema other) {
        bitset = (BitSet) other.bitset.clone();
        logic_size = other.logic_size;
    }

    public Schema(Comb comb) {
        this(comb.size());
        for (int i = 0; i < comb.size(); i++) {
            if (comb.get(i) != Comb.UNKNOWN) {
                bitset.set(i);
            }
        }
    }

    public Schema(int size, List<Integer> indices) {
        this(size);
        for (Integer index : indices) {
            bitset.set(index);
        }
    }

    /* 2. Other methods */

    public int getLogicSize() {
        return logic_size;
    }

    public boolean get(int index) {
        return bitset.get(index);
    }

    public int size() {
        return bitset.cardinality();
    }

    public Comb getComb(Testcase testcase) {
        MutableComb comb = new MutableComb(this.getLogicSize());
        for (int i = 0; i < this.getLogicSize(); i++) {
            if (bitset.get(i)) {
                comb.set(i, testcase.get(i));
            } else {
                comb.set(i, Comb.UNKNOWN);
            }
        }
        return new Comb(comb);
    }

    public List<Integer> getIndices() {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < this.getLogicSize(); i++) {
            if (bitset.get(i)) {
                indices.add(i);
            }
        }
        return indices;
    }

    /* 3. Operations and Operator */

    public boolean equals(Object other) {
        if (other instanceof Schema otherSchema) {
            return this.bitset.equals(otherSchema.bitset);
        }
        return false;
    }

    public Schema and(Schema other) {
        if (this.getLogicSize() != other.getLogicSize()) {
            throw new IllegalArgumentException("Schemas must have the same getLogicSize");
        }
        Schema result = new Schema(this);
        result.bitset.and(other.bitset);

        return result;
    }

    public Schema or(Schema other) {
        if (this.getLogicSize() != other.getLogicSize()) {
            throw new IllegalArgumentException("Schemas must have the same getLogicSize");
        }

        Schema result = new Schema(this.getLogicSize());
        for (int i = 0; i < this.getLogicSize(); i++) {
            result.bitset.set(i, this.bitset.get(i) || other.bitset.get(i));
        }
        return result;
    }

    public Schema complement() {
        Schema result = new Schema(this.getLogicSize());
        for (int i = 0; i < this.getLogicSize(); i++) {
            result.bitset.set(i, !this.bitset.get(i));
        }
        return result;
    }

    public Schema complement(Schema universe) {
        if (this.getLogicSize()!= universe.getLogicSize()) {
            throw new IllegalArgumentException("Schemas must have the same getLogicSize");
        } else if (!isSubsetOf(universe)) {
            throw new IllegalArgumentException("Universe must be the super set of this schema");
        }

        Schema result = new Schema(this.getLogicSize());
        for (int i = 0; i < this.getLogicSize(); i++) {
            if (universe.bitset.get(i)) {
                result.bitset.set(i, !this.bitset.get(i));
            }
        }
        return result;
    }

    public boolean isSubsetOf(Schema other) {
        if (this.getLogicSize() != other.getLogicSize()) {
            throw new IllegalArgumentException("Schemas must have the same getLogicSize");
        }

        return this.and(other).equals(this);
    }

    public boolean isSupersetOf(Schema other) {
        if (this.getLogicSize() != other.getLogicSize()) {
            throw new IllegalArgumentException("Schemas must have the same getLogicSize");
        }

        return other.isSubsetOf(this);
    }

    public boolean overlapsWith(Schema other) {
        if (this.getLogicSize() != other.getLogicSize()) {
            throw new IllegalArgumentException("Schemas must have the same getLogicSize");
        }

        for (int i = 0; i < this.getLogicSize(); i++) {
            if (this.bitset.get(i) && other.bitset.get(i)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return logic_size + ":" + getIndices().toString();
    }

}
