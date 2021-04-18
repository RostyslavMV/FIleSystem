package com.opk.fs.entity;

public class Cache {

  private static final int OPEN_FILE_TABLE_SIZE = 3;

  private final Buffer[] openFileTable = new Buffer[OPEN_FILE_TABLE_SIZE];

  public void addBufferToOpenFileTable(Buffer buffer) {
    for (int i = 0; i < OPEN_FILE_TABLE_SIZE; i++) {
      if (openFileTable[i] == null) {
        openFileTable[i] = buffer;
        return;
      }
    }
    System.out.println("No empty place in open file table");
  }
}
