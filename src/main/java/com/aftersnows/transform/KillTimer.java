package com.aftersnows.transform;

import com.aftersnows.visitor.FilterKillVisitor;
import com.aftersnows.visitor.TimerKillVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;

import static com.aftersnows.utils.utils.CanonicalClassNameToPoint;

public class KillTimer implements ClassFileTransformer{
    private final String TargetClass;
    public KillTimer(String targetClass) {
        TargetClass = targetClass;
    }
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        className = CanonicalClassNameToPoint(className);
        if(className.equals(TargetClass)){
            System.out.println("scalpel  on  Target: " + className);
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new TimerKillVisitor(Opcodes.ASM9, cw);
            int parsingOptions = ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES;
            cr.accept(cv, parsingOptions);
            return cw.toByteArray();
        }
        return new byte[0];
    }
    public static void cancelGCDaemon(Thread gcDaemonThread) {
        try {
            // 使用反射获取 Thread 类的 daemon 字段
            Field daemonField = Thread.class.getDeclaredField("daemon");
            daemonField.setAccessible(true);

            // 反射取消 GC Daemon 2 线程的守护进程属性
            daemonField.setBoolean(gcDaemonThread, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
