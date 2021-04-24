package com.opk.fs.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class FileDescriptor {
  private int fileLength;
  private List<Integer> fileBlocksIndexesInDisk;
}
