package io.bitsquare.testbed;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SeedNodeApp {
    public static void main(String[] args) {
        Path dataDir = Paths.get(System.getProperty("user.dir"));

        System.out.println(dataDir.toString());
    }
}
