package com.opk.fs.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Getter
@Setter
public class Buffer {

  private LDisk lDisk;

  private byte[] data = new byte[LDisk.getDISK_SIZE()];

  private int currentPositionInData;

  private int currentByteInFile;

  private boolean isReadMode;

  private int blockIndexInDisk;

  private FileDescriptor fileDescriptor;

  public void getNewBlock(byte[] block, FileDescriptor fileDescriptor, int blockIndexInDisk, boolean isReadMode){
    currentPositionInData = 0;
    this.blockIndexInDisk = blockIndexInDisk;
    this.isReadMode = isReadMode;
    this.fileDescriptor = fileDescriptor;
    data = Arrays.copyOf(block, LDisk.getDISK_SIZE());
  }
}
