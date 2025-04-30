package nju.gist.FaultResolver.ComFIL;

import nju.gist.Common.Comb;
import nju.gist.Common.Testcase;
import nju.gist.FaultResolver.AbstractFaultResolver;
import nju.gist.Tester.Checker;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ComFILFaultResolver extends AbstractFaultResolver {
    public ComFILFaultResolver(Checker<Testcase> checker, List<Testcase> Tpass, List<Testcase> Tfail) {
        super(checker, Tpass, Tfail);
    }

    @Override
    public void conduct() {
        if (conducted)
            return;
        conducted = true;
        if (safeValues == null) {
            throw new RuntimeException("Safe values not set");
        } else {
            conduct_with_safevalues();
        }
    }

    private void conduct_with_safevalues() {
        Set<Comb> candidateSet = new HashSet<Comb>();
        for (Testcase tfail : Tfail) {
            candidateSet.addAll(tfail.powerSet());
        }
        for (Testcase tpass : Tpass) {
            candidateSet.removeIf(candidate -> Comb.contains(tpass, candidate));
        }
        Set<Comb> copy = new HashSet<Comb>(candidateSet);
        for (Comb comb : copy) {
            if (!candidateSet.contains(comb)) {
                continue;
            } else {
                if (check_with_safevalues(comb)) { // comb is healthy, remove all its subsets
                    candidateSet.removeIf(candidate -> Comb.contains(comb, candidate));
                } else { // comb is faulty, remove all its supersets except for comb
                    candidateSet.removeIf(candidate -> Comb.contains(candidate, comb) && !candidate.equals(comb));
                }
            }
        }
        MFC_identified.addAll(candidateSet);
    }
}
