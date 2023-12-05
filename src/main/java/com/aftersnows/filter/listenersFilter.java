package com.aftersnows.filter;

import java.util.ArrayList;
import java.util.List;

import static com.aftersnows.check.CheckUtils.*;
import static com.aftersnows.utils.utils.ConvertClassToArray;

public class listenersFilter {
    public Class<?>[] listenerClasses;
    public Class<?>[] MayBListenerClasses;
    public listenersFilter(Class<?>[] allClasses){
        List<Class<?>> ListenerClassList = new ArrayList<>();
        List<Class<?>> MayBListenerClassList = new ArrayList<>();
        for (Class<?> c : allClasses) {
            if (IsListener(c)){
                ListenerClassList.add(c);
                if(!ClassFileIsExists(c)||IsBadClassLoader(c)){
                    MayBListenerClassList.add(c);
                }
            }
        }
        this.listenerClasses =ConvertClassToArray(ListenerClassList);
        this.MayBListenerClasses =ConvertClassToArray(MayBListenerClassList);
    }
}
