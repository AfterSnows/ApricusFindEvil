package com.aftersnows.filter;

import java.util.ArrayList;
import java.util.List;

import static com.aftersnows.check.CheckUtils.*;
import static com.aftersnows.utils.utils.ConvertClassToArray;

public class WebsocketsFilter {
    public Class<?>[] websocketClasses;//用于返回jvm运行的所有filter
    public Class<?>[] MayBWebsocketClasses;//用于返回可能是内存马的filter
    public WebsocketsFilter(Class<?>[] allClasses){
        List<Class<?>> FilterClassList = new ArrayList<>();
        List<Class<?>> MayBFilterClassList = new ArrayList<>();
        for (Class<?> c : allClasses) {
            if (IsWebsocket(c)){
                FilterClassList.add(c);
                if(!ClassFileIsExists(c)||IsBadClassLoader(c)){
                    MayBFilterClassList.add(c);
                }
            }
        }
        this.websocketClasses =ConvertClassToArray(FilterClassList);
        this.MayBWebsocketClasses =ConvertClassToArray(MayBFilterClassList);
    }
}
