package ind.wang;

import java.lang.instrument.Instrumentation;

public class ThreadPoolAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new ThreadPoolTransformer());
    }
}
