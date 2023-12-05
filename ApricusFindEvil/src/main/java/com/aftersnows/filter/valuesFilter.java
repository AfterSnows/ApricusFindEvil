package com.aftersnows.filter;

import java.util.ArrayList;
import java.util.List;

import static com.aftersnows.check.CheckUtils.*;
import static com.aftersnows.utils.utils.ConvertClassToArray;

public class valuesFilter {
    public Class<?>[] valuesClasses;
    public Class<?>[] MayBValuesClasses;
    public valuesFilter(Class<?>[] allClasses){
        List<Class<?>> ValueClassList = new ArrayList<>();
        List<Class<?>> MayBValueClassList = new ArrayList<>();
        for (Class<?> c : allClasses) {
            if (IsValve(c)){
                ValueClassList.add(c);
                if(!ClassFileIsExists(c)||IsBadClassLoader(c)){
                    MayBValueClassList.add(c);
                }
            }
        }
        this.valuesClasses =ConvertClassToArray(ValueClassList);
        this.MayBValuesClasses =ConvertClassToArray(MayBValueClassList);
    }
}
