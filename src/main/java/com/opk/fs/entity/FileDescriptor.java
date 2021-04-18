package com.opk.fs.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class FileDescriptor {
  public int fileLength;
  public int[] fileBlocksIndexesInDisk;
}
