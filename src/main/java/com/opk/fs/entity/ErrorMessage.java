package com.opk.fs.entity;

public class ErrorMessage {
    public static void printNotEnoughArguments(String command){
        System.out.printf("%s: not enough arguments\n", command);
    }

    public static void printHelp(){
        System.out.println("STUB: HELP");
    }
}
