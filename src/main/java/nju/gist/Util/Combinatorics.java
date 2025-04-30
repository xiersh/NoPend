package nju.gist.Util;

import java.math.BigInteger;

public class Combinatorics {
    /**
     * return the value of (n \choose k)
     * @param n
     * @param k
     * @return
     */
    static public int n_choose_k(int n, int k) {
        if (k == 0 || k == n) {
            return 1;
        }

        if (k > n || k < 0) {
            throw new IllegalArgumentException("k is invalid!");
        }

        if (k > n/2) {
            k = n - k;
        }

        BigInteger fraction = BigInteger.ONE, denominator = BigInteger.ONE;
        for (int i = 1; i <= k; i++) {
            fraction = fraction.multiply(BigInteger.valueOf(n - i + 1));
            denominator = denominator.multiply(BigInteger.valueOf(i));
        }
        return fraction.divide(denominator).intValue();
    }
}
