package nju.gist.Common;

public class MutableTestcase extends Testcase {
    public MutableTestcase(Testcase testcase) {
        super(testcase);
    }

    public MutableTestcase(Comb comb) {
        super(comb);
    }

    // mutator
    public void flip(int i) {
        if (i < 0 || i >= elements.size()) {
            throw new IllegalArgumentException("Index out of bounds");
        }
        switch (elements.get(i)) {
            case 0: elements.set(i, 1); break;
            case 1: elements.set(i, 0); break;
            default: throw new IllegalArgumentException("Not Two Values System!");
        }
    }

    public void set(int i, int value) {
        if (i < 0 || i >= elements.size()) {
            throw new IllegalArgumentException("Index out of bounds");
        }
        if (value != 0 && value != 1) {
            throw new IllegalArgumentException("Not Two Values System!");
        }
        elements.set(i, value);
    }
}
