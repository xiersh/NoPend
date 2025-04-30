package nju.gist.FaultResolver;

import lombok.Getter;
import lombok.Setter;
import nju.gist.Common.*;
import nju.gist.Tester.Checker;
import nju.gist.Util.Output;

import java.util.*;

public class AbstractFaultResolver {
    protected final List<Testcase> Tpass, Tfail;
    protected final Checker<Testcase> checker;
    protected final List<Comb> healthyCombs, faultyCombs;
    @Setter
    protected Comb safeValues;
    @Getter
    protected Set<Comb> MFC_identified; // Minimal Faulty Combinations
    protected boolean conducted = false;

    // maximal getLogicSize of MFS
    // required by BenFaultResolver
    @Setter
    protected int t = 0;

    public AbstractFaultResolver(Checker<Testcase> checker, List<Testcase> Tpass, List<Testcase> Tfail) {
        if (Tfail.isEmpty()) {
            throw new IllegalArgumentException("Tfail is empty");
        }

        this.checker = checker;
        this.Tpass = new ArrayList<>(Tpass); this.Tfail = new ArrayList<>(Tfail);
        MFC_identified = new HashSet<>();

        // used in SOFOT, FIC, FICBS, ComFIL, NoPend
        healthyCombs = new ArrayList<>(); faultyCombs = new ArrayList<>();
    }

    public void reset() {
        conducted = false;
        checker.reset();
        MFC_identified.clear();
    }

    public void conduct(){
        if (conducted)
            return;
        conducted = true;
        // Abstract method to be implemented by subclasses
    }

    /**
     * Used in ComFIL, since ComFIL does not locate MFS in one MFS
     * @param comb
     * @return
     */
    protected boolean check_with_safevalues(Comb comb) {
        // // Output.print(comb, "Check with safevalues");
        boolean result = checker.check(Testcase.paddingWithSafeValues(comb, safeValues));
        if (result) {
            healthyCombs.add(new Comb(comb));
        } else {
            faultyCombs.add(new Comb(comb));
        }
        return result;
    }

    protected boolean check_with_safevalues(Comb comb, Testcase tfail) {
//         // Output.print(comb, "Check with safevalues");
        Testcase t = Testcase.paddingWithSafeValues(comb, safeValues, tfail);
        boolean result = checker.check(t);
        if (result) {
            healthyCombs.add(new Comb(comb));
        } else {
            faultyCombs.add(new Comb(comb));
        }
        return result;
    }

    protected boolean checkPending(Comb comb) {
        for (Comb healthyComb : healthyCombs) {
            if (Comb.contains(healthyComb, comb)) {
                // // Output.print(healthyComb, "contained by a healthy comb", System.out);
                return false;
            }
        }
        for (Comb faultyComb : faultyCombs) {
            if (Comb.contains(comb, faultyComb)) {
                // // Output.print(faultyComb, "contain a faulty comb", System.out);
                return false;
            }
        }
        return true;
    }

    /**
     * Use Monte Carlo Method to estimate the proportion of pending comb
     * @param tfail
     * @return
     */
    public double getPendingCombProportion(Testcase tfail) throws IllegalStateException {
        if (healthyCombs.isEmpty() && faultyCombs.isEmpty()) {
            throw new IllegalStateException("This fault resolver does not use Assumption 1");
        }
        // // Output.print(String.format("healthyCombs getLogicSize: %d, faultyCombs getLogicSize: %d", healthyCombs.getLogicSize(), faultyCombs.getLogicSize()));
        int sampling_count = 1000, pending_count = 0;
        for (int i = 0; i < sampling_count; i++) {
            Schema schema = sampling(tfail.size());
            Comb comb = schema.getComb(tfail);
            if (checkPending(comb)) {
                // // Output.print(comb, "pending", System.out);
                pending_count++;
            }
        }
        return pending_count / ((double) sampling_count);
    }

    /**
     * uniformly generate a schema with n bits
     * @param n
     * @return a sampled schema
     */
    private Schema sampling(int n) {
        MutableSchema schema = new MutableSchema(n);
        Random r = new Random();
        for (int i = 0; i < n; i++) {
            if (r.nextBoolean()) {
                schema.set(i, true);
            }
        }
        return schema;
    }
}
