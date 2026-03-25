package nl.vu.cs.softwaredesign;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        City city = CityCli.loadCity(args);
        new CityCli(city).run();
    }
}
