package com.opk.fs.entity;

import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.opk.fs.entity.LDisk.*;

@Getter
@Setter
public class FileSystem {

  private LDisk disk;

  private Cache cache;

  public FileSystem() {
    disk = new LDisk();
    cache = new Cache();
    FileDescriptor fileDescriptor = new FileDescriptor();
    fileDescriptor.setFileLength(0);
    fileDescriptor.setFileBlocksIndexesInDisk(
        Collections.singletonList(DIRECTORY_FIRST_BLOCK_INDEX));
    saveDescriptorByIndex(0, fileDescriptor);
  }

  public boolean createFile(String name) {
    Directory directory = readDirectory();
    if (!checkFileName(name)) {
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

    FileDescriptor directoryDescriptor = getDescriptorByIndex(0);
    Buffer directoryBuffer = new Buffer(directoryDescriptor, 0);
    int directoryBufferIndex = cache.addBufferToOpenFileTable(directoryBuffer);
    int directoryBlockIndex;
    if (directoryDescriptor.getFileLength() != 0
        && directoryDescriptor.getFileLength() % DISK_SIZE == 0) {
      directoryBlockIndex = getFreeBlockIndex();
      if (directoryBlockIndex == -1) {
        System.out.println("Error, no free block for directory");
        return false;
      }
      directoryDescriptor.getFileBlocksIndexesInDisk().add(directoryBlockIndex);
      saveDescriptorByIndex(0, directoryDescriptor);
      setBlockAsNotEmptyBitMap(directoryBlockIndex);
    } else {
      directoryBlockIndex =
          directoryDescriptor
              .getFileBlocksIndexesInDisk()
              .get(directoryDescriptor.getFileBlocksIndexesInDisk().size() - 1);
    }
    directoryBuffer.getNewBlock(
        IOSystem.readBlock(disk, directoryBlockIndex),
        directoryBlockIndex,
        DISK_SIZE * (directoryDescriptor.getFileBlocksIndexesInDisk().size() - 1));
    byte[] currentBlock = directoryBuffer.getData();
    for (int i = 0; i < NUMBER_OF_FILE_INFOS_IN_BLOCK; i++) {
      if (currentBlock[i * DIRECTORY_FILE_INFO_SIZE] == 0) {
        saveBytesByIndex(currentBlock, name.getBytes(), i * DIRECTORY_FILE_INFO_SIZE);
        saveByteByIndex(
            currentBlock,
            (byte) fileDescriptorIndex.intValue(),
            i * DIRECTORY_FILE_INFO_SIZE + FILE_NAME_LENGTH);
        IOSystem.writeBlock(disk, currentBlock, directoryBlockIndex);
        break;
      }
      directoryBuffer.setCurrentPositionInData(
          directoryBuffer.getCurrentPositionInData() + DIRECTORY_FILE_INFO_SIZE);
      directoryBuffer.setCurrentByteInFile(
          directoryBuffer.getCurrentByteInFile() + DIRECTORY_FILE_INFO_SIZE);
    }
    cache.deleteBufferFromOpenFileTable(directoryBufferIndex);
    int fileFirstBlockIndex = getFreeBlockIndex();
    setBlockAsNotEmptyBitMap(fileFirstBlockIndex);
    FileDescriptor fileDescriptor = new FileDescriptor();
    fileDescriptor.setFileLength(0);
    fileDescriptor.setFileBlocksIndexesInDisk(new ArrayList<>());
    fileDescriptor.getFileBlocksIndexesInDisk().add(fileFirstBlockIndex);
    saveDescriptorByIndex(fileDescriptorIndex, fileDescriptor);
    return true;
  }

  public boolean deleteFile(String name) {
    Directory directory = readDirectory();
    if (!checkFileName(name)) {
      return false;
    }
    FileDescriptor directoryDescriptor = getDescriptorByIndex(0);
    Buffer directoryBuffer = new Buffer(directoryDescriptor, 0);
    int directoryBufferIndex = cache.addBufferToOpenFileTable(directoryBuffer);
    List<Integer> blockIndexes = directoryDescriptor.getFileBlocksIndexesInDisk();
    Integer fileDescriptorIndex = null;
    for (int i = 0; i < blockIndexes.size(); i++) {
      int currentBlockIndex = blockIndexes.get(i);
      directoryBuffer.getNewBlock(
          IOSystem.readBlock(disk, currentBlockIndex), currentBlockIndex, i * DISK_SIZE);
      for (int j = 0; j < DISK_SIZE; j += DIRECTORY_FILE_INFO_SIZE) {
        String fileName =
            new String(Arrays.copyOfRange(directoryBuffer.getData(), j, j + FILE_NAME_LENGTH)).trim();
        if (fileName.equals(name)) {
          fileDescriptorIndex = (int) directoryBuffer.getData()[j + FILE_NAME_LENGTH];
          saveBytesByIndex(directoryBuffer.getData(), new byte[8], j);
          IOSystem.writeBlock(disk, directoryBuffer.getData(), currentBlockIndex);
        }
      }
    }
    cache.deleteBufferFromOpenFileTable(directoryBufferIndex);
    if (fileDescriptorIndex == null) {
      System.out.println("Error, when finding file descriptor");
      return false;
    }
    FileDescriptor fileDescriptor = getDescriptorByIndex(fileDescriptorIndex);
    for (Integer blockIndex : fileDescriptor.getFileBlocksIndexesInDisk()) {
      emptyBlockByIndex(blockIndex);
    }
    saveDescriptorByIndex(fileDescriptorIndex, new FileDescriptor(0, Arrays.asList(0, 0, 0, 0)));

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
          return (i - FILE_DESCRIPTORS_FIRST_BLOCK_INDEX) * NUMBER_OF_DESCRIPTORS_IN_BLOCK + j;
        }
      }
    }

    return null;
  }

  private Directory readDirectory() {
    Directory directory = new Directory();
    FileDescriptor directoryDescriptor = getDescriptorByIndex(0);
    boolean endOfDirectory = false;
    Buffer directoryBuffer = new Buffer(directoryDescriptor, 0);
    int directoryBufferIndex = cache.addBufferToOpenFileTable(directoryBuffer);
    for (int j = 0; j < directoryDescriptor.getFileBlocksIndexesInDisk().size(); j++) {
      Integer blockIndex = directoryDescriptor.getFileBlocksIndexesInDisk().get(j);
      byte[] currentBlock = IOSystem.readBlock(disk, blockIndex);
      directoryBuffer.getNewBlock(currentBlock, blockIndex, j * DISK_SIZE);
      for (int i = 0; i < NUMBER_OF_FILE_INFOS_IN_BLOCK; i++) {
        if (directoryBuffer.getCurrentByteInFile() == directoryDescriptor.getFileLength() - 1
            || directoryDescriptor.getFileLength() == 0) {
          endOfDirectory = true;
          break;
        }
        byte[] fileNameBytes =
            Arrays.copyOfRange(
                directoryBuffer.getData(),
                i * DIRECTORY_FILE_INFO_SIZE,
                (i + 1) * DIRECTORY_FILE_INFO_SIZE);
        String fileName = new String(fileNameBytes);
        int fileDescriptorIndex =
            Byte.valueOf(directoryBuffer.getData()[(i + 1) * DIRECTORY_FILE_INFO_SIZE - 1])
                .intValue();
        FileInfo fileInfo = new FileInfo(fileName, fileDescriptorIndex);
        directory.addFileInfo(fileInfo);
        directoryBuffer.setCurrentPositionInData(
            directoryBuffer.getCurrentPositionInData() + DIRECTORY_FILE_INFO_SIZE);
        directoryBuffer.setCurrentByteInFile(
            directoryBuffer.getCurrentByteInFile() + DIRECTORY_FILE_INFO_SIZE);
      }
      if (endOfDirectory) {
        break;
      }
    }
    cache.deleteBufferFromOpenFileTable(directoryBufferIndex);
    return directory;
  }

  private FileDescriptor getDescriptorByIndex(int index) {
    FileDescriptor fileDescriptor = new FileDescriptor();
    int blockIndex = index / NUMBER_OF_DESCRIPTORS_IN_BLOCK + FILE_DESCRIPTORS_FIRST_BLOCK_INDEX;
    int descriptorStart =
        (index
                - ((blockIndex - FILE_DESCRIPTORS_FIRST_BLOCK_INDEX)
                    * NUMBER_OF_DESCRIPTORS_IN_BLOCK))
            * FILE_DESCRIPTOR_SIZE;
    byte[] currentBlock = IOSystem.readBlock(disk, blockIndex);
    byte[] fileLengthBytes =
        Arrays.copyOfRange(currentBlock, descriptorStart, descriptorStart + FILE_LENGTH_LENGTH);
    fileDescriptor.setFileLength(ByteBuffer.wrap(fileLengthBytes).getInt());
    fileDescriptor.setFileBlocksIndexesInDisk(new ArrayList<>());
    for (int i = descriptorStart + FILE_LENGTH_LENGTH;
        i < descriptorStart + FILE_DESCRIPTOR_SIZE;
        i++) {
      if (currentBlock[i] != 0) {
        fileDescriptor.getFileBlocksIndexesInDisk().add(Byte.valueOf(currentBlock[i]).intValue());
      } else {
        break;
      }
    }
    return fileDescriptor;
  }

  private void saveDescriptorByIndex(int index, FileDescriptor fileDescriptor) {
    int blockIndex = index / NUMBER_OF_DESCRIPTORS_IN_BLOCK + FILE_DESCRIPTORS_FIRST_BLOCK_INDEX;
    int descriptorStart =
        (index
                - ((blockIndex - FILE_DESCRIPTORS_FIRST_BLOCK_INDEX)
                    * NUMBER_OF_DESCRIPTORS_IN_BLOCK))
            * FILE_DESCRIPTOR_SIZE;
    byte[] currentBlock = IOSystem.readBlock(disk, blockIndex);
    byte[] descriptorBytes = new byte[FILE_DESCRIPTOR_SIZE];
    saveBytesByIndex(
        descriptorBytes, ByteBuffer.allocate(4).putInt(fileDescriptor.getFileLength()).array(), 0);
    for (int i = 0; i < fileDescriptor.getFileBlocksIndexesInDisk().size(); i++) {
      saveByteByIndex(
          descriptorBytes,
          (byte) fileDescriptor.getFileBlocksIndexesInDisk().get(i).intValue(),
          FILE_LENGTH_LENGTH + i);
    }
    saveBytesByIndex(currentBlock, descriptorBytes, descriptorStart);
    IOSystem.writeBlock(disk, currentBlock, blockIndex);
  }

  private int getFreeBlockIndex() {
    byte[] bitMap = IOSystem.readBlock(disk, 0);
    for (int i = 0; i < DISK_SIZE; i++) {
      if (bitMap[i] == 0) {
        return i;
      }
    }
    return -1;
  }

  private void setBlockAsNotEmptyBitMap(int index) {
    byte[] bitMap = IOSystem.readBlock(disk, 0);
    bitMap[index] = 1;
    IOSystem.writeBlock(disk, bitMap, 0);
  }

  private void setBlockAsEmptyBitMap(int index) {
    byte[] bitMap = IOSystem.readBlock(disk, 0);
    bitMap[index] = 0;
    IOSystem.writeBlock(disk, bitMap, 0);
  }

  private void saveBytesByIndex(byte[] data, byte[] newData, int startIndex) {
    System.arraycopy(newData, 0, data, startIndex, newData.length);
  }

  private void saveByteByIndex(byte[] data, byte newByte, int index) {
    data[index] = newByte;
  }

  private boolean checkFileName(String name) {
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

    return true;
  }

  private void emptyBlockByIndex(int blockIndex) {
    IOSystem.writeBlock(disk, new byte[DISK_SIZE], blockIndex);
    setBlockAsEmptyBitMap(blockIndex);
  }
}
