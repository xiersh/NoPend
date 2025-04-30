package nju.gist.Common;

import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class Comb {
    public static final int UNKNOWN = -1;
    protected List<Integer> elements;

    // 1. Constructor
    public Comb(int size) {
        elements = new ArrayList<Integer>(size);
        for (int i = 0; i < size; i++) {
            elements.add(UNKNOWN);
        }
    }

    /**
     * Input: [1,1,UNKNOWN,1,0]
     * @param comb
     */
    public Comb(List<Integer> comb) {
        elements = new ArrayList<Integer>(comb);
        for (Integer integer : comb) {
            if (integer < 0 && integer != UNKNOWN) {
                throw new IllegalArgumentException("Not a valid combination");
            }
        }
    }

    public Comb(Comb testcase, List<Integer> indices) {
        elements = new ArrayList<Integer>(testcase.size());
        for (int i = 0; i < testcase.size(); i++) {
            elements.add(UNKNOWN);
        }
        for (Integer index : indices) {
            elements.set(index, testcase.get(index));
        }
    }

    public Comb(Comb comb) {
        elements = new ArrayList<Integer>(comb.elements);
    }


    // 2. Getter
    public Integer get(int index) {
        return elements.get(index);
    }

    public int size() {
        return elements.size();
    }

    // 3. Binary Operations
    static public boolean contains(Comb A, Comb B) throws IllegalArgumentException {
        if (A.size() != B.size()) {
            throw new IllegalArgumentException("A.getLogicSize() != B.getLogicSize()");
        }
        for (int i = 0; i < A.size(); i++) {
            if (!Objects.equals(A.get(i), B.get(i)) && B.get(i) != UNKNOWN) {
                return false;
            }
        }
        return true;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Comb comb = (Comb) o;
        return Objects.equals(elements, comb.elements);
    }

    // 4. toString
    public String toString() {
        Iterator<Integer> it = this.elements.iterator();
        if (!it.hasNext())
            return "[]";

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (; ; ) {
            Integer e = it.next();
            sb.append(e.equals(UNKNOWN) ? "-" : e);
            if (!it.hasNext()) {
                sb.append(']');
                break;
            }
            sb.append(',');
        }
        return sb.toString();
    }

    // 5. Hash
    public int hashCode() {
        return elements.hashCode();
    }
}
