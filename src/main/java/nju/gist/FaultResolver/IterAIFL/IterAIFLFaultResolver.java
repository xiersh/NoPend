package nju.gist.FaultResolver.IterAIFL;

import nju.gist.Common.Comb;
import nju.gist.Common.Testcase;
import nju.gist.FaultResolver.AbstractFaultResolver;
import nju.gist.Tester.Checker;
import nju.gist.Util.Output;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IterAIFLFaultResolver extends AbstractFaultResolver {
    public IterAIFLFaultResolver(Checker<Testcase> checker, List<Testcase> Tpass, List<Testcase> Tfail) {
        super(checker, Tpass, Tfail);
    }

    @Override
    public void conduct() {
        if (conducted)
            return;
        conducted = true;

        // IterAIFL does not require safe values
        Set<Comb> suspiciousSet = new HashSet<Comb>();
//        // // Output.print(Tfail, System.out, "Tfail");
        for (Testcase tfail : Tfail) {
            suspiciousSet.addAll(tfail.powerSet());
        }
//        // // Output.print(new ArrayList<>(suspiciousSet), System.out, "suspiciousSet");
        for (Testcase tpass : Tpass) {
            suspiciousSet.removeIf(candidate -> Comb.contains(tpass, candidate));
        }
        // // Output.print("suspiciousSet-0 getLogicSize: " + suspiciousSet.getLogicSize());
//        // // Output.print(new ArrayList<>(suspiciousSet), System.out, "suspiciousSet-0");
        for (int i = 1; i < Tfail.getFirst().size(); i++) {
            List<Testcase> AT = IterAIFL.getAT(i, Tfail); // get AT_i
            for (Testcase testcase : AT) {
                if (checker.check(testcase)) { // testcase is passed
//                    // // Output.print(testcase, System.out, "passed testcase");
                    suspiciousSet.removeIf(candidate -> Comb.contains(testcase, candidate));
                }
            }
            if (suspiciousSet.isEmpty()) {
                throw new RuntimeException("Implementation is wrong");
            }
            // // Output.print("suspiciousSet-%d getLogicSize: %d".formatted(i, suspiciousSet.getLogicSize()));
//            // // Output.print(new ArrayList<>(suspiciousSet), System.out, "suspiciousSet-%d".formatted(i));
        }
//        // // Output.print(new ArrayList<>(suspiciousSet), System.out, "suspiciousSet");
        MFC_identified.addAll(IterAIFL.getMinimal(suspiciousSet));
    }
}
