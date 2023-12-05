package com.aftersnows.transform;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;


public class socketVim implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            if (className.equals("sun/nio/ch/SocketChannelImpl")) {
                Class<?> socketChannelImplClass = Class.forName("sun.nio.ch.SocketChannelImpl");
                Field fdField = socketChannelImplClass.getDeclaredField("fd");
                // 设置私有字段 fd 可访问性
                fdField.setAccessible(true);
                return new ClassAdapter(classfileBuffer, fdField).getBytes();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    static class ClassAdapter {
        private final byte[] classBytes;
        private final Field fdField;

        ClassAdapter(byte[] classBytes, Field fdField) {
            this.classBytes = classBytes;
            this.fdField = fdField;
        }

        byte[] getBytes() throws Exception {
            // 使用字节码操作库修改字节码
            // 在 SocketChannelImpl.read() 方法中插入代码获取地址信息
            // 以下示例代码仅用于演示，实际应根据需求进行修改
            // 在这里将地址信息直接打印到控制台
            // 例如：System.out.println("Socket Address: " + socketAddress);

            // 在 SocketChannelImpl.read() 方法返回之前，插入获取地址信息的处理代码
            String getCode = "java.nio.channels.SocketChannel channel = (java.nio.channels.SocketChannel) this;\n" +
                    "java.net.Socket socket = channel.socket();\n" +
                    "java.net.InetSocketAddress address = (java.net.InetSocketAddress) socket.getRemoteSocketAddress();\n" +
                    "if (address != null) {\n" +
                    "    String socketAddress = address.getAddress().getHostAddress() + ':' + address.getPort();\n" +
                    "    System.out.println(\"Socket Address: \" + socketAddress);\n" +
                    "}";

            int getCodeIndex = -1;

            // 查找 SocketChannelImpl.read() 方法对应的字节码位置
            for (int i = 0; i < classBytes.length - 4; i++) {
                if (classBytes[i] == (byte) 0x2a && classBytes[i+1] == (byte) 0xb7 && classBytes[i+3] == (byte) 0xc4) {
                    getCodeIndex = i;
                    break;
                }
            }

            if (getCodeIndex < 0) {
                throw new IllegalStateException("Failed to find SocketChannelImpl.read() method");
            }

            // 将插入的代码转成字节数组
            byte[] getCodeBytes = getCode.getBytes();

            // 创建新的字节数组，包括原始字节码和插入的代码
            byte[] newClassBytes = new byte[classBytes.length + getCodeBytes.length];
            System.arraycopy(classBytes, 0, newClassBytes, 0, getCodeIndex);
            System.arraycopy(getCodeBytes, 0, newClassBytes, getCodeIndex, getCodeBytes.length);
            System.arraycopy(classBytes, getCodeIndex, newClassBytes, getCodeIndex + getCodeBytes.length, classBytes.length - getCodeIndex);

            return newClassBytes;
        }
    }


}
