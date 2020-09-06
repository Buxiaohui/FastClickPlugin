package com.buxiaohui.fastclick;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class FastClickClassVisitor extends ClassVisitor {
    public static final String TAG = "FastClickClassVisitor-";

    public FastClickClassVisitor(int api) {
        super(api);
    }

    public FastClickClassVisitor(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                     String[] exceptions) {
        System.out.println(TAG + "visitMethod,access:" + access
                +"\n"
                + ",descriptor:" + descriptor
                +"\n"
                + ",name:" + name
                +"\n"
                + ",signature:" + signature
                +"\n"
                + ",exceptions:" + exceptions);
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new FastClickMethodVisitor(api, mv, access, name, descriptor);

    }

}
