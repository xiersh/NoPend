package nju.gist.Experiment;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class Result {
    @Setter
    @Getter
    Long time;
    @Setter @Getter
    Integer numAT;
    @Setter @Getter
    Double precision, recall, f1, numAT_avg, pendingSchemaProportion;
    public Result() {
        time = null;
        numAT = null;
        numAT_avg = null;
        precision = null;
        recall = null;
        f1 = null;
        pendingSchemaProportion = null;
    }
}
