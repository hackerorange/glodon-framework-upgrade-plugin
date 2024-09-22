//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.github.hackerorange.glodonframeworkupgradeplugin.extension.points;

import com.github.hackerorange.glodonframeworkupgradeplugin.domain.processor.PsiFileProcessor;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.serviceContainer.LazyExtensionInstance;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.Nullable;

public class MyBatisPlusUpgradeExtension extends LazyExtensionInstance<PsiFileProcessor> {
    @Attribute("suppressId")
    public String id;
    @Attribute("implementationClassName")
    public String implementationClassName;

    @Override
    protected @Nullable String getImplementationClassName() {
        return implementationClassName;
    }

}
