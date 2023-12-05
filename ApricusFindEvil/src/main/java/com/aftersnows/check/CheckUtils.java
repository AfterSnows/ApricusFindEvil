package com.aftersnows.check;

import java.net.URL;

import static com.aftersnows.utils.utils.CanonicalClassNameToBackslash;
import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;

public class CheckUtils {
    public static boolean ClassFileIsExists(Class clazz){
        if(clazz == null){
            log.println("class is null");
            return false;
        }
        String className = clazz.getName();
        String classNamePath =  CanonicalClassNameToBackslash(className) + ".class";
        Object clazzLoader = clazz.getClassLoader();
        URL is;
        if (clazzLoader==null){
            is = ClassLoader.getSystemClassLoader().getResource(classNamePath);
            return true;
        }else{
            is = clazz.getClassLoader().getResource(classNamePath);
        }
        if(is == null){
            return false;
        }else{
            return true;
        }

    }
    public static boolean IsBadClassLoader(Class targetClass){
        ClassLoader classLoader = null;
        if(targetClass.getClassLoader() != null) {
            classLoader = targetClass.getClassLoader();
        }else{
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        if(classLoader.getClass().getName().contains("com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl$TransletClassLoader")
                    || classLoader.getClass().getName().contains("com.sun.org.apache.bcel.internal.util.ClassLoader")){
                return true;
            }
        return false;
    }

    public static boolean IsFilter(Class targetClass){
        ClassLoader classLoader = null;
        if(targetClass.getClassLoader() != null) {
            classLoader = targetClass.getClassLoader();
        }else{
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        Class clsFilter =  null;
        try {
            clsFilter = classLoader.loadClass("javax.servlet.Filter");
        }catch (Exception e){
        }

        if(clsFilter != null && clsFilter.isAssignableFrom(targetClass)){
            return true;
        }else{
            return false;
        }
    }
    public static boolean IsRunnable(Class targetClass){
        ClassLoader classLoader = null;
        if(targetClass.getClassLoader() != null) {
            classLoader = targetClass.getClassLoader();
        }else{
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        Class clsFilter =  null;
        try {
            clsFilter = classLoader.loadClass("java.lang.Runnable");
        }catch (Exception e){
        }

        if(clsFilter != null && clsFilter.isAssignableFrom(targetClass)){
            return true;
        }else{
            return false;
        }
    }


    public static boolean IsServlet(Class targetClass){
        ClassLoader classLoader = null;
        if(targetClass.getClassLoader() != null) {
            classLoader = targetClass.getClassLoader();
        }else{
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        Class clsFilter =  null;
        try {
            clsFilter = classLoader.loadClass("javax.servlet.Servlet");
        }catch (Exception e){
        }

        if(clsFilter != null && clsFilter.isAssignableFrom(targetClass)){
            return true;
        }else{
            return false;
        }
    }

    public static boolean IsListener(Class targetClass){
        ClassLoader classLoader = null;
        if(targetClass.getClassLoader() != null) {
            classLoader = targetClass.getClassLoader();
        }else{
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        Class clsFilter =  null;
        try {
            clsFilter = classLoader.loadClass("javax.servlet.ServletRequestListener");
        }catch (Exception e){
        }

        if(clsFilter != null && clsFilter.isAssignableFrom(targetClass)){
            return true;
        }else{
            return false;
        }
    }
    public static boolean IsValve(Class targetClass){
        ClassLoader classLoader = null;
        if(targetClass.getClassLoader() != null) {
            classLoader = targetClass.getClassLoader();
        }else{
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        Class clsFilter =  null;
        try {
            clsFilter = classLoader.loadClass("org.apache.catalina.Valve");
        }catch (Exception e){
        }

        if(clsFilter != null && clsFilter.isAssignableFrom(targetClass)){
            return true;
        }else{
            return false;
        }
    }
    public static boolean IsWebsocket(Class targetClass){
        ClassLoader classLoader = null;
        if(targetClass.getClassLoader() != null) {
            classLoader = targetClass.getClassLoader();
        }else{
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        Class clsFilter =  null;
        try {
            clsFilter = classLoader.loadClass("javax.websocket.Endpoint");
        }catch (Exception e){
        }

        if(clsFilter != null && clsFilter.isAssignableFrom(targetClass)){
            return true;
        }else{
            return false;
        }
    }

}
