package org.example.dubbo;

public class BDubboSpi implements DubboSpi {
    @Override
    public void say() {
        System.out.println("B");
    }
}
