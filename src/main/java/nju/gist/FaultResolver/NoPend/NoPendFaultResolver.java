package nju.gist.FaultResolver.NoPend;

import com.sun.istack.Nullable;
import nju.gist.Common.*;
import nju.gist.FaultResolver.AbstractFaultResolver;
import nju.gist.Tester.Checker;
import nju.gist.Util.Output;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NoPendFaultResolver extends AbstractFaultResolver {
    public NoPendFaultResolver(Checker<Testcase> checker, List<Testcase> Tpass, List<Testcase> Tfail) {
        super(checker, Tpass, Tfail);
    }

    @Override
    public void conduct() {
        if (conducted)
            return;
        conducted = true;

        if (safeValues == null) {
            throw new RuntimeException("Safe values are not set");
        } else {
            conduct_with_safevalues();
        }
    }

    protected void conduct_with_safevalues() {
        for (Testcase tfail : Tfail) {
            // // Output.print("conduct_with_safevalues: " + tfail.toString());
            MonotoneSchemaSet CMinF = new MonotoneSchemaSet(true); // minimal faulty schemas
            MonotoneSchemaSet CMaxP = new MonotoneSchemaSet(false); // maximal healthy schemas
            CMinF.add(new Schema(tfail.size(), true)); // the failed test-case
            CMaxP.add(new Schema(tfail.size())); // the empty schema

            // initialize CMinF
            for (Comb MFS : MFC_identified) {
                if (Comb.contains(tfail, MFS)) {
                    CMinF.add(new Schema(MFS));
                    break;
                }
            }

            /*
             * initialize CMaxP
             * CMaxP = {overlapping parts of Tpass}
             * We do not do initialization of CMaxP is because of the costly time complexity
             */
//            for (Testcase tpass : Tpass) {
//                Comb overlappingComb = Testcase.getOverlappingParts(tfail, tpass);
//                CMaxP.add(new Schema(overlappingComb));
//            }

            identify(new Schema(tfail.size(), true), CMinF, CMaxP, tfail);

            // // Output.print(CMinF.getSchemas(), tfail, String.format("Identified MFC in %s", tfail.toString()), System.out);
            for (Schema MFS : CMinF.getSchemas()) {
                MFC_identified.add(MFS.getComb(tfail));
            }
        }

    }

    private void identify(Schema universe, MonotoneSchemaSet CMinF, MonotoneSchemaSet CMaxP, Testcase tfail) {
//        // Output.print(">>>>>>>>>>>>>>>>>>>>>>");
        // // Output.print("______\nuniverse: " + universe.getComb(tfail));
        // // Output.print(CMinF.getSchemas(), tfail, "CMinF", System.out);
        // // Output.print(CMaxP.getSchemas(), tfail, "CMaxP", System.out);
        // Output.print("universe.size: " + universe.size());

        if (universe.size() == 1) {
//             // Output.print("<<<<<<<<<<<<<<<<<<<<<<<<<");
            return;
        }
        while (true) {
            MonotoneSchemaSet topSchemas, bottomSchemas;
            bottomSchemas = MonotoneSchemaSet.getMHS(CMaxP.complement(universe));
            topSchemas = (MonotoneSchemaSet.getMHS(CMinF)).complement(universe);

            // print topSchemas and bottomSchemas
            // // Output.print(topSchemas.getSchemas(), tfail, "topSchemas", System.out);
            // // Output.print(bottomSchemas.getSchemas(), tfail, "bottomSchemas", System.out);

            Map.Entry<Schema, Schema> longestChain = NoPend.getLongestChain(topSchemas, bottomSchemas);
            if (longestChain == null) { // all pending schemas are eliminated
//                // Output.print("<<<<<<<<<<<<<<<<<<<<<<<<<");
                return;
            }

            Schema max = longestChain.getKey();
            Schema min = longestChain.getValue();
            // Output.print(String.format("max.size = %d, min.size = %d", max.size(), min.size()));
            // Output.print(max.getComb(tfail), "max");
            // Output.print(min.getComb(tfail), "min");
            Schema minfault = binarySearch(max, min, tfail);
            // // Output.print("minfault: " + (minfault == null? "null" : minfault.getComb(tfail)));
            if (minfault == null) { // all pending schemas are eliminated
                // update CMaxP
                CMaxP.add(new Schema(max));
            } else {
//                // Output.print(String.format("minfault.size = %d", minfault.size()));
                Schema universe_rec = new Schema(minfault);
                MonotoneSchemaSet CMinF_rec = new MonotoneSchemaSet(true);
                MonotoneSchemaSet CMaxP_rec = new MonotoneSchemaSet(false);
                CMinF_rec.add(universe_rec);
                CMaxP_rec.add(new Schema(universe_rec.getLogicSize()));

                // recursively called identify
                identify(universe_rec, CMinF_rec, CMaxP_rec, tfail);

                // update CMinF and CMaxP
                for (Schema cminf : CMinF_rec.getSchemas()) {
                    CMinF.add(cminf);
                }
                for (Schema cmaxp : CMaxP_rec.getSchemas()) {
                    CMaxP.add(cmaxp);
                }
                // // Output.print("Update: ");
                // // Output.print(CMinF.getSchemas(), tfail, "CMinF", System.out);
                // // Output.print(CMaxP.getSchemas(), tfail, "CMaxP", System.out);
            }
        }


    }

    @Nullable
    private Schema binarySearch(Schema max, Schema min, Testcase tfail) {
        if (max.getLogicSize() != min.getLogicSize()) {
            throw new RuntimeException("Schema sizes mismatch");
        }
        if (!max.isSupersetOf(min)) {
            throw new IllegalArgumentException("max is not a superset of min");
        }

        // prune this branch if max is healthy
        if (check_with_safevalues(max.getComb(tfail), tfail)) { // max is healthy
            return null;
        }

        List<Schema> chain = new ArrayList<Schema>(max.size() - min.size() + 1);
        chain.add(min);
        MutableSchema now = new MutableSchema(min);

        // version 1
//        for (int i = 0; i < max.getLogicSize(); i++) {
//            if (!min.get(i) && max.get(i)) {
//                now.set(i, true);
//                chain.add(new Schema(now));
//            }
//        }

        // version 2
        for (int i = max.getLogicSize() - 1; i >= 0; i--) {
            if (!min.get(i) && max.get(i)) {
                now.set(i, true);
                chain.add(new Schema(now));
            }
        }


        // // Output.print(chain, tfail, "longest chain", System.out);

        int left = 0, right = chain.size() - 1, mid, result = -1;
        while (left <= right) {
            mid = left + (right - left) / 2;
//            // Output.print(String.format("mid.size: %d", chain.get(mid).size()));
//            // Output.print(chain.get(mid).getComb(tfail), "mid");
            if (check_with_safevalues(chain.get(mid).getComb(tfail), tfail)) { // healthy
                left = mid + 1;
            } else { // faulty
                result = mid;
                right = mid - 1;
            }
        }

        if (result == -1) {
            return null;
        } else {
            return chain.get(result);
        }
    }
}
