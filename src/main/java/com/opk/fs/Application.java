package com.opk.fs;

import com.opk.fs.entity.FileSystem;

public class Application {

  public static void main(String[] args) {
    FileSystem fileSystem = new FileSystem();
    fileSystem.createFile("ggwp");
    fileSystem.openFile("ggwp");
    String str = "TestTestTestTestTestTestTestTestTestTestTestTestTestTestTestTestTest";
    String str2 = "AnotherStr";
    fileSystem.writeFile(0, str.toCharArray(), str.length());
    fileSystem.writeFile(0, str2.toCharArray(), 10);
    fileSystem.closeFile(0);
    fileSystem.openFile("ggwp");
    fileSystem.seek(0, 63);
    fileSystem.writeFile(0, str.toCharArray(), 4);
    fileSystem.seek(0, 63);
    fileSystem.readFile(0, 14);
    fileSystem.createFile("gg");
    fileSystem.createFile("f1");
    fileSystem.openFile("f1");
    fileSystem.writeFile(1, str.toCharArray(), str.length());
    fileSystem.saveDiskToTextFile("disk2.txt");
    //    fileSystem.initializeDiskFromFile("disk1.txt");

    fileSystem.listDirectory();
    fileSystem.initializeDiskFromFile("disk1.txt");
    fileSystem.listDirectory();
    fileSystem.initializeDiskFromFile("disk2.txt");
    fileSystem.listDirectory();
  }
}
