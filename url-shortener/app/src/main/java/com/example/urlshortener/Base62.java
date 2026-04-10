package com.example.urlshortener;

public final class Base62 {

  private static final char[] CHARS =
      "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
  private static final int BASE = CHARS.length;

  private Base62() {}

  public static String encode(long value) {
    if (value < 0) {
      throw new IllegalArgumentException("Base62 encode requires non-negative value");
    }
    if (value == 0) {
      return "0";
    }
    StringBuilder sb = new StringBuilder();
    while (value > 0) {
      sb.append(CHARS[(int) (value % BASE)]);
      value /= BASE;
    }
    return sb.reverse().toString();
  }

  public static long decode(String s) {
    long value = 0;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      int digit = indexOf(c);
      if (digit < 0) {
        throw new IllegalArgumentException("Invalid base62 character: " + c);
      }
      value = value * BASE + digit;
    }
    return value;
  }

  private static int indexOf(char c) {
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'a' && c <= 'z') return c - 'a' + 10;
    if (c >= 'A' && c <= 'Z') return c - 'A' + 36;
    return -1;
  }
}
