package nju.gist.Common;

import nju.gist.Util.Combinatorics;
import nju.gist.Util.Output;
import org.raistlic.common.permutation.Combination;

import java.util.*;

public class Testcase extends Comb {
    public Testcase(Comb comb) {
        super(comb);
        for (Integer integer : this.elements) {
            if (integer == Comb.UNKNOWN) {
                throw new IllegalArgumentException("This Combination is not a Testcase");
            }
        }
    }

    public Testcase(List<Integer> elements) {
        super(elements);
    }

    /**
     * used in ComFIL
     * @param comb
     * @param safeValues
     * @return
     */
    static public Testcase paddingWithSafeValues(Comb comb, Comb safeValues) {
        List<Integer> elements = new ArrayList<>(comb.elements);
        for (int i=0; i<elements.size(); i++) {
            if (elements.get(i) == Comb.UNKNOWN) {
                if (safeValues.elements.get(i) == Comb.UNKNOWN) { // randomly choose 0 or 1
                    elements.set(i, new Random().nextInt(0, 2));
                } else {
                    elements.set(i, safeValues.elements.get(i));
                }
            }
        }
        return new Testcase(elements);
    }

    /**
     * used in SOFOT, FIC, FICBS, NoPend
     * @param comb
     * @param safeValues
     * @param tfail
     * @return
     */
    static public Testcase paddingWithSafeValues(Comb comb, Comb safeValues, Testcase tfail) {
        List<Integer> elements = new ArrayList<>(comb.elements);
        for (int i=0; i<elements.size(); i++) {
            if (elements.get(i) == Comb.UNKNOWN) {
                if (safeValues.elements.get(i) == Comb.UNKNOWN) {
                    elements.set(i, 1 - tfail.elements.get(i));
                } else {
                    elements.set(i, safeValues.elements.get(i));
                }
            }
        }
        return new Testcase(elements);
    }

    /**
     * @param t: t-way
     * @return the t-way combination of this Testcase
     * (1,2,3,4), t = 2 -> {(1, 2, -, -), (1, -, 3, -), ...}
     */
    public List<Comb> tWayComb(int t) {
        List<Comb> res = new ArrayList<>(Combinatorics.n_choose_k(this.elements.size(), t));
        List<Integer> TestcaseIndex = new ArrayList<>();
        for (int i = 0; i < this.size(); i++) {
            TestcaseIndex.add(i);
        }
        Combination<Integer> combinationIndex = Combination.of(TestcaseIndex, t);
        for (List<Integer> combIndex : combinationIndex) {
            res.add(new Comb(this, combIndex));
        }

        return res;
    }

    /**
     *
     * @return the power set of this Testcase, but exclude emptySet
     */
    public List<Comb> powerSet() {
        List<Comb> res = new ArrayList<>();
        for (int i = 1; i <= this.size(); i++) {
            res.addAll(tWayComb(i));
        }
        return res;
    }

    static public Comb getOverlappingParts(Testcase a, Testcase b) {
        if (a.size() != b.size()) {
            throw new IllegalArgumentException("Testcases must have the same getLogicSize");
        }
        MutableComb result = new MutableComb(a.size());
        for (int i = 0; i < a.size(); i++) {
            if (Objects.equals(a.get(i), b.get(i))) {
                result.set(i, a.get(i));
            } else {
                result.set(i, Comb.UNKNOWN);
            }
        }
        return result;
    }
}
