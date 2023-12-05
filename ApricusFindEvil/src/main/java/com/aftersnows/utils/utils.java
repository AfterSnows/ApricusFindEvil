package com.aftersnows.utils;

import java.lang.reflect.Field;
import java.util.List;

public class utils {
    //将点换成斜杠
    public static String CanonicalClassNameToBackslash(String className){
        return className.replace(".", "/");
    }
    public static String CanonicalClassNameToPoint(String className){
        return className.replace("/", ".");
    }
    public static Class<?>[] ConvertClassToArray(List<Class<?>> classList) {
        Class<?>[] array = new Class<?>[classList.size()];
        classList.toArray(array);
        return array;
    }
    public static Object getField(Object object, String fieldName) throws Exception {
        Field field = null;
        Class clazz = object.getClass();

        while (clazz != Object.class) {
            try {
                field = clazz.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException var5) {
                clazz = clazz.getSuperclass();
            }
        }

        if (field == null) {
            throw new NoSuchFieldException(fieldName);
        } else {
            field.setAccessible(true);
            return field.get(object);
        }
    }
    public static ThreadGroup getSystemThreadGroup() {
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        while (!group.getName().equals("system")) {
            group = group.getParent();
        }
        return group;
    }
    public static String extractMainClassName(String innerClassName) {
        int index = innerClassName.lastIndexOf('$');
        if (index != -1) {
            return innerClassName.substring(0, index);
        }
        return innerClassName;
    }

    public static Thread[] ConvertThreadToArray(List<Thread> ThreadList) {
        Thread[] array = new Thread[ThreadList.size()];
        ThreadList.toArray(array);
        return array;
    }

}
