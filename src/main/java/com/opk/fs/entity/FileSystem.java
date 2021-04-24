package com.opk.fs.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.util.ArrayList;
import java.util.Arrays;

import static com.opk.fs.entity.LDisk.*;

@Getter
@Setter
@AllArgsConstructor
public class FileSystem {

  private LDisk disk;

  private Buffer buffer;

  private Cache cache;

  public boolean createFile(String name) {
    Directory directory = readDirectory();
    if (name == null) {
      System.out.println("Error, file name should be present");
      return false;
    }
    if (name.isBlank()) {
      System.out.println("Error, file name should not be blank");
      return false;
    }
    if (name.length() > FILE_NAME_LENGTH) {
      System.out.println("Error, file must have name not longer than " + FILE_NAME_LENGTH);
      return false;
    }

    for (FileInfo fileInfo : directory.getFileInfos()) {
      if (fileInfo.getSymbolicName().equals(name)) {
        System.out.println("Error, file name should be unique");
        return false;
      }
    }

    Integer fileDescriptorIndex = getFreeDescriptorIndex();
    if (fileDescriptorIndex == null) {
      System.out.println("Error, there is no empty descriptor for a file");
      return false;
    }

    /* TODO: add file to directory and allocate a first block for it, save descriptor of new file to
    disk, save bitmap changes*/

    return true;
  }

  private Integer getFreeDescriptorIndex() {
    for (int i = FILE_DESCRIPTORS_FIRST_BLOCK_INDEX; i <= FILE_DESCRIPTORS_LAST_BLOCK_INDEX; i++) {
      byte[] currentBlock = IOSystem.readBlock(disk, i);
      for (int j = 0; j < NUMBER_OF_DESCRIPTORS_IN_BLOCK; j++) {
        // file descriptor does not contain file if there is no first file data block assigned
        boolean descriptorContainsFile =
            currentBlock[j * FILE_DESCRIPTOR_SIZE + FILE_LENGTH_LENGTH] == 0;
        if (descriptorContainsFile) {
          return i * NUMBER_OF_DESCRIPTORS_IN_BLOCK + j;
        }
      }
    }

    return null;
  }

  private Directory readDirectory() {
    Directory directory = new Directory();
    byte[] currentBlock = IOSystem.readBlock(disk, FILE_DESCRIPTORS_FIRST_BLOCK_INDEX);
    FileDescriptor directoryDescriptor = new FileDescriptor();
    byte[] fileLengthBytes = Arrays.copyOfRange(currentBlock, 0, 4);
    directoryDescriptor.setFileLength(ByteBuffer.wrap(fileLengthBytes).getInt());
    directoryDescriptor.setFileBlocksIndexesInDisk(new ArrayList<>());
    for (int i = FILE_LENGTH_LENGTH; i < FILE_DESCRIPTOR_SIZE; i++) {
      if (currentBlock[i] != 0) {
        directoryDescriptor
            .getFileBlocksIndexesInDisk()
            .add(Byte.valueOf(currentBlock[i]).intValue());
      } else {
        break;
      }
    }

    int currentIndex = 0;
    boolean endOfDirectory = false;
    for (Integer blockIndex : directoryDescriptor.getFileBlocksIndexesInDisk()) {
      currentBlock = IOSystem.readBlock(disk, blockIndex);
      for (int i = 0; i < NUMBER_OF_FILE_INFOS_IN_BLOCK; i++) {
        if (currentIndex == directoryDescriptor.fileLength - 1) {
          endOfDirectory = true;
          break;
        }
        byte[] fileNameBytes =
            Arrays.copyOfRange(
                currentBlock, i * DIRECTORY_FILE_INFO_SIZE, (i + 1) * DIRECTORY_FILE_INFO_SIZE);
        String fileName = new String(fileNameBytes);
        int fileDescriptorIndex =
            Byte.valueOf(currentBlock[(i + 1) * DIRECTORY_FILE_INFO_SIZE]).intValue();
        FileInfo fileInfo = new FileInfo(fileName, fileDescriptorIndex);
        directory.addFileInfo(fileInfo);
      }
      if (endOfDirectory) {
        break;
      }
    }
    return directory;
  }
}
