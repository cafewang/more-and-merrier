package ind.wang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InvocationRecorder {
    private static final List<List<Object>> PARAMS = new ArrayList<>();
    private static final List<Object> RETURN_VALUES = new ArrayList<>();
    private static final List<Throwable> EXCEPTIONS = new ArrayList<>();

    public static void record(Object[] params, Object returnValue, Throwable exception) {
        PARAMS.add(Arrays.stream(params).toList());
        RETURN_VALUES.add(returnValue);
        EXCEPTIONS.add(exception);
    }

    public static void reset() {
        PARAMS.clear();
        RETURN_VALUES.clear();
        EXCEPTIONS.clear();
    }

    public static int getInvocations() {
        return PARAMS.size();
    }

    public static List<Object> getParams(int i) {
        return i < PARAMS.size() ? PARAMS.get(i) : null;
    }

    public static Object getReturnValue(int i) {
        return i < RETURN_VALUES.size() ? RETURN_VALUES.get(i) : null;
    }

    public static Throwable getException(int i) {
        return i < EXCEPTIONS.size() ? EXCEPTIONS.get(i) : null;
    }

}
