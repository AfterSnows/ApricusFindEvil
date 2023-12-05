import com.aftersnows.filter.filtersFilter;
import com.aftersnows.transform.KillFilter;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

public class FindEvilAgent {
    public static Instrumentation staticInstrumentation;
    public static Class<?>[] staticClasses;

    private FindEvilAgent() {
        throw new InstantiationError("Don't  instantiate the  class");
    }

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        staticClasses = (Class<?>[]) instrumentation.getAllLoadedClasses();

    }
    public static void agentmain(String agentArgs, Instrumentation instrumentation) throws UnmodifiableClassException {
        staticClasses = (Class<?>[]) instrumentation.getAllLoadedClasses();
        filtersFilter filtersFilter = new filtersFilter(staticClasses);
        for (Class<?> c : filtersFilter.filterClasses) {
            System.out.println("ALL:"+c.getName());
        }
        for (Class<?> c : filtersFilter.MayBFilterClasses) {
            System.out.println("May B: " + c.getName());
//            使用 ClassDumpTransformer 查看经过 KillFilter 之前的类转换
//            ClassDumpTransformer dumpTransformer = new ClassDumpTransformer(c.getName());
//            instrumentation.addTransformer(dumpTransformer, true);
            // 使用 KillFilter 进行类转换
            KillFilter killFilter = new KillFilter(c.getName());
            instrumentation.addTransformer(killFilter, true);
            instrumentation.retransformClasses(c);
            instrumentation.removeTransformer(killFilter);
        }
//        for (Class<?> c : filtersFilter.MayBfilterClasses) {
//            System.out.println("after:");
//            ClassDumpTransformer dumpTransformer = new ClassDumpTransformer(c.getName());
//            instrumentation.addTransformer(dumpTransformer, true);
//            instrumentation.retransformClasses(c);
//            instrumentation.removeTransformer(dumpTransformer);
//
//        }

    }
}
