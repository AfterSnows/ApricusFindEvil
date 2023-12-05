package com.aftersnows.filter;

import java.util.ArrayList;
import java.util.List;

import static com.aftersnows.check.CheckUtils.*;
import static com.aftersnows.utils.utils.ConvertClassToArray;

public class filtersFilter {
    public Class<?>[] filterClasses;//用于返回jvm运行的所有filter
    public Class<?>[] MayBFilterClasses;//用于返回可能是内存马的filter
    public filtersFilter(Class<?>[] allClasses){
        List<Class<?>> FilterClassList = new ArrayList<>();
        List<Class<?>> MayBFilterClassList = new ArrayList<>();
        for (Class<?> c : allClasses) {
           if (IsFilter(c)){
               FilterClassList.add(c);
               if(!ClassFileIsExists(c)||IsBadClassLoader(c)){
                   MayBFilterClassList.add(c);
               }
           }
        }
        this.filterClasses=ConvertClassToArray(FilterClassList);
        this.MayBFilterClasses =ConvertClassToArray(MayBFilterClassList);
    }


}
