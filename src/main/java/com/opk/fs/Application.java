package com.opk.fs;

import com.opk.fs.entity.FileSystem;

public class Application {

  public static void main(String[] args) {
    FileSystem fileSystem = new FileSystem();
    fileSystem.createFile("ggwp");
    fileSystem.createFile("gg");
    fileSystem.deleteFile("ggwp");
  }
}
