package com.aftersnows.transform;

import com.aftersnows.visitor.FilterKillVisitor;
import org.objectweb.asm.*;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import static com.aftersnows.utils.utils.*;

public class KillFilter implements ClassFileTransformer {
    private final String TargetClass;
    public byte[] data;

    public KillFilter(String targetClass) {
        TargetClass = targetClass;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)  {
        className = CanonicalClassNameToPoint(className);
        if(className.equals(TargetClass)){
            System.out.println("scalpel  on  Target: " + className);
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new FilterKillVisitor(Opcodes.ASM9, cw);
            int parsingOptions = ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES;
            cr.accept(cv, parsingOptions);
            return cw.toByteArray();

        }
        return new byte[0];
    }

}
