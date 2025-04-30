package nju.gist.FaultResolver.IterAIFL;

import nju.gist.Common.Comb;
import nju.gist.Common.MutableTestcase;
import nju.gist.Common.Testcase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IterAIFL {

    static public List<Testcase> getAT(int strength, List<Testcase> Tfail) {
        List<Testcase> res = new ArrayList<>();
        for (Testcase tfail : Tfail) {
            res.addAll(mutate(strength, tfail));
        }
        return res;
    }

    static public List<Testcase> mutate(int strength, Testcase tfail) {
        List<Testcase> res = new ArrayList<>();
        List<Comb> twaySets = tfail.tWayComb(strength);
        for (Comb tway : twaySets) {
            MutableTestcase t = new MutableTestcase(tfail);
            for (int i=0; i<t.size(); i++) {
                if (tway.get(i) != Comb.UNKNOWN) { // modify this parameter
                    t.flip(i); // flip the value of i
                }
            }
            res.add(new Testcase(t));
        }
        return res;
    }

    static public Set<Comb> getMinimal(Set<Comb> faultyComb) { // get minimal combs in faultyComb
        Set<Comb> minimalSet = new HashSet<>(faultyComb);
        for (Comb comb : faultyComb) {
            if (!minimalSet.contains(comb)) { // already deleted
                continue;
            } else { // delete all proper superset of comb
                minimalSet.removeIf(element -> Comb.contains(element, comb) && !element.equals(comb));
            }
        }
        return minimalSet;
    }
}
