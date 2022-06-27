package org.example.dubbo;

public class ADubboSpi implements DubboSpi {
    @Override
    public void say() {
        System.out.println("A");
    }
}
