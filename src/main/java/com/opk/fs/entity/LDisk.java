package com.opk.fs.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class LDisk implements Serializable {
    public static final int DISK_SIZE = 64;
    public static final Integer BIT_MAP_BLOCK_INDEX = 0;
    public static final Integer DIRECTORY_FIRST_BLOCK_INDEX = 4;
    public static final Integer FILE_DESCRIPTORS_FIRST_BLOCK_INDEX = 1;
    public static final Integer FILE_DESCRIPTORS_LAST_BLOCK_INDEX = 3;
    public static final Integer FILE_NAME_LENGTH = 7;
    public static final Integer FILE_DESCRIPTOR_INDEX_SIZE = 1;
    public static final Integer DIRECTORY_FILE_INFO_SIZE =
            FILE_NAME_LENGTH + FILE_DESCRIPTOR_INDEX_SIZE;
    public static final Integer FILE_LENGTH_LENGTH = 4;
    public static final Integer FILE_DESCRIPTOR_BLOCK_INDEXES_SIZE = 4;
    public static final Integer FILE_DESCRIPTOR_SIZE =
            FILE_LENGTH_LENGTH + FILE_DESCRIPTOR_BLOCK_INDEXES_SIZE;
    public static final Integer NUMBER_OF_DESCRIPTORS_IN_BLOCK = DISK_SIZE / FILE_DESCRIPTOR_SIZE;
    public static final Integer NUMBER_OF_FILE_INFOS_IN_BLOCK = DISK_SIZE / DIRECTORY_FILE_INFO_SIZE;

    private byte[][] disk;

    public LDisk( ) {
        disk = new byte[DISK_SIZE][DISK_SIZE];
        for ( int i = 0; i <= DIRECTORY_FIRST_BLOCK_INDEX; i++ ) {
            disk[0][i] = 1;
        }
    }
}

