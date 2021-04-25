package com.opk.fs;

import com.opk.fs.entity.FileSystem;

public class Application {

  public static void main(String[] args) {
    FileSystem fileSystem = new FileSystem();
    fileSystem.createFile("ggwp");
    fileSystem.openFile("ggwp");
    String str = "Test";
    String str2 = "AnotherStr";
    fileSystem.writeFile(0, str.toCharArray(), 4);
    fileSystem.writeFile(0, str2.toCharArray(), 10);
    fileSystem.closeFile(0);
    fileSystem.openFile("ggwp");
    fileSystem.seek(0, 4);
    fileSystem.writeFile(0, str.toCharArray(), 4);
    fileSystem.seek(0, 0);
    fileSystem.readFile(0, 14);

    fileSystem.listDirectory();
  }
}
