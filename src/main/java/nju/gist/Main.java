package nju.gist;

import nju.gist.Experiment.FaultLocalization;
import nju.gist.Experiment.Result;
import nju.gist.FaultResolver.AbstractFaultResolver;
import nju.gist.FaultResolver.Ben.BenFaultResolver;
import nju.gist.FaultResolver.ComFIL.ComFILFaultResolver;
import nju.gist.FaultResolver.FINOVLP.FICBSFaultResolver;
import nju.gist.FaultResolver.FINOVLP.FICFaultResovler;
import nju.gist.FaultResolver.IterAIFL.IterAIFLFaultResolver;
import nju.gist.FaultResolver.NoPend.NoPendFaultResolver;
import nju.gist.FaultResolver.NoPend.TRTFaultResolver;
import nju.gist.FaultResolver.SOFOT.SOFOTFaultResolver;
import nju.gist.Util.Output;
import nju.gist.Util.PathResolver;

import java.util.HashMap;
import java.util.Map;

public class Main {
    private static final Map<String, Class<? extends AbstractFaultResolver>> METHOD_MAP = new HashMap<>();
    static {
        METHOD_MAP.put("SOFOT", SOFOTFaultResolver.class);
        METHOD_MAP.put("FIC", FICFaultResovler.class);
        METHOD_MAP.put("FICBS", FICBSFaultResolver.class);
        METHOD_MAP.put("NoPend", NoPendFaultResolver.class);
        METHOD_MAP.put("IterAIFL", IterAIFLFaultResolver.class);
        METHOD_MAP.put("ComFIL", ComFILFaultResolver.class);
        METHOD_MAP.put("TRT", TRTFaultResolver.class);
        METHOD_MAP.put("BEN", BenFaultResolver.class);
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java Main <SUT_path> <method_name> <max_mfs>");
            System.exit(1);
        }

        String SUT_path = args[0];
        String method_name = args[1];
        int maxMfs = Integer.parseInt(args[2]);
         // Output.print("SUT_path: " + SUT_path);
         // Output.print("method_name: " + method_name);
         // Output.print("max_mfs: " + maxMfs);

        String basename = PathResolver.getBase(SUT_path);
        String modelPath = basename + "-model.csv";
        String MFSPath = basename + "-mfs.csv";
        String safePath = basename + "-safe.csv";

        try {
            Result result = runExperiment(modelPath, MFSPath, safePath, maxMfs, method_name);
            System.out.println(resultToJson(result));
        } catch (Exception e) {
            System.err.println("Error running experiment: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (OutOfMemoryError e) {
            System.err.println("Out of Memory: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Result runExperiment(String modelPath, String MFSPath, String safePath,
                                        int max_mfs, String method) throws Exception, OutOfMemoryError {
        Class<? extends AbstractFaultResolver> methodClass = METHOD_MAP.get(method);
        if (methodClass == null) {
            throw new IllegalArgumentException("Unknown method: " + method);
        }

        FaultLocalization faultLocalization = new FaultLocalization(modelPath, MFSPath, safePath, max_mfs, methodClass);
        int numTfail = faultLocalization.getNumTfail();
        long startTime = System.nanoTime();
        faultLocalization.conduct();
        long endTime = System.nanoTime();

        Result result = new Result();
        result.setPrecision(faultLocalization.getPrecision());
        result.setRecall(faultLocalization.getRecall());
        result.setF1(faultLocalization.getF1());
        result.setNumAT(faultLocalization.getAdditionalTestcase().size());
        result.setNumAT_avg((double) faultLocalization.getAdditionalTestcase().size() / numTfail);
        result.setTime(endTime - startTime);
        try {
            result.setPendingSchemaProportion(faultLocalization.getPendingCombProportion());
        } catch (IllegalStateException e) {
            result.setPendingSchemaProportion(null);
        }

        return result;
    }

    private static String resultToJson(Result result) {
        String ans = String.format(
                "{\"precision\": %.5f, \"recall\": %.5f, \"f1\": %.5f, \"numAT\": %d, \"numAT_avg\": %.5f, \"time\": %d",
                result.getPrecision(), result.getRecall(), result.getF1(),
                result.getNumAT(), result.getNumAT_avg(), result.getTime()
        );
        if (result.getPendingSchemaProportion() != null) {
            ans += String.format(", \"pendingSchemaProportion\": %.5f", result.getPendingSchemaProportion());
        }
        ans += "}";
        return ans;
    }
}




