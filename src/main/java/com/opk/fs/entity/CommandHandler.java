package com.opk.fs.entity;

public class CommandHandler {
    FileSystem fileSystem;
    String[] args;

    public CommandHandler(){
        fileSystem = new FileSystem();
    }

    public void run(String[] args){
        this.args = args;

        if (args.length < 1){
            ErrorMessage.printHelp();
            return;
        }

        switch (args[0]){
            case "cd":
                doCd();
                return;
            case "de":
                doDe();
                return;
            case "op":
                doOp();
                return;
            case "cl":
                doCl();
                return;
            case "rd":
                doRd();
                return;
            case "wr":
                doWr();
                return;
            case "sk":
                doSk();
                return;
            case "dr":
                doDr();
                return;
            case "in":
                doIn();
                return;
            case "sv":
                doSv();
                return;
            default:
        }
    }

    public boolean argumentsCountCheck(int required){
        if (args.length != required){
            ErrorMessage.printNotEnoughArguments(args[0]);
            return false;
        }

        return true;
    }

    private void doCd(){
        if (!argumentsCountCheck(2)){
            return;
        }

        String filename = args[1];

        fileSystem.createFile(filename);
    }

    private void doDe(){
        if (!argumentsCountCheck(2)){
            return;
        }

        String fileName = args[1];

        fileSystem.deleteFile(fileName);
    }

    private void doOp(){
        if (!argumentsCountCheck(2)){
            return;
        }

        String fileName = args[1];

        fileSystem.openFile(fileName);
    }

    private void doCl(){
        if (!argumentsCountCheck(2)){
            return;
        }

        int index = Integer.parseInt(args[1]);

        fileSystem.closeFile(index);
    }

    private void doRd(){
        if (!argumentsCountCheck(3)){
            return;
        }

        int index = Integer.parseInt(args[1]);
        int count = Integer.parseInt(args[2]);

        fileSystem.readFile(index, count);
    }

    private void doWr(){
        if (!argumentsCountCheck(4)){
            return;
        }

        Integer index = Integer.parseInt(args[1]);
        String characters = args[2];
        int count = Integer.parseInt(args[3]);

        fileSystem.writeFile(index, characters, count);
    }

    private void doSk(){
        if (!argumentsCountCheck(3)){
            return;
        }

        int index = Integer.parseInt(args[1]);
        int pos = Integer.parseInt(args[2]);

        fileSystem.seek(index, pos);
    }

    private void doDr(){
        if (!argumentsCountCheck(1)){
            return;
        }

        fileSystem.listDirectory();
    }

    private void doIn(){
        if (!argumentsCountCheck(2)){
            return;
        }

        String filename = args[1];

        fileSystem.initializeDiskFromFile(filename);
    }

    private void doSv(){
        if (!argumentsCountCheck(2)){
            return;
        }

        String filename = args[1];

        fileSystem.saveDiskToTextFile(filename);
    }

}
