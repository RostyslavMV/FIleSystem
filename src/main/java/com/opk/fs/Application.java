package com.opk.fs;

import com.opk.fs.entity.FileSystem;

public class Application {

  public static void main(String[] args) {
    FileSystem fileSystem = new FileSystem();
    fileSystem.createFile("ggwp");
    fileSystem.openFile( "ggwp" );
    String str="Test";
    for(int i=0;i<20;i++){
      fileSystem.writeFile( 0,str.toCharArray(),4 );
    }
  }
}
