package com.aftersnows.JVM.Defense;

import com.aftersnows.JVM.HotSpot;
import sun.misc.Unsafe;

import java.lang.reflect.Modifier;


public class DEF {
    public String LockClassName ;
    public String MethodName;
    public DEF() {}
    public DEF(String lockClassName,String methodName){
        System.out.println("开始进行对HttpServlet service的操作");
        this.LockClassName=lockClassName;
        this.MethodName=methodName;
        try {
            HotSpot hotSpot = new HotSpot();
            Unsafe unsafe = hotSpot.unsafe;
            Class LockClass = Class.forName(LockClassName);
            long LockClassHandle= HotSpot.getKlass(LockClass);
            long LockClassMethodHandle = HotSpot.getMethodByKlass(LockClassHandle,MethodName,HotSpot.getMethodSignature(LockClass.getMethod(MethodName)));
            long _access_flagsFields=HotSpot.getFieldOffset("Method","_access_flags");
            int newMethodAccessFlags = unsafe.getInt(LockClassMethodHandle+_access_flagsFields);
            newMethodAccessFlags =newMethodAccessFlags | Modifier.FINAL;
            unsafe.putInt((LockClassMethodHandle+_access_flagsFields),newMethodAccessFlags);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }

    }
}

