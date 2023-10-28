package com.gmail.visualbukkit.blocks.parameters;

import com.gmail.visualbukkit.blocks.classes.ClassInfo;
import com.gmail.visualbukkit.blocks.classes.ClassRegistry;
import com.gmail.visualbukkit.ui.PopOverSelector;

public class ClassParameter extends PopOverSelector<ClassInfo> implements BlockParameter {

    public ClassParameter() {
        super(ClassRegistry.getClasses());
    }

    @Override
    public String generateJava() {
        return getValue() != null ? getValue().getName() : null;
    }

    @Override
    public Object serialize() {
        return generateJava();
    }

    @Override
    public void deserialize(Object obj) {
        if (obj instanceof String clazz) {
            setValue(ClassRegistry.getClass(clazz).orElseThrow(() -> new IllegalStateException("Class not registered: " + clazz)));
        }
    }
}
