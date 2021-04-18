package com.opk.fs.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Directory {

  private List<FileInfo> fileInfos = new ArrayList<>();

  public void addFileInfo(FileInfo fileInfo) {
    fileInfos.add(fileInfo);
  }
}
