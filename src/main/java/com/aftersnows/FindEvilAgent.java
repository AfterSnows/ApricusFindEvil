package com.aftersnows;
import com.aftersnows.JVM.Defense.HTTPServiceDEF;
import com.aftersnows.JVM.Defense.agentDEF;
import com.aftersnows.filter.*;
import com.aftersnows.transform.*;

import java.lang.instrument.Instrumentation;
import static com.aftersnows.check.CheckUtils.IsRunnable;

public class FindEvilAgent {
    public static Instrumentation staticInstrumentation;
    public static Class<?>[] staticClasses;

    private FindEvilAgent() {
        throw new InstantiationError("Don't  instantiate the  class");
    }

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        staticClasses = (Class<?>[]) instrumentation.getAllLoadedClasses();
    }

    public static void agentmain(String agentArgs, Instrumentation instrumentation) throws Exception {
        staticClasses = (Class<?>[]) instrumentation.getAllLoadedClasses();
        filtersFilter filtersFilter = new filtersFilter(staticClasses);
        WebsocketsFilter websocketsFilter = new WebsocketsFilter(staticClasses);
        listenersFilter listenersFilter=new listenersFilter(staticClasses);
        valuesFilter valuesFilter=new valuesFilter(staticClasses);
        servletsFilter servletsFilter=new servletsFilter(staticClasses);

        TimerFilter timerFilter= new TimerFilter();
        System.out.println("Agent start...");
        new HTTPServiceDEF(true);
        new agentDEF();


        //KillWebsocket
        for (Class<?> c : websocketsFilter.MayBWebsocketClasses) {
            System.out.println("May Bad websocket: " + c.getName());
            KillWebsocket killWebsocket = new KillWebsocket(c.getName());
            ClassDumpTransformer dumpTransformer = new ClassDumpTransformer(c.getName());
            instrumentation.addTransformer(killWebsocket, true);
            System.out.println("Kill ing");
            instrumentation.addTransformer(dumpTransformer, true);
            instrumentation.retransformClasses(c);
        }

        //KillListener
        for (Class<?> c : listenersFilter.MayBListenerClasses) {
            System.out.println("May Bad websocket: " + c.getName());
            KillListener killListener=new KillListener(c.getName());
            ClassDumpTransformer dumpTransformer = new ClassDumpTransformer(c.getName());
            instrumentation.addTransformer(killListener, true);
            System.out.println("Kill ing");
            instrumentation.addTransformer(dumpTransformer, true);
            instrumentation.retransformClasses(c);
        }

        //KillValue
        for (Class<?> c : valuesFilter.MayBValuesClasses) {
            System.out.println("May Bad websocket: " + c.getName());
            KillValue KillValue=new KillValue(c.getName());
            ClassDumpTransformer dumpTransformer = new ClassDumpTransformer(c.getName());
            instrumentation.addTransformer(KillValue, true);
            System.out.println("Kill ing");
            instrumentation.addTransformer(dumpTransformer, true);
            instrumentation.retransformClasses(c);
        }

        //KillServlet
        for (Class<?> c : servletsFilter.MayBServletClasses) {
            System.out.println("May Bad websocket: " + c.getName());
            KillServlet KillServlet=new KillServlet(c.getName());
            ClassDumpTransformer dumpTransformer = new ClassDumpTransformer(c.getName());
            instrumentation.addTransformer(KillServlet, true);
            System.out.println("Kill ing");
            instrumentation.addTransformer(dumpTransformer, true);
            instrumentation.retransformClasses(c);
        }

        //KillFilter
        for (Class<?> c : filtersFilter.MayBFilterClasses) {
            System.out.println("May Bad filter: " + c.getName());
            KillFilter killFilter = new KillFilter(c.getName());
            ClassDumpTransformer dumpTransformer = new ClassDumpTransformer(c.getName());
            instrumentation.addTransformer(killFilter, true);
            System.out.println("Kill ing");
            instrumentation.addTransformer(dumpTransformer, true);
            instrumentation.retransformClasses(c);
        }


        //KillTimer
        for (Class<?> c : timerFilter.MayBTimerClasses) {
            System.out.println("May Bad Timer: " + c.getName());
            if(IsRunnable(c)){
                KillTimer killTimer=new KillTimer(c.getName());
                instrumentation.addTransformer(killTimer, true);
            }
            ClassDumpTransformer dumpTransformer = new ClassDumpTransformer(c.getName());
            instrumentation.addTransformer(dumpTransformer, true);
            instrumentation.retransformClasses(c);
        }


    }
}
