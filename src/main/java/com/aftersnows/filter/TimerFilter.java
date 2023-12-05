package com.aftersnows.filter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static com.aftersnows.check.CheckUtils.ClassFileIsExists;
import static com.aftersnows.check.CheckUtils.IsBadClassLoader;
import static com.aftersnows.utils.utils.*;


public class TimerFilter {
    public Class<?>[] MayBTimerClasses;
    public Thread[] EvilTread;
    public TimerFilter() throws Exception {
        List<Class<?>> MayTimerClasses = new ArrayList<>();
        List<Thread> MayTimerThread = new ArrayList<>();

        Thread[] threads = (Thread[]) ((Thread[]) getField(getSystemThreadGroup(), "threads"));
        Object target;
        for (Thread nowThread : threads) {
            if(nowThread==null){
                continue;
            }
            target = getField(nowThread, "target");
            if (target == null) {
                continue;
            } else {
                //确认thread的runnable是否有问题,直接给它封掉
                if (!ClassFileIsExists(target.getClass()) || IsBadClassLoader(target.getClass())) {
                    String nowThreadName=nowThread.getName();
                    System.out.println("存在恶意线程: "+nowThreadName);
                    MayTimerClasses.add(target.getClass());
                    MayTimerThread.add(nowThread);
                    nowThread.stop();
                    System.out.println("已停止恶意线程: "+nowThreadName+",需要进一步确认生成此线程的内存马");
                }
            }
        }
        this.MayBTimerClasses =ConvertClassToArray(MayTimerClasses);
        this.EvilTread=ConvertThreadToArray(MayTimerThread);

    }
}
