package nju.gist.FaultResolver.FINOVLP;

import nju.gist.Common.*;
import nju.gist.Tester.Checker;
import nju.gist.Util.Output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.max;

public class FICBSFaultResolver extends FINOVLPFaultResolver {
    public FICBSFaultResolver(Checker<Testcase> checker, List<Testcase> Tpass, List<Testcase> Tfail) {
        super(checker, Tpass, Tfail);
    }


    /**
     * Locate a fixed parameter in the MFS, and delete elements in freeParameters that are no need of further check
     * @param tfail
     * @param freeParameters: in ascending order, which would be modified
     * @return the index of the fixed parameter in `freeParameters`
     */
    @Override
    protected int locateOneFixedParameter(Testcase tfail, List<Integer> freeParameters) {
        // // Output.print("tfail:" + tfail);
        // // Output.print("freeParameters: " + freeParameters.toString());
        if (freeParameters.isEmpty()) {
            return -1;
        }


        int ans = -1;
        int left = 0, right = freeParameters.size() - 1;

        // implementation 1
        // f(k): the target >= k
        // find the maximal k in freeParameters
//        while (left <= right) {
//            int mid = left + (right - left) / 2; // left <= mid <= right
//            MutableTestcase t = new MutableTestcase(tfail);
//
//            for (int i = mid; i < freeParameters.getLogicSize(); i++) {
//                t.set(freeParameters.get(i), safeValues.get(freeParameters.get(i)));
//            }
//
//            if (checker.check(t)) { // f(mid) == true
//                ans = mid;
//                left = mid + 1;
//            } else { // f(mid) == false
//                right = mid - 1;
//            }
//        }

        // implementation 2
        // f(k): the target <= k
        // find the minimal k in freeParameters
        while (left <= right) {
            int mid = left + (right - left) / 2; // left <= mid <= right
            MutableComb comb = new MutableComb(tfail);

            for (int i = 0; i <= mid; i++) {
                comb.set(freeParameters.get(i), Comb.UNKNOWN);
            }

            if (check_with_safevalues(comb, tfail)) { // f(mid) == true
                ans = mid;
                right = mid - 1;
            } else { // f(mid) == false
                left = mid + 1;
            }
        }

        return ans;
    }
}
