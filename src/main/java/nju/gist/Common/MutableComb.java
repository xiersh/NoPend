package nju.gist.Common;

public class MutableComb extends Comb {
    public MutableComb(int n) {
        super(n);
    }

    public MutableComb(Comb comb) {
        super(comb.elements);
    }

    // 1. Setter
    public void set(int i, int value) {
        super.elements.set(i, value);
    }

    // 2. clone
    public MutableComb clone() {
        return new MutableComb(this);
    }
}
