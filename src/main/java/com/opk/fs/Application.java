package com.opk.fs;

import com.opk.fs.entity.CommandHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

public class Application {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("ERROR: no input file");
            return;
        }

        Scanner scanner;

        try {
            scanner = new Scanner(new File(args[0]));
        } catch (FileNotFoundException e) {
            System.out.printf("File %s not found\n", args[0]);
            return;
        }

        CommandHandler commandHandler = new CommandHandler();

        PrintStream printStream;
        try {
            printStream = new PrintStream(new FileOutputStream("output.txt"));
            System.setOut(printStream);
        } catch (FileNotFoundException e) {
            System.out.println("Failed to setup output log file");
            return;
        }

        while (scanner.hasNext()) {
            commandHandler.run(scanner.nextLine().split("\\s+"));
        }

        printStream.close();
        scanner.close();
    }
}
