package edu.um.planet.gui;

import java.io.File;

public class Test {

    public static void main(String[] args) {
        File f =new File("data/kernel");
        for(File e : f.listFiles()) {
            System.out.println("'kernel/" + e.getName() + "',");
        }
    }

}
