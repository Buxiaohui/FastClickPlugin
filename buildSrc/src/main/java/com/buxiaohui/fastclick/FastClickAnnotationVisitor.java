package com.buxiaohui.fastclick;

import org.objectweb.asm.AnnotationVisitor;

public class FastClickAnnotationVisitor extends AnnotationVisitor {
    public static final String TAG = "FastClickAnnotationVisitor-";
    private String tagVal;
    private long timeInterval;

    public FastClickAnnotationVisitor(int api) {
        super(api);
    }

    public FastClickAnnotationVisitor(int api, AnnotationVisitor annotationVisitor) {
        super(api, annotationVisitor);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String descriptor) {
        System.out.println(TAG + "visitAnnotation,name:" + name + ",descriptor:" + descriptor);
        return super.visitAnnotation(name, descriptor);
    }

    private OnAnnotationVisitListener mOnAnnotationVisitListener;

    public FastClickAnnotationVisitor(int api, AnnotationVisitor av,
                                      OnAnnotationVisitListener mOnAnnotationVisitListener) {
        super(api, av);
        this.mOnAnnotationVisitListener = mOnAnnotationVisitListener;
    }

    public interface OnAnnotationVisitListener {
        void onAnnotationVisitEnd(String tag, long timeInterval);
    }

    @Override
    public void visitEnd() {
        if (mOnAnnotationVisitListener != null) {
            mOnAnnotationVisitListener.onAnnotationVisitEnd(tagVal, timeInterval);
        }
        super.visitEnd();
    }

    @Override
    public void visit(String name, Object value) {
        System.out.println(TAG + "visit,name:" + name + ",value:" + value);
        super.visit(name, value);
        if ("tag".contentEquals(name)) {
            tagVal = (String) value;
        }
        if ("timeInterval".contentEquals(name)) {
            timeInterval = (long) value;
        }
    }

    @Override
    public void visitEnum(String name, String descriptor, String value) {
        System.out.println(TAG + "visitEnum,name:" + name + ",descriptor:" + descriptor + ",value:" + value);
        super.visitEnum(name, descriptor, value);
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        System.out.println(TAG + "visitArray,name:" + name);
        return super.visitArray(name);
    }
}
