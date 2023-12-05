package com.aftersnows.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.RETURN;

public class ListenerKillVisitor extends ClassVisitor {

    public ListenerKillVisitor(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
    }
    @Override
    public MethodVisitor visitMethod(int access, String methodName, String descriptor,
                                     String signature, String[] exceptions) {

        // 判断方法的名称是否为doFilter，方法的描述符是否匹配
        MethodVisitor methodVisitor = super.visitMethod(access, methodName, descriptor, signature, exceptions);
        if (methodVisitor != null && methodName.equals("requestDestroyed")||methodName.equals("requestInitialized")) {
            methodVisitor.visitCode();
            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(0, 2);
            methodVisitor.visitEnd();
        }
        return methodVisitor;
    }
}
