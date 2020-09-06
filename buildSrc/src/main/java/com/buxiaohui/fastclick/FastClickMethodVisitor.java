/*
 * Copyright (C) 2020 Baidu, Inc. All Rights Reserved.
 */
package com.buxiaohui.fastclick;/*
 * Copyright (C) 2020 Baidu, Inc. All Rights Reserved.
 */

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

public class FastClickMethodVisitor extends AdviceAdapter {
    public static final String TAG = "FastClickMethodVisitor-";
    private boolean mIsFDCAnnotation;
    private String tagVal;
    private long timeInterval;

    public FastClickMethodVisitor(int api, MethodVisitor methodVisitor, int access,
                                  String name, String descriptor) {
        super(api, methodVisitor, access, name, descriptor);
        System.out.println(TAG + "construct method");
    }

    @Override
    public void visitCode() {
        super.visitCode();
    }

    @Override
    protected void onMethodEnter() {
        super.onMethodEnter();
        System.out.println(
                TAG + "onMethodEnter,methodDesc:" + methodDesc
                        + "\n" + "mIsFDCAnnotation:" + mIsFDCAnnotation
                        + "\n" + "tagVal:" + tagVal
                        + "\n" + "timeInterval:" + timeInterval
                        + "\n");
        if (mIsFDCAnnotation) {
            if (isEmpty(tagVal)) {
                if (timeInterval <= 0) {
                    // isFastClick();
                    mv.visitMethodInsn(INVOKESTATIC, METHOD_OWNER, METHOD_NAME,
                            "()Z", false);
                } else {

                    // isFastClick(long timeInterval);
                    Label l1 = new Label();
                    mv.visitJumpInsn(IFEQ, l1);
                    mv.visitLdcInsn(new Long(timeInterval));
                    mv.visitMethodInsn(INVOKESTATIC, METHOD_OWNER, METHOD_NAME, "(J)Z", false);
                    mv.visitInsn(RETURN);
                }
            } else {
                if (timeInterval <= 0) {
                    // isFastClick(String tagVal);
                    mv.visitLdcInsn(tagVal);
                    mv.visitMethodInsn(INVOKESTATIC, METHOD_OWNER, METHOD_NAME,
                            "(Ljava/lang/String;)Z", false);

                } else {
                    // isFastClick(String tagVal, long timeInterval);
                    Label l0 = new Label();
                    mv.visitLabel(l0);
                    mv.visitLdcInsn(tagVal);
                    mv.visitLdcInsn(new Long(timeInterval));
                    mv.visitMethodInsn(INVOKESTATIC, METHOD_OWNER, METHOD_NAME, "(Ljava/lang/String;J)Z", false);
                    Label l1 = new Label();
                    mv.visitJumpInsn(IFEQ, l1);
                    Label l2 = new Label();
                    mv.visitLabel(l2);
                    mv.visitInsn(RETURN);
                    mv.visitLabel(l1);
                }
            }
        }
    }

    private final static String METHOD_OWNER = "com/buxiaohui/fastclickplugin/fdc/FastClickUtils";
    private final static String METHOD_NAME = "isFastClick";

    private static boolean isEmpty(String str) {
        return str == null || str.equals("");
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        return super.visitParameterAnnotation(parameter, desc, visible);
    }

    @Override
    public void visitAttribute(Attribute attr) {
        super.visitAttribute(attr);
    }

    @Override
    protected void onMethodExit(int opcode) {
        super.onMethodExit(opcode);
        System.out.println(TAG + "onMethodExit,methodDesc:" + methodDesc + ",opcode:" + opcode);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        AnnotationVisitor annotationVisitor = mv.visitAnnotation(descriptor, visible);
        System.out.println(TAG + "visitAnnotation,descriptor:" + descriptor + ",visible:" + visible);
        System.out.println(TAG + "visitAnnotation,annotationVisitor:" + annotationVisitor);
        if (descriptor.contains("Lcom/buxiaohui/annotation/FDC")) {
            FastClickAnnotationVisitor fastClickAnnotationVisitor = new FastClickAnnotationVisitor(api,
                    annotationVisitor, new FastClickAnnotationVisitor.OnAnnotationVisitListener() {
                @Override
                public void onAnnotationVisitEnd(String tagVal, long timeInterval) {
                    System.out.println(TAG + "visitAnnotation,onAnnotationVisitEnd,tagVal:" + tagVal
                            + "\n"
                            + ",timeInterval:" + timeInterval
                            + "\n");
                    FastClickMethodVisitor.this.tagVal = tagVal;
                    FastClickMethodVisitor.this.timeInterval = timeInterval;
                }
            });
            mIsFDCAnnotation = true;
            return fastClickAnnotationVisitor;
        } else {
            FastClickMethodVisitor.this.tagVal = "默认空";
            FastClickMethodVisitor.this.timeInterval = 300L;
            mIsFDCAnnotation = false;
            return annotationVisitor;
        }
    }
}
