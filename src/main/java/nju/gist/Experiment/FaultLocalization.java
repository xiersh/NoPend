package nju.gist.Experiment;

import nju.gist.Common.Comb;
import nju.gist.Common.Testcase;
import nju.gist.FaultResolver.AbstractFaultResolver;
import nju.gist.FaultResolver.FaultResolverFactory;
import nju.gist.Tester.Checker;
import nju.gist.Util.CAResolver;
import nju.gist.Util.CSVResolver;
import nju.gist.Util.Measure;
import nju.gist.Util.Output;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.max;

public class FaultLocalization {
    private Checker<Testcase> checker;
    private List<Testcase> Tpass, Tfail;
    int n, t;
    private final List<Comb> MFC_real;
    private AbstractFaultResolver faultResolver;
    private final Comb safeValues;
    private boolean conducted = false;

    public FaultLocalization(String modelPath, String MFSPath, String safePath,
                             int max_mfs, Class<? extends AbstractFaultResolver> FaultResolverType) throws Exception {

        // initialize checker
        MFC_real = CSVResolver.readCombinations(MFSPath);
        checker = new Checker<Testcase>(MFC_real);
//         // Output.print(MFC_real, "MFC_real");

        // initialize n and t
        List<Map.Entry<String, Integer>> SUT = CSVResolver.readParameterValues(modelPath);
        n = SUT.size();
        t = max_mfs;

        // initialize Tpass and Tfail
        Tpass = new ArrayList<Testcase>();
        Tfail = new ArrayList<Testcase>();

        List<Testcase> testcases;
        if (t >= 2) { // using existing CA
            testcases = CAResolver.multiwayCA(n, t, 2);
        } else { // generate CA manually
            testcases = CAResolver.oneWayCA(n, 2);
        }

        for (Testcase testcase : testcases) {
            if (checker.check(testcase)) {
                Tpass.add(testcase);
            } else {
                Tfail.add(testcase);
            }
        }
        // // Output.print(Tpass, "Initial Tpass", System.out);
         // Output.print(Tfail, "Initial Tfail");
        checker.reset();
        // Tpass and Tfail should never change after

        // initialize safeValues
        safeValues = CSVResolver.readSafeValue(safePath);

        // initialize faultResolver
        faultResolver = FaultResolverFactory.createResolver(FaultResolverType, checker, Tpass, Tfail);
        faultResolver.setSafeValues(safeValues);
        faultResolver.setT(t);
    }

    public void reset() {
        conducted = false;
        faultResolver.reset();
    }

    public void conduct() throws OutOfMemoryError {
        if (conducted) {
            return;
        }
        faultResolver.conduct();
    }

    public int getNumTfail() {
        return Tfail.size();
    }

    // getData

    public List<Comb> getMFC_identified() {
        return new ArrayList<>(faultResolver.getMFC_identified());
    }

    public Set<Testcase> getAdditionalTestcase() {
        return checker.getExecutedTestcase();
    }

    public double getRecall() {
        return Measure.recall(MFC_real, new ArrayList<>(faultResolver.getMFC_identified()));
    }

    public double getPrecision() {
        return Measure.precision(MFC_real, new ArrayList<>(faultResolver.getMFC_identified()));
    }

    public double getF1() {
        return Measure.f1(MFC_real, new ArrayList<>(faultResolver.getMFC_identified()));
    }

    public double getPendingCombProportion() {
        double ans = 0.0;
        for (Testcase tfail : Tfail) {
            ans += faultResolver.getPendingCombProportion(tfail);
        }
        return ans / Tfail.size();
    }

    public void cleanup() {
        faultResolver = null;
        Tpass = null;
        Tfail = null;
        System.gc();
    }
}
