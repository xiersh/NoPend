package nju.gist.FaultResolver.FINOVLP;

import nju.gist.Common.*;
import nju.gist.Tester.Checker;

import java.util.List;

public class FICFaultResovler extends FINOVLPFaultResolver{
    FICFaultResovler(Checker<Testcase> checker, List<Testcase> Tpass, List<Testcase> Tfail){
        super(checker, Tpass, Tfail);
    }

    @Override
    protected Comb locateOneMFC(Testcase tfail, Schema fixedParameter) {
        // comb is flipped during the locating process
        MutableComb comb = new MutableComb(tfail);
        MutableSchema MFS = new MutableSchema(tfail.size());
        for (int i=0; i<tfail.size(); i++) {
            if (!fixedParameter.get(i)) { // this parameter is not fixed
                comb.set(i, Comb.UNKNOWN);
                if (check_with_safevalues(comb, tfail)) { // i is involved in a MFS
                    MFS.set(i, true);
                    comb.set(i, tfail.get(i));
                }
            }
        }
        return MFS.getComb(tfail);
    }

}
