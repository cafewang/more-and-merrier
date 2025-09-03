package ind.wang;

import javassist.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Set;

public class ThreadPoolTransformer implements ClassFileTransformer {
    private static final Map<String, Set<String>> KLASS_TO_METHODS = Map.of(
            "java.util.concurrent.ThreadPoolExecutor", Set.of("addWorker", "processWorkerExit"),
           "java.util.concurrent.ThreadPoolExecutor$Worker", Set.of("run")
    );

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        String klassName = className.replaceAll("/", ".");
        // 检查是否为ThreadPoolExecutor类
        if (KLASS_TO_METHODS.containsKey(klassName)) {
            try {
                // 使用Javassist加载类
                ClassPool classPool = ClassPool.getDefault();
                classPool.insertClassPath(new ByteArrayClassPath(klassName, classfileBuffer));

                CtClass ctClass = classPool.get(klassName);

                // 获取目标方法
                CtMethod[] methods = ctClass.getDeclaredMethods();
                boolean modified = false;
                // 查找addWorker方法
                for (CtMethod method : methods) {
                    if (!KLASS_TO_METHODS.get(klassName).contains(method.getName())) {
                        continue;
                    }
                    modified = true;
                    // Intercept the return value
                    method.addLocalVariable("result", classPool.get("java.lang.Object"));
                    method.insertAfter("""
                        {result = ($w)$_; \
                         Class aClass = ClassLoader.getSystemClassLoader().loadClass("ind.wang.InvocationRecorder");
                         try {
                             aClass.getDeclaredMethod("record", new Class[]{java.lang.String.class, java.lang.Object[].class, java.lang.Object.class, java.lang.Throwable.class})\
                               .invoke(null, new Object[]{"%s", $args, result, null});
                         } catch (Exception e) {
                             e.printStackTrace();
                         } }""".formatted(method.getName()));
                    // Handle exceptions
                    method.addCatch("""
                            { Class aClass = ClassLoader.getSystemClassLoader().loadClass("ind.wang.InvocationRecorder"); \
                             try {
                                 aClass.getDeclaredMethod("record", new Class[]{java.lang.String.class, java.lang.Object[].class, java.lang.Object.class, java.lang.Throwable.class})\
                                   .invoke(null, new Object[]{"%s", $args, null, $e});
                             } catch (Exception e) {
                                 e.printStackTrace();
                             }\
                             throw $e; }""".formatted(method.getName()), classPool.get("java.lang.Exception"));
                }

                if (modified) {
                    // 返回修改后的字节码
                    byte[] bytecode = ctClass.toBytecode();
                    ctClass.detach(); // 释放内存
                    return bytecode;
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
