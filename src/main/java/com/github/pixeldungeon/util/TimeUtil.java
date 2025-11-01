package com.github.pixeldungeon.util;
public class TimeUtil{
  public static String formatMillis(long ms){
    long m = ms/60000; long s = (ms%60000)/1000; long msr = ms%1000;
    return String.format("%02d:%02d.%03d", m, s, msr);
  }
}