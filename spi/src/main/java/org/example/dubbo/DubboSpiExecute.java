package org.example.dubbo;

import org.apache.dubbo.common.extension.ExtensionLoader;

public class DubboSpiExecute {
    public static void main(String[] args) {
        ExtensionLoader<DubboSpi> extensionLoader =
            ExtensionLoader.getExtensionLoader(DubboSpi.class);
        DubboSpi a = extensionLoader.getExtension("true");
        a.say();
        DubboSpi b = extensionLoader.getExtension("B");
        b.say();
    }
}
