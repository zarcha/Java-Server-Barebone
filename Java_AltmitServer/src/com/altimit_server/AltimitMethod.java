package com.altimit_server;
/**
 * Created by Zach on 10/22/15.
 */

import org.reflections.Reflections;
import org.reflections.scanners.*;
import org.reflections.util.*;

import java.lang.reflect.*;
import java.util.*;

public class AltimitMethod {

    static List<Method> allMethods = null;

    public static void AltimitMethodCompile(){
        allMethods = GetAltimitMethods("com.altimit_server");
    }

    public static void CallAltimitMethod(String methodName, Object... args){
        //List<Method> altimitMethods = GetAltimitMethods("com.altimit_server");
        Object obj = null;

        for(Method m : allMethods){
            if(Objects.equals(m.getName(), methodName)){
                try{
                    Class c = m.getDeclaringClass();
                    obj = c.newInstance();

                    m.invoke(obj, args);
                    return;
                }catch (Exception e){
                    System.out.println(e.toString());
                }
            }
        }
    }

    private static List<Method> GetAltimitMethods(String packageName){
        System.out.println("Compiling list of methods...");
        List<ClassLoader> classLoadersList = new LinkedList<ClassLoader>();
        classLoadersList.add(ClasspathHelper.contextClassLoader());
        classLoadersList.add(ClasspathHelper.staticClassLoader());

        Reflections reflections = new Reflections(new ConfigurationBuilder().setScanners(new SubTypesScanner(false), new ResourcesScanner()).setUrls(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0]))).filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(packageName))));

        List<Method> AltimitCmdMethods = new LinkedList<Method>();

        Object[] allClasses2 = reflections.getSubTypesOf(Object.class).toArray();

        for (Object c : allClasses2) {
            Class curClass = (Class)c;
            Method[] allMethods = curClass.getDeclaredMethods();
            for(Method m : allMethods){
                if(m.isAnnotationPresent(AltimitCmd.class)) {
                    AltimitCmdMethods.add(m);
                }
            }
        }

        return AltimitCmdMethods;
    }
}
