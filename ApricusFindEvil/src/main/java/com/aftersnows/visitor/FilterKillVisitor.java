package com.aftersnows.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.objectweb.asm.Opcodes.*;

//todo 完成查杀，对于dofilter方法其实还需要进一步看看比如检测或者查杀之类的
public class FilterKillVisitor extends ClassVisitor {
    public FilterKillVisitor(int i, ClassVisitor classVisitor) {
        super(i, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String descriptor,
                                     String signature, String[] exceptions) {

        // 判断方法的名称是否为doFilter，方法的描述符是否匹配
        MethodVisitor methodVisitor = super.visitMethod(access, methodName, descriptor, signature, exceptions);
        if (methodVisitor != null && methodName.equals("doFilter")) {
            methodVisitor.visitCode();
            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(0, 1);
            methodVisitor.visitEnd();
        }
        return methodVisitor;
}
}


