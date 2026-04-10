package com.example.scaling.lab2;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 개략적 규모 추정 (back-of-the-envelope) 계산기.
 * 책 2장의 사고 과정을 그대로 옮긴 단순 산식 + 치트시트.
 */
public class Estimator {

  private static final long SECONDS_PER_DAY = 86_400L;
  private static final double KB = 1024.0;
  private static final double MB = KB * 1024;
  private static final double GB = MB * 1024;
  private static final double TB = GB * 1024;

  public static void main(String[] args) {
    Map<String, String> opts = parseArgs(args);

    if (opts.containsKey("cheatsheet")) {
      printCheatsheet();
      return;
    }

    if (!opts.containsKey("dau") || !opts.containsKey("reqPerUser") || !opts.containsKey("payloadBytes")) {
      System.err.println("usage: --dau=N --reqPerUser=N --payloadBytes=N [--newItemsPerYear=N] [--peakFactor=N] [--cheatsheet]");
      System.exit(1);
    }

    long dau = Long.parseLong(opts.get("dau"));
    long reqPerUser = Long.parseLong(opts.get("reqPerUser"));
    long payloadBytes = Long.parseLong(opts.get("payloadBytes"));
    long newItemsPerYear = Long.parseLong(opts.getOrDefault("newItemsPerYear", "0"));
    double peakFactor = Double.parseDouble(opts.getOrDefault("peakFactor", "3"));

    long dailyRequests = dau * reqPerUser;
    double avgQps = dailyRequests / (double) SECONDS_PER_DAY;
    double peakQps = avgQps * peakFactor;
    double avgBwBps = avgQps * payloadBytes;
    double peakBwBps = peakQps * payloadBytes;
    double yearlyRawBytes = (double) newItemsPerYear * payloadBytes;
    double yearlyReplicatedBytes = yearlyRawBytes * 3;

    NumberFormat nf = NumberFormat.getInstance(Locale.US);

    System.out.println();
    System.out.println("== 입력 ==");
    System.out.printf("%-26s %s%n", "DAU", nf.format(dau));
    System.out.printf("%-26s %s%n", "사용자당 일 요청", nf.format(reqPerUser));
    System.out.printf("%-26s %s%n", "평균 페이로드 (B)", nf.format(payloadBytes));
    System.out.printf("%-26s %s%n", "1년 신규 레코드", nf.format(newItemsPerYear));
    System.out.printf("%-26s %s%n", "피크 배수", peakFactor);

    System.out.println();
    System.out.println("== 트래픽 ==");
    System.out.printf("%-26s %s%n", "일 요청 수", nf.format(dailyRequests));
    System.out.printf("%-26s ~%s%n", "평균 QPS", nf.format(Math.round(avgQps)));
    System.out.printf("%-26s ~%s%n", "피크 QPS", nf.format(Math.round(peakQps)));

    System.out.println();
    System.out.println("== 대역폭 ==");
    System.out.printf("%-26s ~%s%n", "평균", humanBytesPerSec(avgBwBps));
    System.out.printf("%-26s ~%s%n", "피크", humanBytesPerSec(peakBwBps));

    if (newItemsPerYear > 0) {
      System.out.println();
      System.out.println("== 저장 (1년) ==");
      System.out.printf("%-26s ~%s%n", "원시 데이터", humanBytes(yearlyRawBytes));
      System.out.printf("%-26s ~%s%n", "복제 3x", humanBytes(yearlyReplicatedBytes));
    }

    System.out.println();
    printCheatsheet();
  }

  private static String humanBytes(double b) {
    if (b >= TB) return String.format("%.2f TB", b / TB);
    if (b >= GB) return String.format("%.2f GB", b / GB);
    if (b >= MB) return String.format("%.2f MB", b / MB);
    if (b >= KB) return String.format("%.2f KB", b / KB);
    return String.format("%.0f B", b);
  }

  private static String humanBytesPerSec(double b) {
    return humanBytes(b) + "/s";
  }

  private static void printCheatsheet() {
    System.out.println("== 치트시트 ==");
    System.out.println("[2의 제곱수]");
    System.out.println("  2^10 ≈ 1 K     (1 KB)");
    System.out.println("  2^20 ≈ 1 M     (1 MB)");
    System.out.println("  2^30 ≈ 1 B     (1 GB)");
    System.out.println("  2^40 ≈ 1 T     (1 TB)");
    System.out.println("  2^50 ≈ 1 Pb    (1 PB)");
    System.out.println();
    System.out.println("[지연시간 (Jeff Dean)]");
    System.out.println("  L1 캐시 참조             0.5 ns");
    System.out.println("  분기 예측 실패           5 ns");
    System.out.println("  L2 캐시 참조             7 ns");
    System.out.println("  뮤텍스 락/언락           25 ns");
    System.out.println("  메인 메모리 참조         100 ns");
    System.out.println("  Zippy 1KB 압축           10 us");
    System.out.println("  1Gbps 망 2KB 송신        20 us");
    System.out.println("  SSD 랜덤 읽기            150 us");
    System.out.println("  메모리 1MB 순차 읽기     250 us");
    System.out.println("  같은 DC RTT              500 us");
    System.out.println("  SSD 1MB 순차 읽기        1 ms");
    System.out.println("  디스크 탐색              10 ms");
    System.out.println("  HDD 1MB 순차 읽기        20 ms");
    System.out.println("  CA → 네덜란드 RTT        150 ms");
    System.out.println();
    System.out.println("[가용성]");
    System.out.println("  99      %  → 3.65 일/년");
    System.out.println("  99.9    %  → 8.76 시간/년");
    System.out.println("  99.99   %  → 52.56 분/년");
    System.out.println("  99.999  %  → 5.26 분/년");
    System.out.println("  99.9999 %  → 31.56 초/년");
  }

  private static Map<String, String> parseArgs(String[] args) {
    Map<String, String> out = new HashMap<>();
    for (String a : args) {
      if (!a.startsWith("--")) continue;
      String stripped = a.substring(2);
      int eq = stripped.indexOf('=');
      if (eq < 0) {
        out.put(stripped, "true");
      } else {
        out.put(stripped.substring(0, eq), stripped.substring(eq + 1));
      }
    }
    return out;
  }
}
