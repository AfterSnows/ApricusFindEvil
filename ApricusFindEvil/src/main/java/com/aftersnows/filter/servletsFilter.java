package com.aftersnows.filter;

import java.util.ArrayList;
import java.util.List;

import static com.aftersnows.check.CheckUtils.*;
import static com.aftersnows.utils.utils.ConvertClassToArray;

public class servletsFilter {
    public Class<?>[] servletClasses;
    public Class<?>[] MayBServletClasses;
    public servletsFilter(Class<?>[] allClasses){
        List<Class<?>> ServletClassList = new ArrayList<>();
        List<Class<?>> MayBServletClassList = new ArrayList<>();
        for (Class<?> c : allClasses) {
            if (IsServlet(c)){
                ServletClassList.add(c);
                if(!ClassFileIsExists(c)||IsBadClassLoader(c)){
                    MayBServletClassList.add(c);
                }
            }
        }
        this.servletClasses =ConvertClassToArray(ServletClassList);
        this.MayBServletClasses =ConvertClassToArray(MayBServletClassList);
    }
}
