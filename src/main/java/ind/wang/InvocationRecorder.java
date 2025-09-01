package ind.wang;

import java.util.*;

public class InvocationRecorder {
    private static final Map<String, List<List<Object>>> PARAMS = new HashMap<>();
    private static final Map<String, List<Object>> RETURN_VALUES = new HashMap<>();
    private static final Map<String, List<Throwable>> EXCEPTIONS = new HashMap<>();

    public static void record(String methodSignature, Object[] params, Object returnValue, Throwable exception) {
        PARAMS.putIfAbsent(methodSignature, new ArrayList<>());
        PARAMS.get(methodSignature).add(Arrays.stream(params).toList());
        RETURN_VALUES.putIfAbsent(methodSignature, new ArrayList<>());
        RETURN_VALUES.get(methodSignature).add(returnValue);
        EXCEPTIONS.putIfAbsent(methodSignature, new ArrayList<>());
        EXCEPTIONS.get(methodSignature).add(exception);
    }

    public static void reset() {
        PARAMS.clear();
        RETURN_VALUES.clear();
        EXCEPTIONS.clear();
    }

    public static int getInvocations(String methodSignature) {
        return PARAMS.get(methodSignature).size();
    }

    public static List<Object> getParams(String methodSignature, int i) {
        List<List<Object>> list = PARAMS.getOrDefault(methodSignature, new ArrayList<>());
        return i < list.size() ? list.get(i) : null;
    }

    public static Object getReturnValue(String methodSignature, int i) {
        List<Object> list = RETURN_VALUES.getOrDefault(methodSignature, new ArrayList<>());
        return i < list.size() ? list.get(i) : null;
    }

    public static Throwable getException(String methodSignature, int i) {
        List<Throwable> list = EXCEPTIONS.getOrDefault(methodSignature, new ArrayList<>());
        return i < list.size() ? list.get(i) : null;
    }

}
