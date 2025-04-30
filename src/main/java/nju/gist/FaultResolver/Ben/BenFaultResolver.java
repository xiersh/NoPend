package nju.gist.FaultResolver.Ben;

import lombok.Getter;
import nju.gist.Common.Comb;
import nju.gist.Common.MutableComb;
import nju.gist.Common.MutableTestcase;
import nju.gist.Common.Testcase;
import nju.gist.FaultResolver.AbstractFaultResolver;
import nju.gist.Tester.Checker;
import nju.gist.Util.Output;
import org.raistlic.common.permutation.Combination;

import java.util.*;

public class BenFaultResolver extends AbstractFaultResolver {
    private class Component {
        @Getter
        int parameter;
        @Getter
        int value;
        Component(int parameter, int value) {
            this.parameter = parameter;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Component component = (Component) o;
            return parameter == component.parameter && value == component.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(parameter, value);
        }
    }
    private Map<Component, Double> components_suspiciousness;
    public BenFaultResolver(Checker<Testcase> checker, List<Testcase> Tpass, List<Testcase> Tfail) {
        super(checker, Tpass, Tfail);
        components_suspiciousness = new HashMap<>();
    }

    @Override
    public void conduct() {
        if (conducted)
            return;
        conducted = true;

        Set<Comb> suspiciousCombs, suspiciousCombs_old = null;
        List<Comb> suspiciousCombs_list = null;
        // no need for safa value
        while (true) {
            suspiciousCombs = getAllSuspiciousCombinations();
            // // // Output.print(suspiciousCombs, System.out, "Suspicious combinations");
            if (suspiciousCombs.isEmpty()) { // identified nothing
                return;
            }

            if (suspiciousCombs_old != null && suspiciousCombs.size() == suspiciousCombs_old.size()) {
                // 3. derive MFC
//                // // Output.print(Tpass, System.out, "Final Tpass");
//                // // Output.print(Tfail, System.out, "Final Tfail");
//                // // Output.print("Final suspicious combinations: ");
//                for (Comb comb : suspiciousCombs_list) {
//                    // // Output.print(comb, System.out, String.format("%f", getRho_c(comb) + getRho_e(comb)));
//                }
                for (Comb comb : suspiciousCombs) {
                    for (int i = 1; i <= t; i++) {
                        MFC_identified.addAll(smallerCombs(comb, i));
                    }
                }
                return;
            }

            // 1. produce a ranking
            components_suspiciousness.clear();
            computeSuspiciousnessForAllComponents(suspiciousCombs); // update components_suspiciousness
//            components_suspiciousness.forEach((component, suspiciousness) ->
//                    // // Output.print(String.format("Component (%d:%d) has suspiciousness %f", component.parameter, component.value, suspiciousness), System.out)
//            );
            suspiciousCombs_list = rank(suspiciousCombs);

            // 2. generate new testcases
            List<Testcase> newTestcases = generateNewTestcasesForAllSuspiciousCombs(suspiciousCombs_list);
//            // // Output.print(newTestcases, System.out, "New Testcases");
            for (Testcase testcase : newTestcases) {
                if (checker.check(testcase)) { // passed
                    Tpass.add(testcase);
                } else {
                    Tfail.add(testcase);
                }
            }

            suspiciousCombs_old = suspiciousCombs;

        }

    }

    private Set<Comb> getAllSuspiciousCombinations(){
        Set<Comb> res = new HashSet<>();
        for (Testcase tfail : Tfail) {
            res.addAll(tfail.tWayComb(t));
        }
        // // // Output.print(res, System.out, "twaycombs in Tfail");
        res.removeIf(this::isCurrentlyHealthy);
        return res;
    }

    private boolean isCurrentlyHealthy(Comb comb) {
        for (Testcase tpass : Tpass) {
            if(Comb.contains(tpass, comb)){
                return true;
            }
        }
        return false;
    }

    private List<Comb> rank(Set<Comb> suspiciousCombs_set) {
        List<Comb> suspiciousCombs_list = new ArrayList<>(suspiciousCombs_set);
        suspiciousCombs_list.sort((comb1, comb2) ->
                Double.compare(getRho_c(comb1) + getRho_e(comb1), getRho_c(comb2) + getRho_e(comb2))
        );
        return suspiciousCombs_list;
    }


    private double getRho_c(Comb comb) { // combination suspiciousness
        double rho_c = 0;
        int num_components = 0;
        for (int i = 0; i < comb.size(); i++) {
            if (comb.get(i) != Comb.UNKNOWN) {
                num_components++;
                Double component_suspiciousness = components_suspiciousness.get(new Component(i, comb.get(i)));
                if (component_suspiciousness == null) {
                    // // // Output.print(String.format("Component (%d:%d) is not found", i, comb.get(i)), System.out);
                    throw new RuntimeException("Component not found");
                }
                rho_c += component_suspiciousness;
            }
        }
        rho_c /= num_components;
        return rho_c;
    }
    
    private double getRho_e(Comb comb) { // environment suspiciousness
        double rho_s = Double.MAX_VALUE;
        for (Testcase tfail : Tfail) {
            if (!Comb.contains(tfail, comb)) {
                continue;
            }
            // // // Output.print(tfail, System.out, "tfail");
            // // // Output.print(comb, System.out, "comb");
            // tfail contains comb
            double tmp = 0;
            int num_components = 0;
            for (int i = 0; i < tfail.size(); i++) {
                if (comb.get(i) == Comb.UNKNOWN) {  // the component that comb does not contain
                    num_components++;
                    Double component_suspiciousness = components_suspiciousness.get(new Component(i, tfail.get(i)));
                    if (component_suspiciousness == null) {
                        // // // Output.print(String.format("Component (%d:%d) is not found", i, tfail.get(i)), System.out);
                        throw new RuntimeException("Component not found");
                    }
                    tmp += component_suspiciousness;
                }
            }
            tmp /= num_components;
            rho_s = Math.min(rho_s, tmp);
        }
        return rho_s;
    }

    private List<Testcase> generateNewTestcasesForAllSuspiciousCombs(List<Comb> suspiciousCombs_ranked) {
        Set<Testcase> testcases = new HashSet<>();
        for (Comb suspiciousComb : suspiciousCombs_ranked) {
            Testcase newTestcase = generateNewTestcaseForOneSuspiciousComb(suspiciousComb);
            // // Output.print(newTestcase, "New Testcase for " + suspiciousComb.toString(), System.out);
            testcases.add(newTestcase);
        }
        return new ArrayList<>(testcases);
    }

    private Testcase generateNewTestcaseForOneSuspiciousComb(Comb suspiciousComb) {
        MutableComb newComb = new MutableComb(suspiciousComb);
        for (int i = 0; i < newComb.size(); i++) {
            if (newComb.get(i) == Comb.UNKNOWN) {
                // For two values system only
                Component component1 = new Component(i, 0);
                Component component2 = new Component(i, 1);
                if (components_suspiciousness.get(component1) ==  null) {
                    newComb.set(i, 1);
                } else if (components_suspiciousness.get(component2) ==  null) {
                    newComb.set(i, 0);
                } else {
                    if (components_suspiciousness.get(component1) > components_suspiciousness.get(component2)) {
                        newComb.set(i, 1);
                    } else {
                        newComb.set(i, 0);
                    }
                }
            }
        }

        MutableTestcase newTestcase = new MutableTestcase(newComb);
        if (Tpass.contains(newTestcase) || Tfail.contains(newTestcase)) { // has been tested before
            Random random = new Random();
            while (true) {
                int index = random.nextInt(suspiciousComb.size()); // randomly select a component
                if (suspiciousComb.get(index) == Comb.UNKNOWN) {
                    newTestcase.flip(index); // flip the component
                    if (!Tpass.contains(newTestcase) && !Tfail.contains(newTestcase)) { // has not been tested before
                        break;
                    }
                    // reset and select another component
                    newTestcase.flip(index);
                }
            }
        }

        return new Testcase(newTestcase);
    }


    private List<Comb> smallerCombs(Comb suspiciousComb, int d) {
        List<Comb> res = new ArrayList<>();
        List<Integer> combIndex = new ArrayList<>();
        for (int i = 0; i < suspiciousComb.size(); i++) {
            if(suspiciousComb.get(i) != Comb.UNKNOWN) {
                combIndex.add(i);
            }
        }
        Combination<Integer> combinationIndex = Combination.of(combIndex, d);
        for (List<Integer> smallerCombIndex : combinationIndex) {
            res.add(new Comb(suspiciousComb, smallerCombIndex));
        }
        return res;
    }

    private void computeSuspiciousnessForAllComponents(Set<Comb> suspiciousCombs) {
        for (Testcase testcase : Tfail) {
            for (int i = 0; i < testcase.size(); i++) {
                Component component = new Component(i, testcase.get(i));
                double suspiciousness = computeSuspiciousnessForOneComponent(component, suspiciousCombs);
                // // // Output.print(String.format("Component (%d:%d) is added", i, testcase.get(i)), System.out);
                components_suspiciousness.put(component, suspiciousness);
            }
        }

        for (Testcase testcase : Tpass) {
            for (int i = 0; i < testcase.size(); i++) {
                Component component = new Component(i, testcase.get(i));
                double suspiciousness = computeSuspiciousnessForOneComponent(component, suspiciousCombs);
                // // // Output.print(String.format("Component (%d:%d) is added", i, testcase.get(i)), System.out);
                components_suspiciousness.put(component, suspiciousness);
            }
        }
    }

    private double computeSuspiciousnessForOneComponent(Component component, Set<Comb> suspiciousCombs) {
        if (components_suspiciousness.get(component) !=  null) {
            return components_suspiciousness.get(component);
        }

        double numComponentInTfail = 0;
        for (Testcase testcase : Tfail) {
            if (testcase.get(component.parameter) == component.value) {
                numComponentInTfail += 1.0;
            }
        }

        double numComponentInTpass = 0;
        for (Testcase testcase : Tpass) {
            if (testcase.get(component.parameter) == component.value) {
                numComponentInTpass += 1.0;
            }
        }


        // compute u
        double u = numComponentInTfail / Tfail.size();

        // compute v
        double v = numComponentInTfail / (numComponentInTfail + numComponentInTpass);

        // compute w
        double w = 0;
        for (Comb suspiciousComb : suspiciousCombs) {
            if (suspiciousComb.get(component.parameter) == component.value) {
                w += 1.0;
            }
        }
        w /= suspiciousCombs.size();

        return (u + v + w) / 3.0;
    }
}
