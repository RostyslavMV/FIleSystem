package com.opk.fs.entity;

import lombok.Getter;

@Getter
public class Cache {

  private static final int OPEN_FILE_TABLE_SIZE = 5;

  private final Buffer[] openFileTable = new Buffer[OPEN_FILE_TABLE_SIZE];

  public int addBufferToOpenFileTable(Buffer buffer) {
    for (int i = 0; i < OPEN_FILE_TABLE_SIZE; i++) {
      if (openFileTable[i] == null) {
        openFileTable[i] = buffer;
        return i;
      }
    }
    System.out.println("No empty place in open file table");
    return -1;
  }
  public void deleteBufferFromOpenFileTable(int index) {
    openFileTable[index] = null;
  }
}
