package com.opk.fs.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LDisk {
    @Getter
    private static final int DISK_SIZE = 64;

    private byte[][] disk = new byte[DISK_SIZE][DISK_SIZE];

}
