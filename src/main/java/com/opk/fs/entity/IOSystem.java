package com.opk.fs.entity;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

public class IOSystem {

  public static byte[] readBlock(LDisk lDisk, int blockIndex) {
    return Arrays.copyOf(lDisk.getDisk()[blockIndex], LDisk.DISK_SIZE);
  }

  public static void writeBlock(LDisk lDisk, byte[] block, int blockIndex) {
    lDisk.getDisk()[blockIndex] = block;
  }

  public static void saveToTextFile(LDisk lDisk, String fileName) {
    try {
      FileOutputStream fileOutputStream = new FileOutputStream(fileName);
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
      objectOutputStream.writeObject(lDisk);
      objectOutputStream.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static LDisk getDiskFromTextFile(String fileName) {
    try {
      FileInputStream fileInputStream = new FileInputStream(fileName);
      ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
      LDisk lDisk = (LDisk) objectInputStream.readObject();
      objectInputStream.close();
      return lDisk;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
