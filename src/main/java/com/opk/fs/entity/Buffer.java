package com.opk.fs.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Getter
@Setter
public class Buffer {

  private byte[] data = new byte[LDisk.DISK_SIZE];

  private int currentPositionInData;

  private int currentByteInFile;

  private boolean isReadMode;

  private int blockIndexInDisk;

  private int descriptorIndex;

  private FileDescriptor fileDescriptor;

  public Buffer(FileDescriptor fileDescriptor, int descriptorIndex) {
    this.fileDescriptor = fileDescriptor;
    this.descriptorIndex = descriptorIndex;
  }

  public void getNewBlock(
      byte[] block, int blockIndexInDisk,int currentByteInFile, boolean isReadMode) {
    currentPositionInData = 0;
    this.blockIndexInDisk = blockIndexInDisk;
    this.currentByteInFile = currentByteInFile;
    this.isReadMode = isReadMode;
    data = block;
  }

}
