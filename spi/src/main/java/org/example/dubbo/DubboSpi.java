package org.example.dubbo;

import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;

@SPI
public interface DubboSpi {
    void say();
}
