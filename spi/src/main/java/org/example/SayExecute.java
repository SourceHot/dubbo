package org.example;

import java.util.ServiceLoader;

public class SayExecute {
    public static void main(String[] args) {
        ServiceLoader<Say> load = ServiceLoader.load(Say.class);
        for (Say say : load) {
            say.say("hi");
        }
    }
}
