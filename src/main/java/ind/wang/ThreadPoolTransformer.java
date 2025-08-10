package ind.wang;

import javassist.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class ThreadPoolTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        // 检查是否为ThreadPoolExecutor类
        if ("java/util/concurrent/ThreadPoolExecutor".equals(className)) {
            System.out.println("ThreadPoolTransformer: Transforming ThreadPoolExecutor");
            try {
                // 使用Javassist加载类
                ClassPool classPool = ClassPool.getDefault();
                classPool.insertClassPath(new ByteArrayClassPath("java.util.concurrent.ThreadPoolExecutor", classfileBuffer));

                CtClass ctClass = classPool.get("java.util.concurrent.ThreadPoolExecutor");

                // 获取addWorker方法
                CtMethod[] methods = ctClass.getDeclaredMethods();
                CtMethod addWorkerMethod = null;

                // 查找addWorker方法
                for (CtMethod method : methods) {
                    if ("addWorker".equals(method.getName())) {
                        addWorkerMethod = method;
                        break;
                    }
                }

                if (addWorkerMethod != null) {
                    System.out.println("ThreadPoolTransformer: Found addWorker method");
                    // Intercept the return value
                    addWorkerMethod.addLocalVariable("result", classPool.get("java.lang.Object"));
                    addWorkerMethod.insertAfter("""
                            { result = Boolean.valueOf($_); System.out.println("Return value: " + result);\
                             Class aClass = ClassLoader.getSystemClassLoader().loadClass("ind.wang.InvocationRecorder");
                             try {
                                 aClass.getDeclaredMethod("record", new Class[]{java.lang.Object[].class, java.lang.Object.class, java.lang.Throwable.class})\
                                   .invoke(null, new Object[]{$args, result, null});
                             } catch (Exception e) {
                                 e.printStackTrace();
                             } }""");
                    // Handle exceptions
                    addWorkerMethod.addCatch("""
                            { System.out.println("Exception caught: " + $e); \
                             Class aClass = ClassLoader.getSystemClassLoader().loadClass("ind.wang.InvocationRecorder"); \
                             try {
                                 aClass.getDeclaredMethod("record", new Class[]{java.lang.Object[].class, java.lang.Object.class, java.lang.Throwable.class})\
                                   .invoke(null, new Object[]{$args, null, $e});
                             } catch (Exception e) {
                                 e.printStackTrace();
                             }\
                             throw $e; }""", classPool.get("java.lang.Exception"));

                    System.out.println("ThreadPoolTransformer: Added timing code to addWorker method");

                    // 返回修改后的字节码
                    byte[] bytecode = ctClass.toBytecode();
                    ctClass.detach(); // 释放内存
                    return bytecode;
                } else {
                    System.out.println("ThreadPoolTransformer: addWorker method not found");
                }

                ctClass.detach();
            } catch (Exception e) {
                System.err.println("ThreadPoolTransformer: Error transforming class: " + e.getMessage());
                e.printStackTrace();
            }
        }
        // 如果不是目标类，返回原始字节码
        return classfileBuffer;
    }
}
