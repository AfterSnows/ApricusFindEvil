package com.aftersnows.JVM.Defense;

import com.aftersnows.JVM.HotSpot;

public class agentDEF {

    public agentDEF() throws NoSuchMethodException, ClassNotFoundException {
        SystemGetPropertyHook();
        HTTPServiceHook();
    }

    public void SystemGetPropertyHook() throws NoSuchMethodException {
        long LockClassHandle = HotSpot.getKlass(DEFUtils.class);
        long systemClassHandle = HotSpot.getKlass(System.class);
        String methodType = HotSpot.hasType("methodOopDesc") ? "methodOopDesc" : "Method";
        long systemMethodHandle = HotSpot.getMethodByKlass(systemClassHandle, "getProperty", HotSpot.getMethodSignature(System.class.getMethod("getProperty", String.class)));
        long hotspotMethodHandle = HotSpot.getMethodByKlass(LockClassHandle, "getProperty", HotSpot.getMethodSignature(DEFUtils.class.getMethod("getProperty", String.class)));
        System.out.println("被hook的方法地址 system->getProperty : 0x" + Long.toHexString(systemMethodHandle));
        String constMethodType = HotSpot.hasType("ConstMethod") ? "ConstMethod" : "constMethodOopDesc";
        long methodIdField = HotSpot.getFieldOffset(constMethodType, HotSpot.hasField(constMethodType, "_method_idnum") ? "_method_idnum" : "_method_index");
        long _constMethodField = HotSpot.getFieldOffset(methodType, "_constMethod");
        long targetConstMethod = HotSpot.unsafe.getAddress(systemMethodHandle + _constMethodField);
        long newConstMethod = HotSpot.unsafe.getAddress(hotspotMethodHandle + _constMethodField);
        short hotspot_method_idnum = HotSpot.unsafe.getShort(newConstMethod + methodIdField);
        short system_method_idnum = HotSpot.unsafe.getShort(newConstMethod + methodIdField);
        HotSpot.typeSwap(methodType, hotspotMethodHandle, systemMethodHandle);

        HotSpot.unsafe.putShort(targetConstMethod + methodIdField, hotspot_method_idnum);
        HotSpot.unsafe.putShort(newConstMethod + methodIdField, system_method_idnum);
    }

    public void HTTPServiceHook() throws NoSuchMethodException, ClassNotFoundException {
        String LockClassName = "javax.servlet.http.HttpServlet";
        Class LockClass = Class.forName(LockClassName);
        long LockClassHandle = HotSpot.getKlass(LockClass);
        String methodType = HotSpot.hasType("methodOopDesc") ? "methodOopDesc" : "Method";
        long systemMethodHandle = HotSpot.getMethodByKlass(LockClassHandle, "service", HotSpot.getMethodSignature(LockClass.getMethod("service", Class.forName("javax.servlet.ServletRequest"),Class.forName("javax.servlet.ServletResponse"))));
        long hotspotMethodHandle = HotSpot.getMethodByKlass(LockClassHandle, "service", HotSpot.getMethodSignature(LockClass.getMethod("service", Class.forName("javax.servlet.ServletRequest"),Class.forName("javax.servlet.ServletResponse"))));


        System.out.println("被hook的方法地址 HttpServlet->service : 0x" + Long.toHexString(systemMethodHandle));
        String constMethodType = HotSpot.hasType("ConstMethod") ? "ConstMethod" : "constMethodOopDesc";
        long methodIdField = HotSpot.getFieldOffset(constMethodType, HotSpot.hasField(constMethodType, "_method_idnum") ? "_method_idnum" : "_method_index");
        long _constMethodField = HotSpot.getFieldOffset(methodType, "_constMethod");
        long targetConstMethod = HotSpot.unsafe.getAddress(systemMethodHandle + _constMethodField);
        long newConstMethod = HotSpot.unsafe.getAddress(hotspotMethodHandle + _constMethodField);
        short hotspot_method_idnum = HotSpot.unsafe.getShort(newConstMethod + methodIdField);
        short system_method_idnum = HotSpot.unsafe.getShort(newConstMethod + methodIdField);
        HotSpot.typeSwap(methodType, hotspotMethodHandle, systemMethodHandle);
        HotSpot.unsafe.putShort(targetConstMethod + methodIdField, hotspot_method_idnum);
        HotSpot.unsafe.putShort(newConstMethod + methodIdField, system_method_idnum);
    }
    public void WindDumperHook() throws NoSuchMethodException, ClassNotFoundException {
        String LockClassName = "sun.jvm.hotspot.debugger.windbg.WindbgDebuggerLocal";
        Class LockClass = Class.forName(LockClassName);
        long LockClassHandle = HotSpot.getKlass(LockClass);
        String methodType = HotSpot.hasType("methodOopDesc") ? "methodOopDesc" : "Method";
        long systemMethodHandle = HotSpot.getMethodByKlass(LockClassHandle, "attach", HotSpot.getMethodSignature(LockClass.getMethod("attach", int.class)));
        long hotspotMethodHandle = HotSpot.getMethodByKlass(LockClassHandle, "attach", HotSpot.getMethodSignature(DEFUtils.class.getMethod("attach", int.class)));
        System.out.println("被hook的方法地址 WindbgDebuggerLocal->attach : 0x" + Long.toHexString(systemMethodHandle));
        String constMethodType = HotSpot.hasType("ConstMethod") ? "ConstMethod" : "constMethodOopDesc";
        long methodIdField = HotSpot.getFieldOffset(constMethodType, HotSpot.hasField(constMethodType, "_method_idnum") ? "_method_idnum" : "_method_index");
        long _constMethodField = HotSpot.getFieldOffset(methodType, "_constMethod");
        long targetConstMethod = HotSpot.unsafe.getAddress(systemMethodHandle + _constMethodField);
        long newConstMethod = HotSpot.unsafe.getAddress(hotspotMethodHandle + _constMethodField);
        short hotspot_method_idnum = HotSpot.unsafe.getShort(newConstMethod + methodIdField);
        short system_method_idnum = HotSpot.unsafe.getShort(newConstMethod + methodIdField);
        HotSpot.typeSwap(methodType, hotspotMethodHandle, systemMethodHandle);

        HotSpot.unsafe.putShort(targetConstMethod + methodIdField, hotspot_method_idnum);
        HotSpot.unsafe.putShort(newConstMethod + methodIdField, system_method_idnum);
    }


}
