package com.aftersnows.utils;

import java.io.File;
        import java.lang.reflect.Method;
        import java.net.URL;
        import java.net.URLClassLoader;
        import java.security.AccessController;
        import java.security.PrivilegedActionException;
        import java.security.PrivilegedExceptionAction;
        import java.util.ArrayList;
        import java.util.List;

/**
 * 从lib/tools.jar加载VirtualMachine
 */
public class VirtualMachineClassLoader {
    private static final String VIRTUAL_MACHINE_CLASSNAME = "com.sun.tools.attach.VirtualMachine";
    private static volatile Class<?> vmClass;

    static {
        try {
            vmClass = getVirtualMachineClass();
        } catch (ClassNotFoundException e) {
            System.err.println("not found tools.jar");
        }
    }

    public static Class<?> getVmClass() {
        return vmClass;
    }

    private static Class<?> getVirtualMachineClass() throws ClassNotFoundException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                public Class<?> run() throws Exception {
                    try {
                        return ClassLoader.getSystemClassLoader().loadClass(VIRTUAL_MACHINE_CLASSNAME);
                    } catch (ClassNotFoundException cnfe) {
                        for (File jar : getPossibleToolsJars()) {
                            try {
                                Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                                method.setAccessible(true);
                                method.invoke(ClassLoader.getSystemClassLoader(), jar.toURI().toURL());

                                return ClassLoader.getSystemClassLoader().loadClass(VIRTUAL_MACHINE_CLASSNAME);
                            } catch (Exception t) {
                                System.err.println("Exception while loading tools.jar from  " + jar + " " + t);
                            }
                        }
                        throw new ClassNotFoundException(VIRTUAL_MACHINE_CLASSNAME);
                    }
                }
            });
        } catch (PrivilegedActionException pae) {
            Throwable actual = pae.getCause();
            if (actual instanceof ClassNotFoundException) {
                throw (ClassNotFoundException) actual;
            }
            throw new AssertionError("Unexpected checked exception : " + actual);
        }
    }

    private static List<File> getPossibleToolsJars() {
        List<File> jars = new ArrayList<>();

        File javaHome = new File(System.getProperty("java.home"));
        File jreSourced = new File(javaHome, "lib/tools.jar");
        if (jreSourced.exists()) {
            jars.add(jreSourced);
        }
        if ("jre".equals(javaHome.getName())) {
            File jdkHome = new File(javaHome, "../");
            File jdkSourced = new File(jdkHome, "lib/tools.jar");
            if (jdkSourced.exists()) {
                jars.add(jdkSourced);
            }
        }
        return jars;
    }
}
