package com.aftersnows.JVM.Defense;


import com.aftersnows.JVM.HotSpot;
import sun.misc.Unsafe;

import java.lang.reflect.Modifier;


public class HTTPServiceDEF {
    public HotSpot hotSpot = new HotSpot();
    public Unsafe unsafe = hotSpot.unsafe;
    public String LockClassName ="javax.servlet.http.HttpServlet";
    public HTTPServiceDEF() {}
    public HTTPServiceDEF(boolean IfDEF){
        System.out.println("开始进行对HttpServlet service的操作");
        try {
            Class LockClass = Class.forName(LockClassName);
            long LockClassHandle= HotSpot.getKlass(LockClass);
            long LockClassMethodHandle = HotSpot.getMethodByKlass(LockClassHandle,"service",HotSpot.getMethodSignature(LockClass.getMethod("service",Class.forName("javax.servlet.ServletRequest"),Class.forName("javax.servlet.ServletResponse"))));
            long _access_flagsFields=HotSpot.getFieldOffset("Method","_access_flags");
            int newMethodAccessFlags = unsafe.getInt(LockClassMethodHandle+_access_flagsFields);
            newMethodAccessFlags =newMethodAccessFlags | Modifier.FINAL;
            unsafe.putInt((LockClassMethodHandle+_access_flagsFields),newMethodAccessFlags);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }

    }


}
