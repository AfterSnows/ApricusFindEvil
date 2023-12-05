package com.aftersnows.Agent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static com.aftersnows.check.CheckUtils.*;
import static com.aftersnows.utils.utils.ConvertClassToArray;

public class socketFilter {
    public Class<?>[] socketClasses;

    public socketFilter(Class<?>[] allClasses) {
        List<Class<?>> socketClasses = new ArrayList<>();
        for (Class<?> c : allClasses) {
            if ("sun.nio.ch.SocketChannelImpl".equals(c.getName())){
                socketClasses.add(c);
                if(!ClassFileIsExists(c)||IsBadClassLoader(c)){
                    socketClasses.add(c);
                }
            }
        }
        this.socketClasses=ConvertClassToArray(socketClasses);
    }
}


