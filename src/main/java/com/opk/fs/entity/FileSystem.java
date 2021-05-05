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
    initialize();
  }

  public void initialize() {
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
    Integer directoryBufferIndex = cache.addBufferToOpenFileTable(directoryBuffer);
    if (directoryBufferIndex == null) {
      System.out.println("Error, while adding file to cache");
      return false;
    }
    int directoryBlockIndex;
    if (directoryDescriptor.getFileLength() != 0
        && directoryDescriptor.getFileLength() % DISK_SIZE == 0) {
      directoryBlockIndex = getFreeBlockIndex();
      if (directoryBlockIndex == -1) {
        System.out.println("Error, no free block for directory");
        return false;
      }
      directoryDescriptor.getFileBlocksIndexesInDisk().add(directoryBlockIndex);
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
        if (directoryBuffer.getCurrentByteInFile() == directoryDescriptor.getFileLength()) {
          directoryDescriptor.setFileLength(
              directoryDescriptor.getFileLength() + DIRECTORY_FILE_INFO_SIZE);
        }
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
    saveDescriptorByIndex(0, directoryDescriptor);
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

  public boolean readFile(Integer index, int count) {
    if (!checkIndex(index)) {
      return false;
    }
    Buffer buffer = cache.getOpenFileTable()[index];
    if (buffer == null) {
      System.out.println("File not opened");
      return false;
    }
    FileDescriptor descriptor = buffer.getFileDescriptor();
    if (count > descriptor.getFileLength() - buffer.getCurrentByteInFile()) {
      count = descriptor.getFileLength() - buffer.getCurrentByteInFile();
    }
    StringBuilder result = new StringBuilder();
    int numbersOfBytesRead = 0;
    while (numbersOfBytesRead < count) {
      byte[] currentData = buffer.getData();
      int blockIndex = buffer.getCurrentByteInFile() / DISK_SIZE;
      int length = DISK_SIZE - buffer.getCurrentPositionInData();
      if (DISK_SIZE - buffer.getCurrentPositionInData() > count - numbersOfBytesRead) {
        length = count - numbersOfBytesRead;
      }
      result.append(
          new String(
              Arrays.copyOfRange(
                  currentData,
                  buffer.getCurrentPositionInData(),
                  buffer.getCurrentPositionInData() + length)));
      buffer.setCurrentByteInFile(buffer.getCurrentByteInFile() + length);
      buffer.setCurrentPositionInData(buffer.getCurrentPositionInData() + length);
      if (buffer.getCurrentPositionInData() == DISK_SIZE) {
        if (blockIndex < buffer.getFileDescriptor().getFileBlocksIndexesInDisk().size() - 1) {
          int nextBlockIndex =
              buffer.getFileDescriptor().getFileBlocksIndexesInDisk().get(blockIndex + 1);
          buffer.getNewBlock(
              IOSystem.readBlock(disk, nextBlockIndex),
              nextBlockIndex,
              buffer.getCurrentByteInFile());
        }
      }
      saveDescriptorByIndex(buffer.getDescriptorIndex(), descriptor);
      numbersOfBytesRead += length;
    }
    System.out.println(count + " bytes read: " + result);
    return true;
  }

  public boolean writeFile(Integer index,String stringData, int count) {
    StringBuilder newData= new StringBuilder( );
    if(stringData.length()<count){
      for(int i=0;i<count;){
        int minLength=Math.min(count-i,stringData.length());
        i+=minLength;
        newData.append( stringData.substring(0,minLength) );
      }
    }
    char[] data=newData.toString( ).toCharArray();
    if (!checkIndex(index)) {
      return false;
    }
    Buffer buffer = cache.getOpenFileTable()[index];
    if (buffer == null) {
      System.out.println("File not opened");
      return false;
    }
//    if (count > data.length) {
//      count = data.length;
//    }
    int currentPositionInNewData = 0;
    while (currentPositionInNewData < count) {
      byte[] currentData = buffer.getData();
      int blockIndex = buffer.getCurrentByteInFile() / DISK_SIZE;
      int length = DISK_SIZE - buffer.getCurrentPositionInData();
      if (DISK_SIZE - buffer.getCurrentPositionInData() > count - currentPositionInNewData) {
        length = count - currentPositionInNewData;
      }
      saveBytesByIndexAndLength(
          currentData,
          new String(data).getBytes(),
          buffer.getCurrentPositionInData(),
          length,
          currentPositionInNewData);

      buffer.setCurrentByteInFile(buffer.getCurrentByteInFile() + length);
      buffer.setCurrentPositionInData(buffer.getCurrentPositionInData() + length);
      FileDescriptor descriptor = buffer.getFileDescriptor();
      descriptor.setFileLength(Math.max(buffer.getCurrentByteInFile(), descriptor.getFileLength()));
      if (buffer.getCurrentPositionInData() == DISK_SIZE) {
        int nextBlockIndex;
        if (blockIndex == buffer.getFileDescriptor().getFileBlocksIndexesInDisk().size() - 1) {
          nextBlockIndex = getFreeBlockIndex();
          descriptor.getFileBlocksIndexesInDisk().add(nextBlockIndex);
        } else {
          nextBlockIndex =
              buffer.getFileDescriptor().getFileBlocksIndexesInDisk().get(blockIndex + 1);
        }
        buffer.getNewBlock(
            IOSystem.readBlock(disk, nextBlockIndex),
            nextBlockIndex,
            buffer.getCurrentByteInFile());
      }
      IOSystem.writeBlock(
          disk,
          currentData,
          buffer.getFileDescriptor().getFileBlocksIndexesInDisk().get(blockIndex));
      saveDescriptorByIndex(buffer.getDescriptorIndex(), descriptor);
      currentPositionInNewData += length;
    }
    System.out.println(count + " bytes written");
    return true;
  }

  public boolean deleteFile(String name) {
    if (!checkFileName(name)) {
      return false;
    }
    closeFileByName(name);
    FileDescriptor directoryDescriptor = getDescriptorByIndex(0);
    Buffer directoryBuffer = new Buffer(directoryDescriptor, 0);
    Integer directoryBufferIndex = cache.addBufferToOpenFileTable(directoryBuffer);
    if (directoryBufferIndex == null) {
      System.out.println("Error, while adding file to cache");
      return false;
    }
    List<Integer> blockIndexes = directoryDescriptor.getFileBlocksIndexesInDisk();
    Integer fileDescriptorIndex = null;
    for (int i = 0; i < blockIndexes.size(); i++) {
      int currentBlockIndex = blockIndexes.get(i);
      directoryBuffer.getNewBlock(
          IOSystem.readBlock(disk, currentBlockIndex), currentBlockIndex, i * DISK_SIZE);
      for (int j = 0; j < DISK_SIZE; j += DIRECTORY_FILE_INFO_SIZE) {
        String fileName =
            new String(Arrays.copyOfRange(directoryBuffer.getData(), j, j + FILE_NAME_LENGTH))
                .trim();
        if (fileName.equals(name)) {
          if (directoryBuffer.getCurrentByteInFile() + DIRECTORY_FILE_INFO_SIZE
              >= directoryDescriptor.getFileLength() - 1) {
            directoryDescriptor.setFileLength(
                directoryDescriptor.getFileLength() - DIRECTORY_FILE_INFO_SIZE);
          }
          fileDescriptorIndex = (int) directoryBuffer.getData()[j + FILE_NAME_LENGTH];
          saveBytesByIndex(directoryBuffer.getData(), new byte[8], j);
          IOSystem.writeBlock(disk, directoryBuffer.getData(), currentBlockIndex);
        }
        directoryBuffer.setCurrentPositionInData(
            directoryBuffer.getCurrentPositionInData() + DIRECTORY_FILE_INFO_SIZE);
        directoryBuffer.setCurrentByteInFile(
            directoryBuffer.getCurrentByteInFile() + DIRECTORY_FILE_INFO_SIZE);
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
    saveDescriptorByIndex(directoryBuffer.getDescriptorIndex(), directoryDescriptor);
    saveDescriptorByIndex(fileDescriptorIndex, new FileDescriptor(0, Arrays.asList(0, 0, 0, 0)));
    return true;
  }

  public boolean openFile(String name) {
    Directory directory = readDirectory();
    Integer index = null;
    for (FileInfo fileInfo : directory.getFileInfos()) {
      if (fileInfo.getSymbolicName().equals(name)) {
        int fileDescriptorIndex = fileInfo.getDescriptorIndex();
        FileDescriptor fileDescriptor = getDescriptorByIndex(fileDescriptorIndex);
        Buffer buffer = new Buffer(fileDescriptor, fileDescriptorIndex);
        index = cache.addBufferToOpenFileTable(buffer);
        if (index == null) {
          System.out.println("Error, while adding file to cache");
          return false;
        } else {
          int blockIndexInDisk = fileDescriptor.getFileBlocksIndexesInDisk().get(0);
          buffer.getNewBlock(IOSystem.readBlock(disk, blockIndexInDisk), blockIndexInDisk, 0);
        }
      }
    }
    if (index == null) {
      System.out.println("Error, while opening file");
      return false;
    }

    System.out.println("File " + name + " opened, index = " + (index + 1));
    return true;
  }

  public boolean closeFile(Integer index) {
    if (!checkIndex(index)) {
      return false;
    }
    cache.deleteBufferFromOpenFileTable(index);
    return true;
  }

  public boolean listDirectory() {
    Directory directory = readDirectory();
    System.out.println("Directory files (name | length): ");
    for (FileInfo fileInfo : directory.getFileInfos()) {
      FileDescriptor fileDescriptor = getDescriptorByIndex(fileInfo.getDescriptorIndex());
      System.out.println(fileInfo.getSymbolicName() + " " + fileDescriptor.getFileLength());
    }
    return true;
  }

  public boolean seek(Integer index, int pos) {
    if (!checkIndex(index)) {
      return false;
    }
    Buffer buffer = cache.getOpenFileTable()[index];
    if (buffer == null) {
      System.out.println("Error, file is not opened");
      return false;
    }
    FileDescriptor fileDescriptor = buffer.getFileDescriptor();
    if (pos < 0 || pos > fileDescriptor.getFileLength()) {
      System.out.println("Error, seek position is out of file");
      return false;
    }
    int blockIndexInDescriptor = pos / DISK_SIZE;
    int blockIndex = fileDescriptor.getFileBlocksIndexesInDisk().get(blockIndexInDescriptor);
    buffer.getNewBlock(IOSystem.readBlock(disk, blockIndex), blockIndex, pos);
    buffer.setCurrentPositionInData(pos % DISK_SIZE);
    System.out.println("Current position is " + pos);
    return true;
  }

  public boolean initializeDiskFromFile(String fileName) {
    LDisk lDisk = IOSystem.getDiskFromTextFile(fileName);
    initialize();
    if (lDisk == null) {
      System.out.println("Disk initialized");
      return false;
    }
    disk = lDisk;
    System.out.println("Disk restored from " + fileName);
    return true;
  }

  public boolean saveDiskToTextFile(String fileName) {
    for (int i = 0; i < cache.getOpenFileTable().length; i++) {
      closeFile(i);
    }
    IOSystem.saveToTextFile(disk, fileName);
    System.out.println("Disk saved to text file");
    return true;
  }

  private void closeFileByName(String name) {
    Directory directory = readDirectory();
    for (FileInfo fileInfo : directory.getFileInfos()) {
      if (fileInfo.getSymbolicName().equals(name)) {
        for (int i = 0; i < cache.getOpenFileTable().length; i++) {
          if (cache.getOpenFileTable()[i] != null
              && cache.getOpenFileTable()[i].getDescriptorIndex()
                  == fileInfo.getDescriptorIndex()) {
            closeFile(i);
          }
        }
      }
    }
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
    Integer directoryBufferIndex = cache.addBufferToOpenFileTable(directoryBuffer);
    if (directoryBufferIndex == null) {
      System.out.println("Error, reading directory");
      return null;
    }
    for (int j = 0; j < directoryDescriptor.getFileBlocksIndexesInDisk().size(); j++) {
      Integer blockIndex = directoryDescriptor.getFileBlocksIndexesInDisk().get(j);
      byte[] currentBlock = IOSystem.readBlock(disk, blockIndex);
      directoryBuffer.getNewBlock(currentBlock, blockIndex, j * DISK_SIZE);
      for (int i = 0; i < NUMBER_OF_FILE_INFOS_IN_BLOCK; i++) {
        if (directoryBuffer.getCurrentByteInFile() >= directoryDescriptor.getFileLength() - 1
            || directoryDescriptor.getFileLength() == 0) {
          endOfDirectory = true;
          break;
        }
        byte[] fileNameBytes =
            Arrays.copyOfRange(
                directoryBuffer.getData(),
                i * DIRECTORY_FILE_INFO_SIZE,
                (i + 1) * DIRECTORY_FILE_INFO_SIZE);
        String fileName = new String(fileNameBytes).trim();
        if (!fileName.isBlank()) {
          int fileDescriptorIndex =
              Byte.valueOf(directoryBuffer.getData()[(i + 1) * DIRECTORY_FILE_INFO_SIZE - 1])
                  .intValue();
          FileInfo fileInfo = new FileInfo(fileName, fileDescriptorIndex);
          directory.addFileInfo(fileInfo);
        }
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

  private void saveBytesByIndexAndLength(
      byte[] data, byte[] newData, int startIndex, int length, int newDataIndex) {
    System.arraycopy(newData, newDataIndex, data, startIndex, length);
  }
  private void saveCharsByIndexAndLength(
          char[] data, char[] newData, int startIndex, int length, int newDataIndex) {
    System.arraycopy(newData, newDataIndex, data, startIndex, length);
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

  private boolean checkIndex(Integer index) {
    if (index == null) {
      System.out.println("Error, index should not be null");
      return false;
    }
    if (index <= 0 && index >= cache.getOpenFileTable().length) {
      System.out.println("Error, index should be in range in OFT");
      return false;
    }
    return true;
  }

  private void emptyBlockByIndex(int blockIndex) {
    IOSystem.writeBlock(disk, new byte[DISK_SIZE], blockIndex);
    setBlockAsEmptyBitMap(blockIndex);
  }
}
