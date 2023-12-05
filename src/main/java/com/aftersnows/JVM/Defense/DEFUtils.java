package com.aftersnows.JVM.Defense;

import java.io.IOException;

public class DEFUtils {
    public static String getProperty(String key) {
        return "DEF VALID";
    }

    public static void attach(int i) {
        System.out.println("Windbg HOOK");
    }


}
