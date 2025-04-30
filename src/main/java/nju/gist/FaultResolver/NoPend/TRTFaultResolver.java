package nju.gist.FaultResolver.NoPend;

import com.sun.istack.Nullable;
import nju.gist.Common.*;
import nju.gist.FaultResolver.AbstractFaultResolver;
import nju.gist.Tester.Checker;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TRTFaultResolver extends AbstractFaultResolver {
    public TRTFaultResolver(Checker<Testcase> checker, List<Testcase> Tpass, List<Testcase> Tfail) {
        super(checker, Tpass, Tfail);

        // check the getLogicSize of test-case
        BigInteger trtSize = BigInteger.valueOf(2).pow(Tfail.getFirst().size());
        if (trtSize.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
            throw new UnsupportedOperationException("BitSet getLogicSize exceeds maximum supported getLogicSize");
        }
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
        for (Testcase tfail : Tfail) {
            // // Output.print("conduct_with_safevalues: " + tfail.toString());
            TRT trt = new TRT(tfail.size());
            trt.setTfail(tfail);
            while (true) {
                MonotoneSchemaSet topSchemas = trt.getTopSchemas();
                if (topSchemas.isEmpty()) { // no more pending schema
                    break;
                }
//                // // Output.print("topSchemas getLogicSize: " + topSchemas.getLogicSize());
//                // // Output.print(topSchemas.getSchemas(), tfail, System.out, "topSchemas");

                MonotoneSchemaSet bottomSchemas = trt.getBottomSchemas();
                if (bottomSchemas.isEmpty()) {
                    throw new RuntimeException("Bottom schemas should not be empty");
                }
//                // // Output.print("bottomSchemas getLogicSize: " + bottomSchemas.getLogicSize());
//                // // Output.print(bottomSchemas.getSchemas(), tfail, System.out, "bottomSchemas");

                Map.Entry<Schema, Schema> longestChain = NoPend.getLongestChain(topSchemas, bottomSchemas);
                if (longestChain == null) {
                    throw new RuntimeException("Longest chain should not be empty");
                }

                Schema max = longestChain.getKey();
                Schema min = longestChain.getValue();
                Schema minfault = binarySearch(max, min, tfail);
                if (minfault == null) {
                    trt.setHealthy(max);
                } else {
                    // // Output.print(minfault.getComb(tfail), "minfault", System.out);
                    trt.setFaulty(minfault);
                }
            }

            // all pending schemas are eliminated
            MonotoneSchemaSet MFS = trt.getMFS();
            // // Output.print(MFS.getSchemas(), tfail, "MFS", System.out);
            for (Schema mfs : MFS.getSchemas()) {
                MFC_identified.add(mfs.getComb(tfail));
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

        // prune
        if (check_with_safevalues(max.getComb(tfail), tfail)) {
            return null;
        }


        List<Schema> chain = new ArrayList<Schema>();
        chain.add(min);
        MutableSchema now = new MutableSchema(min);
        for (int i = 0; i < max.getLogicSize(); i++) {
            if (!min.get(i) && max.get(i)) {
                now.set(i, true);
                chain.add(new Schema(now));
            }
        }
        // // Output.print(chain, tfail, "longest chain", System.out);

        int left = 0, right = chain.size() - 1, mid, result = -1;
        while (left <= right) {
            mid = left + (right - left) / 2;
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
