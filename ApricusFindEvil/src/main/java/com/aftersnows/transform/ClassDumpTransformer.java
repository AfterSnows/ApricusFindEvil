package com.aftersnows.transform;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Base64;

import static com.aftersnows.utils.utils.CanonicalClassNameToPoint;


public class ClassDumpTransformer implements ClassFileTransformer {
    public String Base64ClassByte;
    public String HexClassByte;
    private final String TargetClass;

    public ClassDumpTransformer(String targetClass) {
        TargetClass = targetClass;
    }


    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        className = CanonicalClassNameToPoint(className);
        if(className.equals(TargetClass)){
            System.out.println("Dumping class: " + className);
            this.Base64ClassByte=Base64.getEncoder().encodeToString(classfileBuffer);
            System.out.println(Base64ClassByte);
        }
        return classfileBuffer;
    }



}
