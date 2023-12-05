package com.aftersnows.transform;

import com.aftersnows.visitor.ListenerKillVisitor;
import com.aftersnows.visitor.ValueKillVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import static com.aftersnows.utils.utils.CanonicalClassNameToPoint;

public class KillListener implements ClassFileTransformer {
    private final String TargetClass;
    public byte[] data;
    public KillListener(String targetClass) {
        TargetClass = targetClass;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)  {
        className = CanonicalClassNameToPoint(className);
        if(className.equals(TargetClass)){
            System.out.println("scalpel  on  Target: " + className);
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new ListenerKillVisitor(Opcodes.ASM9, cw);
            int parsingOptions = ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES;
            cr.accept(cv, parsingOptions);
            return cw.toByteArray();

        }
        return new byte[0];
    }
}
