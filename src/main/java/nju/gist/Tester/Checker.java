package nju.gist.Tester;


import lombok.Getter;
import nju.gist.Common.Comb;
import nju.gist.Util.Output;


import java.util.*;

public class Checker<T extends Comb> {
    private final List<Comb> MFS;
    private final Set<T> executedTestcase;
    @Getter
    private int counter = 0;

    public Checker(List<Comb> MFS) {
        this.MFS = new ArrayList<>(MFS);
        executedTestcase = new HashSet<>();
    }

    public void reset() {
        counter = 0;
        executedTestcase.clear();
    }

    /**
     * @param testcase
     * @return true if testcase is faulty/failed, otherwise false
     * @throws Exception
     */
    public boolean check(T testcase) throws IllegalArgumentException {
        boolean res = true;
        counter += 1;
        executedTestcase.add(testcase);
        for (Comb mf : MFS) {
            if (Comb.contains(testcase, mf)) {
                res = false;
                break;
            }
        }
        return res;
    }

    public Set<T> getExecutedTestcase() {
        return Collections.unmodifiableSet(executedTestcase);
    }
}
