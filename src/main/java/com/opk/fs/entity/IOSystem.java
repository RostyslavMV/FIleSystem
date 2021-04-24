package com.opk.fs.entity;

import java.util.Arrays;

public class IOSystem {

  public static byte[] readBlock(LDisk lDisk, int blockIndex) {
    return Arrays.copyOf(lDisk.getDisk()[blockIndex], LDisk.DISK_SIZE);
  }

  public static void writeBlock(LDisk lDisk, byte[] block, int blockIndex) {
    lDisk.getDisk()[blockIndex] = block;
  }
}
