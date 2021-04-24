package com.opk.fs;

public class Application {

  public static void main(String[] args) {
    byte b = -128;
    char c = (char) b;
    String str = "string";
    byte[] bytes = str.getBytes();
    String newStr = new String(bytes);
    System.out.println(c);
  }
}
