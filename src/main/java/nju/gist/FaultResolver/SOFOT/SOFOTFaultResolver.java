package nju.gist.FaultResolver.SOFOT;

import nju.gist.Common.*;
import nju.gist.FaultResolver.AbstractFaultResolver;
import nju.gist.Tester.Checker;

import java.util.ArrayList;
import java.util.List;

public class SOFOTFaultResolver extends AbstractFaultResolver {
    public SOFOTFaultResolver(Checker<Testcase> checker, List<Testcase> Tpass, List<Testcase> Tfail) {
        super(checker, Tpass, Tfail);
    }
    @Override
    public void conduct() throws Error {
        if (conducted)
            return;
        conducted = true;

        if (safeValues == null) {
            conduct_without_safevalues();
        } else {
            conduct_with_safevalues();
        }

    }

    protected void conduct_with_safevalues() {
        for (Testcase tfail : Tfail) {
            MutableSchema MFS = new MutableSchema(tfail.size());
            MutableComb t = new MutableComb(tfail);
            for (int i = 0; i < tfail.size(); i++) {
                int old_value = tfail.get(i);
                t.set(i, Comb.UNKNOWN); // t is the combination tested
                if(check_with_safevalues(t, tfail)) { // `i` is related to faulty interaction
                    MFS.set(i, true);
                }
                t.set(i, old_value); // reset
            }
            if(MFS.size() == 0) { // didn't identify any MFS
                continue;
            }
            Comb MFC = MFS.getComb(tfail);
            MFC_identified.add(MFC);
        }
    }

    protected void conduct_without_safevalues() {
        for (Testcase _tfail : Tfail) {
            MutableTestcase tfail = new MutableTestcase(_tfail);
            List<Integer> MFS = new ArrayList<Integer>();
            for (int i = 0; i < tfail.size(); i++) {
                tfail.flip(i);
                if(checker.check(tfail)) { // `i` is related to faulty interaction
                    MFS.add(i);
                }
                tfail.flip(i); // reset
            }
            if(MFS.isEmpty()) {
                continue;
            }
            Comb MFC = (new Schema(_tfail.size(), MFS)).getComb(_tfail);
            MFC_identified.add(MFC);
        }
    }
}


