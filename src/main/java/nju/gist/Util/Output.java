package nju.gist.Util;

import nju.gist.Common.Comb;
import nju.gist.Common.Schema;
import nju.gist.Common.Testcase;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;

public class Output {
    private final static boolean DEBUG = false;
    private static PrintStream my_out;

    static {
        my_out = System.out;
//        try {
//            my_out = new PrintStream("output.log");
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        }
    }

    public static void print(String message, PrintStream out) {
        if (!DEBUG) return;
        out.println(message);
    }

    public static void print(String message) {
        print(message, my_out);
    }

    public static <T extends Comb> void print(T comb, String name, PrintStream out) {
        if (!DEBUG) return;
        out.printf("%s: %s \n", name, comb.toString());
    }

    public static <T extends Comb> void print(T comb, String name) {
        print(comb, name, my_out);
    }

    public static <T extends Comb> void print(T comb, PrintStream out) {
        print(comb, (comb instanceof Testcase)? "Testcase" : "Comb", out);
    }

    public static <T extends Comb> void print(List<T> combs, String name, PrintStream out) {
        if (!DEBUG) return;
        if (combs.isEmpty()) {
            out.println(name + " is empty");
            return;
        }

        if (combs.getFirst() instanceof Testcase) {
            out.printf("%s: %d Testcases \n", name, combs.size());
        } else {
            out.printf("%s: %d Combs \n", name, combs.size());
        }

        int cnt = 1;
        for (Comb comb : combs) {
            out.println(cnt + ": " + comb.toString());
            cnt++;
        }
    }

    public static <T extends Comb> void print(List<T> combs, PrintStream out) {
        print(combs, "Combs/Testcases", out);
    }

    public static <T extends Comb> void print(List<T> combs, String name) {
        print(combs, name, my_out);
    }

    public static <T extends Comb> void print(List<T> combs) {
        print(combs, my_out);
    }

    public static void print(List<Schema> schemas, Testcase testcase, String name, PrintStream out) {
        if (!DEBUG) return;
        out.printf("%s: %d schemas \n", name, schemas.size());
        int cnt = 1;
        for (Schema schema : schemas) {
            out.println(cnt + ":" + schema.getComb(testcase));
            cnt++;
        }
    }

    public static void print(List<Schema> schemas, Testcase testcase) {
        print(schemas, testcase, "Schemas", my_out);
    }

    public static void print(List<Schema> schemas, Testcase testcase, PrintStream out) {
        print(schemas, testcase, "Schemas", out);
    }

    public static void print(List<Schema> schemas, Testcase testcase, String name) {
        print(schemas, testcase, name, my_out);
    }
}
