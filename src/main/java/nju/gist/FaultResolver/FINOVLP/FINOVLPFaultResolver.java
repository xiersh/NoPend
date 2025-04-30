package nju.gist.FaultResolver.FINOVLP;

import nju.gist.Common.*;
import nju.gist.FaultResolver.AbstractFaultResolver;
import nju.gist.Tester.Checker;
import nju.gist.Util.Output;
import org.apache.poi.ss.formula.eval.NotImplementedException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FINOVLPFaultResolver extends AbstractFaultResolver {
    public FINOVLPFaultResolver(Checker<Testcase> checker, List<Testcase> Tpass, List<Testcase> Tfail) {
        super(checker, Tpass, Tfail);
    }

    @Override
    public void conduct()  {
        if (conducted)
            return;
        conducted = true;
        if (safeValues == null) {
            throw new RuntimeException("Safe values not set");
        } else {
            conduct_with_safevalues();
        }
    }

    protected void conduct_with_safevalues()  {
        for (Testcase tfail : Tfail) {
            // // Output.print("Now locating MFC in " + tfail);
            MFC_identified.addAll(FINOVLP(tfail));
        }
    }

    protected Set<Comb> FINOVLP(Testcase _tfail) {
        MutableTestcase tfail = new MutableTestcase(_tfail);
        Set<Comb> MFSSet = new HashSet<>();
        MutableSchema fixedParameters = new MutableSchema(tfail.size());
        while (!checker.check(tfail)) { // while there are MFS left
            Comb MFS = locateOneMFC(tfail, fixedParameters);
            MFSSet.add(MFS);
            for (int i=0; i<MFS.size(); i++) {
                if (MFS.get(i) != Comb.UNKNOWN) {
                    fixedParameters.set(i, true);
                    paddingWithSafeValues(tfail, i);
                }
            }
        }
        // // Output.print(new ArrayList<Comb>(MFSSet), "Identified MFC in " + _tfail, System.out);
        return MFSSet;
    }


    protected Comb locateOneMFC(Testcase tfail, Schema fixedParameter) {
        // // Output.print("tfail:" + tfail);
        // // Output.print("fixedParameter: " + fixedParameter.toString());

        List<Integer> freeParameters = fixedParameter.complement().getIndices(); // in ascending order
        List<Integer> locatedFaultyBits = new ArrayList<Integer>();
        MutableTestcase t = new MutableTestcase(tfail);
        while (true) { //
            int fixedParameterIndex = locateOneFixedParameter(t, freeParameters);
            if (fixedParameterIndex == -1) { // cannot find any parameter leading to failure
                break;
            } else {
                locatedFaultyBits.add(freeParameters.get(fixedParameterIndex));
                freeParameters.remove(fixedParameterIndex);
            }
        }
        return (new Schema(tfail.size(), locatedFaultyBits)).getComb(tfail);
    }

    protected int locateOneFixedParameter(Testcase tfail, List<Integer> freeParameters) {
        throw new NotImplementedException("FINOVLPFaultResolver is abstract");
    }

    protected void paddingWithSafeValues(MutableTestcase t, int index) {
        if (safeValues.get(index) == Comb.UNKNOWN) {
            t.flip(index);
        } else {
            t.set(index, safeValues.get(index));
        }
    }

}
