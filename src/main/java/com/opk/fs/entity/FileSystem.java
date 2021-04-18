package com.opk.fs.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FileSystem {

    private LDisk disk;

    private Buffer buffer;

    private Cache cache;
}
