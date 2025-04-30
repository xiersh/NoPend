package nju.gist.Util;

import nju.gist.Common.Comb;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Measure {

    public static double recall(List<Comb> MFC_real, List<Comb> MFC_identified) {
        if (MFC_real.isEmpty()) {
            return 1.0;
        }

        Set<Comb> MFC_set = new HashSet<Comb>(MFC_real);
        int cnt = 0;
        for (Comb comb : MFC_identified) {
            if (MFC_set.contains(comb)) {
                cnt++;
            }
        }
        return cnt / (double) MFC_set.size();
    }

    public static double precision(List<Comb> MFC_real, List<Comb> MFC_identified) {
        if (MFC_identified.isEmpty()) {
            return 1.0;
        }
        Set<Comb> MFC_set = new HashSet<Comb>(MFC_identified);
        int cnt = 0;
        for (Comb comb : MFC_real) {
            if (MFC_set.contains(comb)) {
                cnt++;
            }
        }
        return cnt / (double) MFC_set.size();
    }

    public static double f1(List<Comb> MFC_real, List<Comb> MFC_identified) {
        double recall = recall(MFC_real, MFC_identified);
        double precision = precision(MFC_real, MFC_identified);
        return 2 * recall * precision / (recall + precision);
    }
}
