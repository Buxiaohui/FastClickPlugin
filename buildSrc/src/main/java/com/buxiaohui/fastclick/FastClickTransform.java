package com.buxiaohui.fastclick;

import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import com.android.build.api.transform.Context;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;

public class FastClickTransform extends com.android.build.api.transform.Transform {
    private static final String TAG = "FastClickTransform";

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(Context context, Collection<TransformInput> inputs,
                          Collection<TransformInput> referencedInputs,
                          TransformOutputProvider outputProvider,
                          boolean isIncremental)
            throws IOException, TransformException, InterruptedException {
        super.transform(context, inputs, referencedInputs, outputProvider, isIncremental);
        System.out.println(TAG + "-----transform(Deprecated)------\n");
    }

    @Override
    public void transform(TransformInvocation transformInvocation)
            throws TransformException, InterruptedException, IOException {
        System.out.println(TAG + "-----transform start------\n");
        long startTime = System.currentTimeMillis();
        super.transform(transformInvocation);
        Collection<TransformInput> inputs = transformInvocation.getInputs();
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
        if (outputProvider != null) {
            outputProvider.deleteAll();
        }
        if (inputs != null) {
            for (TransformInput input : inputs) {
                if (input != null) {
                    for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                        handleDirectoryInput(directoryInput, outputProvider);
                    }
                    for (JarInput jarInput : input.getJarInputs()) {
                        // handleJarInputs(jarInput, outputProvider);
                        onlyCopyJar(jarInput, outputProvider);
                    }
                }
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println(TAG + "-----transform end------\n" + "cost->" + (endTime - startTime) + "ms" + "\n");
    }

    private void onlyCopyJar(JarInput jarInput, TransformOutputProvider outputProvider) throws IOException {
        String jarName = jarInput.getName();
        String md5Name = DigestUtils.md5Hex(jarInput.getFile().getAbsolutePath());
        if (jarName.endsWith(".jar")) {
            jarName = jarName.substring(0, jarName.length() - 4);
        }
        File dest = outputProvider.getContentLocation(jarName + md5Name,
                jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
        FileUtils.copyFile(jarInput.getFile(), dest);
    }

    /**
     * 处理Jar中的class文件
     */
    static void handleJarInputs(JarInput jarInput, TransformOutputProvider outputProvider) throws IOException {
        long startTime = System.currentTimeMillis();
        System.out.println(TAG + "----------- handleJarInputs---start-----'");
        if (jarInput != null && jarInput.getFile().getAbsolutePath().endsWith(".jar")) {
            // 重名名输出文件,因为可能同名,会覆盖
            String jarName = jarInput.getName();
            String md5Name = DigestUtils.md5Hex(jarInput.getFile().getAbsolutePath());
            if (jarName.endsWith(".jar")) {
                jarName = jarName.substring(0, jarName.length() - 4);
            }
            JarFile jarFile = new JarFile(jarInput.getFile());
            Enumeration enumeration = jarFile.entries();
            File tmpFile = new File(jarInput.getFile().getParent() + File.separator + "classes_temp.jar");
            // 避免上次的缓存被重复插入
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpFile));
            // 用于保存
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement();
                String entryName = jarEntry.getName();
                ZipEntry zipEntry = new ZipEntry(entryName);
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                // 插桩class
                if (checkClassFile(entryName)) {
                    //class文件处理
                    jarOutputStream.putNextEntry(zipEntry);
                    ClassReader classReader = new ClassReader(IOUtils.toByteArray(inputStream));
                    ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
                    ClassVisitor cv = new FastClickClassVisitor(Opcodes.ASM5, classWriter);
                    classReader.accept(cv, EXPAND_FRAMES);
                    byte[] code = classWriter.toByteArray();
                    jarOutputStream.write(code);
                } else {
                    jarOutputStream.putNextEntry(zipEntry);
                    jarOutputStream.write(IOUtils.toByteArray(inputStream));
                }
                jarOutputStream.closeEntry();
            }
            //结束
            jarOutputStream.close();
            jarFile.close();
            File dest = outputProvider.getContentLocation(jarName + md5Name,
                    jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
            FileUtils.copyFile(tmpFile, dest);
            tmpFile.delete();
        }
        long endTime = System.currentTimeMillis();
        System.out.println(TAG + "-----handleJarInputs end------\n" + "cost->" + (endTime - startTime) + "ms" + "\n");
    }

    /**
     * 检查class文件是否需要处理
     *
     * @param name
     * @return
     */
    private static boolean checkClassFile(String name) {
        System.out.println(TAG + "----------- checkClassFile---name:" + name + "\n");
        // 只处理需要的class文件
        return (name.endsWith(".class") && name.contains("com/buxiaohui")
                        && !name.startsWith("R$")
                        && !"BuildConfig.class".equals(name));
    }

    /**
     * 处理文件目录下的class文件
     */
    private void handleDirectoryInput(DirectoryInput directoryInput, TransformOutputProvider outputProvider)
            throws IOException {
        long startTime = System.currentTimeMillis();
        System.out.println(TAG + "----------- handleDirectoryInput---start-----'");
        // 是否是目录
        if (directoryInput.getFile().isDirectory()) {
            handleDir(directoryInput.getFile());
        }
        // 处理完输入文件之后，要把输出给下一个任务
        File dest = outputProvider.getContentLocation(directoryInput.getName(),
                directoryInput.getContentTypes(), directoryInput.getScopes(),
                Format.DIRECTORY);
        FileUtils.copyDirectory(directoryInput.getFile(), dest);
        long endTime = System.currentTimeMillis();
        System.out.println(
                TAG + "-----handleDirectoryInput end------\n" + "cost->" + (endTime - startTime) + "ms" + "\n");
    }

    /**
     * 遍历目录所有文件（包含子文件夹，子文件夹内文件）
     *
     * @param file1
     * @throws IOException
     */
    private void handleDir(File file1) throws IOException {
        System.out.println(TAG + "-----handleDir------\n"
                + "file1:" + file1
                + "\n");
        for (File file : file1.listFiles()) {
            if (file.isDirectory()) {
                handleDir(file);
            } else {
                String name = file.getName();
                String canonicalPath = file.getCanonicalPath();
                System.out.println(TAG + "-----handleDir------\n"
                        + "file:" + file
                        + "\n"
                        + "file.getCanonicalPath():" + file.getCanonicalPath()
                        + "\n");
                if (checkClassFile(canonicalPath)) {
                    handleFile(file, name);
                }
            }
        }
    }

    private static void handleFile(File file, String name) throws IOException {
        System.out.println(TAG + "-----handleFile------\n"
                + "name:" + name
                + "\n");
        FileInputStream fileInputStream = new FileInputStream(file);
        ClassReader classReader = new ClassReader(fileInputStream);
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new FastClickClassVisitor(Opcodes.ASM5, classWriter);
        classReader.accept(cv, EXPAND_FRAMES);
        byte[] code = classWriter.toByteArray();
        String destPath = file.getParentFile().getAbsolutePath() + File.separator + name;
        FileOutputStream fos = new FileOutputStream(destPath);
        System.out.println(TAG + "-----handleFile------\n"
                + "destPath:" + destPath
                + "\n");
        fos.write(code);
        fos.close();
    }

    @Deprecated
    private void transformDirectory(File input, File dest) {
        System.out.println(TAG + "-----transformDirectory 1------\n"
                + "input:" + input
                + "\n"
                + "dest:" + dest
                + "\n");
        if (dest.exists()) {
            try {
                System.out.println(TAG + "-----transformDirectory del dest: \n"
                        + dest.getAbsolutePath());
                FileUtils.forceDelete(dest);
            } catch (IOException e) {
                System.out.println(TAG + "-----transformDirectory forceDelete e: \n" + e);
            }
        }
        try {
            FileUtils.forceMkdir(dest);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(TAG + "-----transformDirectory forceMkdir e: \n" + e);
        }

        String srcDirPath = input.getAbsolutePath();
        String destDirPath = dest.getAbsolutePath();
        System.out.println(TAG + "-----transformDirectory 2------\n"
                + "srcDirPath:" + srcDirPath
                + "\n"
                + "destDirPath:" + destDirPath
                + "\n");
        File[] files = input.listFiles();
        for (File file : files) {
            String destFilePath = file.getAbsolutePath().replace(srcDirPath, destDirPath);
            File destFile = new File(destFilePath);
            System.out.println(TAG + "-----transformDirectory 3------\n"
                    + destFile
                    + "\n");
            if (file.isDirectory()) {
                transformDirectory(file, destFile);
            } else if (file.isFile()) {
                try {
                    FileUtils.touch(destFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    transformSingleFile(file, destFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Deprecated
    private void transformSingleFile(File input, File dest) throws FileNotFoundException {
        String inputPath = input.getAbsolutePath();
        String outputPath = dest.getAbsolutePath();
        System.out.println(TAG + "-----transformSingleFile------\n"
                + "inputPath:" + inputPath
                + "\n"
                + "outputPath:" + outputPath
                + "\n");
        FileInputStream fileInputStream = new FileInputStream(inputPath);
        FileOutputStream fileOutputStream = new FileOutputStream(outputPath);
        ClassReader classReader = null;
        try {
            classReader = new ClassReader(fileInputStream);
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            ClassVisitor classVisitor = new FastClickClassVisitor(Opcodes.ASM5, classWriter);
            classReader.accept(classVisitor, EXPAND_FRAMES);
            fileOutputStream.write(classWriter.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
