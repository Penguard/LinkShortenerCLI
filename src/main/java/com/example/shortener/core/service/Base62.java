package com.example.shortener.core.service;

import java.math.BigInteger;

public final class Base62 {
    private static final char[] A = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final BigInteger BASE = BigInteger.valueOf(62);

    private Base62() {}

    public static String encode(byte[] bytes) {
        BigInteger n = new BigInteger(1, bytes);
        if (n.equals(BigInteger.ZERO)) return "0";
        StringBuilder sb = new StringBuilder();
        while (n.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] dr = n.divideAndRemainder(BASE);
            sb.append(A[dr[1].intValue()]);
            n = dr[0];
        }
        return sb.reverse().toString();
    }
}