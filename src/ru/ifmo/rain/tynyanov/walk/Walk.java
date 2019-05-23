package ru.ifmo.rain.tynyanov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;

public class Walk {
    private static final int FNV_PRIME = 0x01000193;
    private static final int FNV_INITIAL = 0x811c9dc5;
    private static final int FNV_MOD = 0xff;

    private static int countHash(final String fileName) {
        int hash = FNV_INITIAL;
        try (FileInputStream fileReader = new FileInputStream(fileName)) {
            byte[] byteArr = new byte[1024];
            int curByte;
            while ((curByte = fileReader.read(byteArr)) != -1) {
                for (int i = 0; i < curByte; i++) {
                    hash = (hash * FNV_PRIME) ^ (byteArr[i] & FNV_MOD);
                }
            }
        } catch (InvalidPathException | IOException e) {
            return 0;
        }
        return hash;
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null ) {
            System.out.println("Wrong amount of arguments: 2 needed");
        } else {
            try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(args[0]), StandardCharsets.UTF_8))) {
                String fileName;
                String outputFile = new File(args[1]).getParent();
                if (outputFile != null) {
                    File directory = new File(outputFile);
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }
                }
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(args[1]), StandardCharsets.UTF_8))) {
                    fileName = inputReader.readLine();
                    while (fileName != null) {
                        int curHash = countHash(fileName);
                        writer.write(String.format("%08x", curHash) + " " + fileName);
                        writer.newLine();
                        fileName = inputReader.readLine();
                    }
                } catch (IOException e) {
                    System.out.println("Error while working with output file: " + args[1]);
                }
            } catch (FileNotFoundException e) {
                System.out.println("Input file not found");
            } catch (UnsupportedEncodingException e) {
                System.out.println("Encoding is not supported");
            } catch (IOException e) {
                System.out.println("Error while reading input file");
            }
        }
    }
}
