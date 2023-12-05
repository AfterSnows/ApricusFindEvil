package com.aftersnows.JVM;

import com.aftersnows.JVM.Defense.agentDEF;
import sun.misc.Unsafe;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;


public class HotSpot {
    static final String[] libPaths = {"/lib/server/","/lib/client/","/lib/amd64/server/","/lib/amd64/client/","/lib/i386/server/","/lib/i386/client/","/bin/server/","/bin/client/","/lib/sparcv9/server/","/lib/sparcv9/client/","/lib/sparc/client/","/lib/sparc/server/","/lib/ia64/server/","/lib/ia64/client/"};
    static final String[] libNames = {"jvm.dll","libjvm.so","libjvm.dylib"};
    static Class NativeLibraryClass;
    public static Unsafe unsafe;
    static Object oldModule;
    static HashMap symEntryCacheMap = new HashMap();
    static LinkedHashMap HotSpotTypeFields = new LinkedHashMap();
    static LinkedHashMap HotSpotTypes = new LinkedHashMap();
    static LinkedHashMap HotSpotVMIntConstants = new LinkedHashMap();
    static LinkedHashMap HotSpotVMLongConstants = new LinkedHashMap();
    static LinkedHashMap HotSpotVMFlags = new LinkedHashMap();
    static final Class currentClass = HotSpot.class;
    static Object NativeLibraryObject;
    static Method findEntryMethod;
    static short javaVersion;
    static long HeapWordSize;
    static long narrowOopBase;
    static long narrowOopShift;
    static String InstanceKlassTypeName;
    static long MethodSize;


    Object _calcObjectAddress;

    static {
        initialize();
    }

    public HotSpot(){

    }

    public static void initialize(){
        if (initializeContext()){
            readHotSpotVMIntConstants();
            readgHotSpotVMLongConstants();
            readHotSpotVMStructEntrys();
            readHotSpotVMTypeEntrys();
            javaVersion = (short) (Double.parseDouble(System.getProperty("java.class.version")) - 44);
            HeapWordSize = lookupIntConstant("HeapWordSize");
            narrowOopBase = 0;
            narrowOopShift = 0;
            try {
                if (hasType("CompressedOops")){
                    narrowOopBase = unsafe.getAddress(getFieldAddress("CompressedOops","_narrow_oop._base"));
                    narrowOopShift = unsafe.getAddress(getFieldAddress("CompressedOops","_narrow_oop._shift"));
                }else{
                    narrowOopBase = unsafe.getAddress(getFieldAddress("Universe","_narrow_oop._base"));
                    narrowOopShift = unsafe.getAddress(getFieldAddress("Universe","_narrow_oop._shift"));
                }
            }catch (Exception e){

            }
            readHotSpotFlags();
            InstanceKlassTypeName = hasType("InstanceKlass")?"InstanceKlass":"instanceKlass";
            MethodSize = hasType("methodOopDesc") ? sizeOf("methodOopDesc") : sizeOf("Method");

        }
    }
    public static boolean initializeContext(){
        PrintStream printStream = new PrintStream(new ByteArrayOutputStream());
        PrintStream sysOut = System.out;
        System.setOut(printStream);
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe = (Unsafe) unsafeField.get(null);
            try {
                // java 5-14
                NativeLibraryClass = Class.forName("java.lang.ClassLoader$NativeLibrary");
                Object[] createInstanceArgs = new Object[2];
                Object[] loadMethodArgs = new Object[1];
                Method loadMethod = null;
                Constructor constructor = null;

                constructor = getConstructor(NativeLibraryClass,new Class[]{Class.class,String.class});

                if (constructor == null){
                    constructor = getConstructor(NativeLibraryClass,new Class[]{Class.class,String.class,boolean.class});
                    createInstanceArgs = new Object[3];
                    createInstanceArgs[2] = false;
                }
                // find load method
                loadMethod = getMethod(NativeLibraryClass,"load",new Class[]{String.class});
                if (loadMethod == null){
                    loadMethod = getMethod(NativeLibraryClass,"load",new Class[]{String.class,boolean.class});
                    loadMethodArgs = new Object[2];
                    loadMethodArgs[1] = false;

                    if (loadMethod == null){
                        loadMethod = getMethod(NativeLibraryClass,"load",new Class[]{String.class,boolean.class,boolean.class});
                        loadMethodArgs = new Object[3];
                        loadMethodArgs[1] = false;
                        loadMethodArgs[2] = true;
                    }

                }
                if (loadMethod == null){
                    loadMethod = getMethod(NativeLibraryClass,"load0",new Class[]{String.class});
                }
                if (loadMethod == null){
                    loadMethod = getMethod(NativeLibraryClass,"load0",new Class[]{String.class,boolean.class});
                    loadMethodArgs = new Object[2];
                    loadMethodArgs[1] = false;
                }

                // find findEntry method
                findEntryMethod = getMethod(NativeLibraryClass,"findEntry",new Class[]{String.class});
                if (findEntryMethod == null){
                    findEntryMethod = getMethod(NativeLibraryClass,"find",new Class[]{String.class});
                }

                LinkedList libs = findLibraryFiles();
                Iterator libsIterator= libs.iterator();
                findEntryMethod.setAccessible(true);
                loadMethod.setAccessible(true);
                constructor.setAccessible(true);
                while (libsIterator.hasNext()){
                    try {
                        String libPath = libsIterator.next().toString();

                        createInstanceArgs[0] = Object.class;
                        createInstanceArgs[1] = libPath;
                        Object _NativeLibraryObject = constructor.newInstance(createInstanceArgs);

                        try {
                            Field nativeLibraryContextField = NativeLibraryClass.getDeclaredField("nativeLibraryContext");
                            nativeLibraryContextField.setAccessible(true);
                            Collection ctx = (Collection) nativeLibraryContextField.get(null);
                            ctx.add(_NativeLibraryObject);
                        }catch (Throwable e){

                        }

                        loadMethodArgs[0] = libPath;
                        loadMethod.invoke(_NativeLibraryObject,loadMethodArgs);

                        long address = (Long) findEntryMethod.invoke(_NativeLibraryObject,new Object[]{"gHotSpotVMStructs"});
                        if (address != 0){
                            NativeLibraryObject = _NativeLibraryObject;
                            readHotSpotVMStructEntrys();
                            boolean flag = isInvalidJvm();
                            HotSpotTypeFields.clear();
                            symEntryCacheMap.clear();
                            if (!flag){
                                break;
                            }
                            NativeLibraryObject = null;
                        }
                    }catch (Throwable ex){

                    }
                }

            } catch (ClassNotFoundException e) {
                //java 15-18
                try {
                    NativeLibraryClass = Class.forName("jdk.internal.loader.NativeLibraries$NativeLibraryImpl");
                    bypassModule();
                    Constructor constructor = getConstructor(NativeLibraryClass,new Class[]{Class.class,String.class,boolean.class,boolean.class});
                    LinkedList libs = findLibraryFiles();
                    Iterator libsIterator= libs.iterator();
                    Method openLibMethod = getMethod(NativeLibraryClass,"open",new Class[0]);
                    findEntryMethod = getMethod(NativeLibraryClass,"find",new Class[]{String.class});
                    findEntryMethod.setAccessible(true);
                    openLibMethod.setAccessible(true);
                    constructor.setAccessible(true);
                    while (libsIterator.hasNext()){
                        try {
                            String libPath = libsIterator.next().toString();
                            Object _NativeLibraryObject = constructor.newInstance(Object.class,libPath,false,false);
                            openLibMethod.invoke(_NativeLibraryObject,new Object[0]);
                            long address = (Long) findEntryMethod.invoke(_NativeLibraryObject,new Object[]{"gHotSpotVMStructs"});
                            if (address != 0){
                                NativeLibraryObject = _NativeLibraryObject;
                                readHotSpotVMStructEntrys();
                                boolean flag = isInvalidJvm();
                                HotSpotTypeFields.clear();
                                symEntryCacheMap.clear();
                                if (!flag){
                                    break;
                                }
                                NativeLibraryObject = null;
                            }
                        }catch (Throwable ex){

                        }
                    }

                } catch (ClassNotFoundException classNotFoundException) {
                    restoreModule();
                }
            }
        }catch (Throwable e){
            e.printStackTrace();
        }
        System.setOut(sysOut);
        return NativeLibraryObject == null ? false : true;
    }
    public static boolean fileExists(String path){
        try {
            return new File(path).exists();
        }catch (Exception e){

        }
        return false;
    }
    public static LinkedList findLibraryFiles(){
        LinkedList list = new LinkedList();
        String javaHome = System.getProperty("java.home");
        if (javaHome != null){
            for (int i = 0; i < libPaths.length; i++) {
                for (int j = 0; j < libNames.length; j++) {
                    String realPath = javaHome + libPaths[i] + libNames[j];
                    String realJrePath = javaHome + "/jre/" +libPaths[i] + libNames[j];
                    if (fileExists(realPath)){
                        list.add(realPath);
                    }
                    if (fileExists(realJrePath)){
                        list.add(realJrePath);
                    }
                }
            }
        }
        return list;
    }
    public static void bypassModule(){
        try {
            Method getModuleMethod = getMethod(Class.class, "getModule", new Class[0]);
            if (getModuleMethod != null) {
                Class targetClass = NativeLibraryClass;
                oldModule = getModuleMethod.invoke(currentClass, new Object[]{});
                Object targetModule = getModuleMethod.invoke(targetClass, new Object[]{});
                unsafe.putObject(currentClass, unsafe.objectFieldOffset(Class.class.getDeclaredField("module")), targetModule);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void restoreModule(){
        try {
            Method getModuleMethod = getMethod(Class.class, "getModule", new Class[0]);
            if (getModuleMethod != null) {
                unsafe.putObject(currentClass, unsafe.objectFieldOffset(Class.class.getDeclaredField("module")), oldModule);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private static Method getMethod(Class clazz, String methodName, Class[] params) {
        Method method = null;
        while (clazz!=null){
            try {
                method = clazz.getDeclaredMethod(methodName,params);
                break;
            }catch (NoSuchMethodException e){
                clazz = clazz.getSuperclass();
            }
        }
        return method;
    }
    private static Constructor getConstructor(Class clazz, Class[] params){
        Constructor constructor = null;
        try {
            constructor = clazz.getDeclaredConstructor(params);
        }catch (NoSuchMethodException e){

        }
        return constructor;
    }
    public static byte[] readData(Object obj,int offset,int length) {
        byte[] temp = new byte[length];
        for (int i = 0; i < length; i++) {
            temp[i] = unsafe.getByteVolatile(obj,offset+i);
        }
        return temp;
    }
    public static byte[] readData(long address,int length) {
        if (address == 0){
            return null;
        }
        byte[] temp = new byte[length];
        for (int i = 0; i < length; i++) {
            temp[i] = unsafe.getByte(address + i);
        }
        return temp;
    }
    public static void writeData(long address,byte[] data) {
        if (address == 0){
            return;
        }

        for (int i = 0; i < data.length; i++) {
            unsafe.putByte(address,data[i]);
            address++;
        }

    }
    public static long readAddressByOop(long address){
        if (isCompressedOopsEnabled()){
            return readCompOopAddressValue(address);
        }else {
            return unsafe.getAddress(address);
        }
    }
    public static long readLong(long address){
        if (address == 0){
            return 0;
        }
        return unsafe.getLong(address);
    }
    public static int readInt(long address){
        if (address == 0){
            return 0;
        }
        return unsafe.getInt(address);
    }
    public static String readCString(long address){
        if (address == 0){
            return null;
        }
        StringBuilder stringBuilder=new StringBuilder();
        byte c;
        while ((c=unsafe.getByte(address))!=0){
            address++;
            stringBuilder.append(((char)c));
        }
        return stringBuilder.toString();
    }
    public static String readCStringByPtr(long address){
        if (address == 0){
            return null;
        }
        long realAddress = unsafe.getAddress(address);
        if (realAddress == 0){
            return null;
        }
        return readCString(realAddress);
    }
    public static long findSym(String symName){
        long address = 0;
        if (!symEntryCacheMap.containsKey(symName)){
            try {
                address = (Long) findEntryMethod.invoke(NativeLibraryObject,new Object[]{symName});
                symEntryCacheMap.put(symName,address);
            } catch (Throwable e) {

            }
        }else {
            return (Long) symEntryCacheMap.get(symName);
        }

        return address;
    }
    //ASSIGN_OFFSET_TO_64BIT_VAR  = long
    public static long findSymAndGetLongValue(String symName){
        return readLong(findSym(symName));
    }
    public static long findSymAndGetAddressValue(String symName){
        long sym = findSym(symName);
        if (sym!=0){
            return unsafe.getAddress(sym);
        }
        throw new IllegalArgumentException("sym == " + sym);
    }
    public static void readHotSpotVMStructEntrys(){
        long gHotSpotVMStructs = findSymAndGetAddressValue("gHotSpotVMStructs");
        long gHotSpotVMStructEntryTypeNameOffset = findSymAndGetLongValue("gHotSpotVMStructEntryTypeNameOffset");
        long gHotSpotVMStructEntryFieldNameOffset = findSymAndGetLongValue("gHotSpotVMStructEntryFieldNameOffset");
        long gHotSpotVMStructEntryTypeStringOffset = findSymAndGetLongValue("gHotSpotVMStructEntryTypeStringOffset");
        long gHotSpotVMStructEntryIsStaticOffset = findSymAndGetLongValue("gHotSpotVMStructEntryIsStaticOffset");
        long gHotSpotVMStructEntryOffsetOffset = findSymAndGetLongValue("gHotSpotVMStructEntryOffsetOffset");
        long gHotSpotVMStructEntryAddressOffset = findSymAndGetLongValue("gHotSpotVMStructEntryAddressOffset");
        long gHotSpotVMStructEntryArrayStride = findSymAndGetLongValue("gHotSpotVMStructEntryArrayStride");

//        typedef struct {
//            const char* typeName;            // The type name containing the given field (example: "Klass")
//            const char* fieldName;           // The field name within the type           (example: "_name")
//            const char* typeString;          // Quoted name of the type of this field (example: "Symbol*";
//            // parsed in Java to ensure type correctness
//            int32_t  isStatic;               // Indicates whether following field is an offset or an address
//            uint64_t offset;                 // Offset of field within structure; only used for nonstatic fields
//            void* address;                   // Address of field; only used for static fields
//            // ("offset" can not be reused because of apparent SparcWorks compiler bug
//            // in generation of initializer data)
//        } VMStructEntry;


        long offset = gHotSpotVMStructs;
        while (offset != 0){

            String typeName = null;
            String fieldName = null;
            String typeString = null;
            boolean isStatic = false;
            long offsetOffset = 0;
            long address = 0;

            String generic = null;

            typeName = readCStringByPtr(offset + gHotSpotVMStructEntryTypeNameOffset);
            if (typeName == null){
                break;
            }

            StringTokenizer tokenizer = new StringTokenizer(typeName,"<>");
            if (tokenizer.countTokens() == 2){
                typeName = tokenizer.nextToken();
                generic = tokenizer.nextToken();
            }

            //VMStructEntry
            LinkedList fields = (LinkedList) HotSpotTypeFields.get(typeName);
            LinkedHashMap fieldInfo = new LinkedHashMap();
            if (fields == null){
                fields = new LinkedList();
                HotSpotTypeFields.put(typeName, fields);
            }
            fields.add(fieldInfo);

            fieldName = readCStringByPtr(offset + gHotSpotVMStructEntryFieldNameOffset);
            isStatic = readInt(offset + gHotSpotVMStructEntryIsStaticOffset) == 1 ? true : false;

            //如果是static字段就说明是绝对地址 反则相对地址
            if (isStatic){
                address = readLong(offset + gHotSpotVMStructEntryAddressOffset);
            }else {
                offsetOffset = readLong(offset + gHotSpotVMStructEntryOffsetOffset);
            }

            typeString = readCStringByPtr(offset + gHotSpotVMStructEntryTypeStringOffset);


            fieldInfo.put("generic",generic);
            fieldInfo.put("typeName",typeName);
            fieldInfo.put("fieldName",fieldName);
            fieldInfo.put("typeString",typeString);
            fieldInfo.put("isStatic",isStatic);
            fieldInfo.put("offset",offsetOffset);
            fieldInfo.put("address",address);


            if ("_dictionary".equals(fieldName)){
                System.out.println();
            }


            offset += gHotSpotVMStructEntryArrayStride;
//            System.out.println("typeName : "+typeName+" fieldName : "+fieldName+" typeString: "+typeString + " isStatic:  "+isStatic+" offset : "+offsetOffset + " address : "+address);
        }
    }
    public static void readHotSpotVMTypeEntrys(){
        long gHotSpotVMTypes = findSymAndGetAddressValue("gHotSpotVMTypes");
        long gHotSpotVMTypeEntryTypeNameOffset = findSymAndGetLongValue("gHotSpotVMTypeEntryTypeNameOffset");
        long gHotSpotVMTypeEntrySuperclassNameOffset = findSymAndGetLongValue("gHotSpotVMTypeEntrySuperclassNameOffset");
        long gHotSpotVMTypeEntryIsOopTypeOffset = findSymAndGetLongValue("gHotSpotVMTypeEntryIsOopTypeOffset");
        long gHotSpotVMTypeEntryIsIntegerTypeOffset = findSymAndGetLongValue("gHotSpotVMTypeEntryIsIntegerTypeOffset");
        long gHotSpotVMTypeEntryIsUnsignedOffset = findSymAndGetLongValue("gHotSpotVMTypeEntryIsUnsignedOffset");
        long gHotSpotVMTypeEntrySizeOffset = findSymAndGetLongValue("gHotSpotVMTypeEntrySizeOffset");
        long gHotSpotVMTypeEntryArrayStride = findSymAndGetLongValue("gHotSpotVMTypeEntryArrayStride");


//        typedef struct {
//            const char* typeName;            // Type name (example: "Method")
//            const char* superclassName;      // Superclass name, or null if none (example: "oopDesc")
//            int32_t isOopType;               // Does this type represent an oop typedef? (i.e., "Method*" or
//            // "Klass*", but NOT "Method")
//            int32_t isIntegerType;           // Does this type represent an integer type (of arbitrary size)?
//            int32_t isUnsigned;              // If so, is it unsigned?
//            uint64_t size;                   // Size, in bytes, of the type
//        } VMTypeEntry;

        long offset = gHotSpotVMTypes;

        while (offset != 0){
            String typeName = readCStringByPtr(offset + gHotSpotVMTypeEntryTypeNameOffset);

            if (typeName == null){
                break;
            }
            String superclassName = readCStringByPtr(offset + gHotSpotVMTypeEntrySuperclassNameOffset);
            boolean isOopType = readInt(offset + gHotSpotVMTypeEntryIsOopTypeOffset) == 0 ? false : true;
            boolean isIntegerType = readInt(offset + gHotSpotVMTypeEntryIsIntegerTypeOffset) == 0 ? false : true;
            boolean isUnsigned = readInt(offset + gHotSpotVMTypeEntryIsUnsignedOffset) == 0 ? false : true;
            long size = readLong(offset + gHotSpotVMTypeEntrySizeOffset);


            String generic = null;

            StringTokenizer tokenizer = new StringTokenizer(typeName,"<>");
            if (tokenizer.countTokens() == 2){
                typeName = tokenizer.nextToken();
            }


            LinkedHashMap typePropertys = new LinkedHashMap();
            HotSpotTypes.put(typeName,typePropertys);


            typePropertys.put("generic",tokenizer.nextToken());
            typePropertys.put("typeName",typeName);
            typePropertys.put("superclassName",superclassName);
            typePropertys.put("isOopType",isOopType);
            typePropertys.put("isIntegerType",isIntegerType);
            typePropertys.put("isUnsigned",isUnsigned);
            typePropertys.put("size",size);
            typePropertys.put("fields",HotSpotTypeFields.get(typeName));


            offset = offset + gHotSpotVMTypeEntryArrayStride;
//            System.out.println("typeName: "+typeName+" superclassName: "+superclassName+" isOopType: "+isOopType + "isIntegerType: "+isIntegerType + " isUnsigned: "+isUnsigned + " size: "+size + " fields: " );//+ HotSpotTypeFields.get(typeName));
        }

    }
    public static void readHotSpotVMIntConstants(){
        long gHotSpotVMIntConstants = findSymAndGetLongValue("gHotSpotVMIntConstants");
        long gHotSpotVMIntConstantEntryNameOffset = findSymAndGetLongValue("gHotSpotVMIntConstantEntryNameOffset");
        long gHotSpotVMIntConstantEntryValueOffset = findSymAndGetLongValue("gHotSpotVMIntConstantEntryValueOffset");
        long gHotSpotVMIntConstantEntryArrayStride = findSymAndGetLongValue("gHotSpotVMIntConstantEntryArrayStride");

//        typedef struct {
//            const char* name;                // Name of constant (example: "_thread_in_native")
//            int32_t value;                   // Value of constant
//        } VMIntConstantEntry;

        long offset = gHotSpotVMIntConstants;

        while (offset != 0){
            String name = readCStringByPtr(offset + gHotSpotVMIntConstantEntryNameOffset);

            if (name == null){
                break;
            }

            int value = readInt(offset + gHotSpotVMIntConstantEntryValueOffset);
            HotSpotVMIntConstants.put(name, value);
            offset = offset + gHotSpotVMIntConstantEntryArrayStride;
//            System.out.println("name: "+name+" value: "+value);
        }


    }
    public static void readgHotSpotVMLongConstants(){
        long gHotSpotVMLongConstants = findSymAndGetAddressValue("gHotSpotVMLongConstants");
        long gHotSpotVMLongConstantEntryNameOffset = findSymAndGetLongValue("gHotSpotVMLongConstantEntryNameOffset");
        long gHotSpotVMLongConstantEntryValueOffset = findSymAndGetLongValue("gHotSpotVMLongConstantEntryValueOffset");
        long gHotSpotVMLongConstantEntryArrayStride = findSymAndGetLongValue("gHotSpotVMLongConstantEntryArrayStride");

//        typedef struct {
//            const char* name;                // Name of constant (example: "_thread_in_native")
//            uint64_t value;                  // Value of constant
//        } VMLongConstantEntry;

        long offset = gHotSpotVMLongConstants;

        while (offset != 0){
            String name = readCStringByPtr(offset + gHotSpotVMLongConstantEntryNameOffset);
            if (name == null){
                break;
            }
            long value = readLong(offset + gHotSpotVMLongConstantEntryValueOffset);
            HotSpotVMLongConstants.put(name, value);
            offset = offset + gHotSpotVMLongConstantEntryArrayStride;
//            System.out.println("name: "+name+" value: "+value);
        }

    }
    public static int lookupIntConstant(String constantName) {
        return (Integer) HotSpotVMIntConstants.get(constantName);
    }
    public static long lookupLongConstant(String constantName) {
        return (Long) HotSpotVMLongConstants.get(constantName);
    }
    public static int getOopSize(){
        try {
            return lookupIntConstant("oopSize");
        }catch (Exception e){
            return (int) sizeOf("char*");
        }
    }
    public static String getCurrentPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return name.split("@")[0];
    }
    public static boolean hasType(String typeName){
        boolean flag = (HotSpotTypeFields.containsKey(typeName) || HotSpotTypes.containsKey(typeName));
        return flag;
    }
    public static Object getTypeProperty(String typeName,String property){
        if (hasType(typeName)){
            Map typeProperty = (Map) HotSpotTypes.get(typeName);
            return typeProperty.get(property);
        }
        return null;
    }
    public static boolean isOopType(String typeName){
        return (Boolean) getTypeProperty(typeName,"isOopType");
    }
    public static boolean isUnsigned(String typeName){
        return (Boolean) getTypeProperty(typeName,"isUnsigned");
    }
    public static boolean isIntegerType(String typeName){
        return (Boolean) getTypeProperty(typeName,"isIntegerType");
    }
    public static long sizeOf(String typeName){
        if (typeName.trim().endsWith("*")){
            return unsafe.addressSize();
        }
        return (Long) getTypeProperty(typeName,"size");
    }
    public static String getSuperclassName(String typeName){
        return (String) getTypeProperty(typeName,"superclassName");
    }
    public static List getFieldInfoOfType(String typeName){
        return (List) getTypeProperty(typeName,"fields");
    }
    public static String[] getFields(String typeName){
        String[] fields = null;
        if (hasType(typeName)){
            LinkedList fieldList = (LinkedList) HotSpotTypeFields.get(typeName);
            fields = new String[fieldList.size()];
            for (int i = 0; i < fields.length; i++) {
                Map fieldInfo = (Map) fieldList.get(i);
                fields[i] = (String) fieldInfo.get("fieldName");
            }
        }
        return fields;
    }
    public static Object getFieldProperty(String typeName,String fieldName,String property){
        if (hasType(typeName)){
            LinkedList fieldList = (LinkedList) HotSpotTypeFields.get(typeName);
            if (fieldList != null){
                Iterator iterator = fieldList.iterator();
                while (iterator.hasNext()){
                    Map fieldInfo = (Map) iterator.next();
                    if (fieldName.equals(fieldInfo.get("fieldName"))){
                        return fieldInfo.get(property);
                    }
                }
            }
        }

        return null;
    }
    public static String getFieldType(String typeName,String fieldName){
        return (String) getFieldProperty(typeName,fieldName,"typeString");
    }
    public static boolean isStaticField(String typeName,String fieldName){
        return (Boolean) getFieldProperty(typeName,fieldName,"isStatic");
    }
    public static long getFieldAddress(String typeName,String fieldName){
        Object value = getFieldProperty(typeName,fieldName,"address");
        if (value == null){
            value = HotSpotVMIntConstants.get(typeName + "::" + fieldName);
        }
        return ((Number) value).longValue();
    }
    public static long getFieldAddressOr(String typeName,String fieldName,String fieldName2){
        try{
            return getFieldAddress(typeName,fieldName);
        }catch (Exception e){
            return getFieldAddress(typeName,fieldName2);
        }
    }
    public static long getFieldOffset(String typeName,String fieldName){
        Object value = getFieldProperty(typeName,fieldName,"offset");
        if (value == null){
            value = HotSpotVMIntConstants.get(typeName + "::" + fieldName);
        }
        return ((Number) value).longValue();
    }
    public static long getFieldOffsetOr(String typeName,String... fieldNames){
        long offset = 0;
        boolean flag = false;
        for (int i = 0; i < fieldNames.length; i++) {
            try {
                offset = getFieldOffset(typeName,fieldNames[i]);
                flag = true;
                break;
            }catch (NullPointerException e){
                continue;
            }
        }
        if (!flag){
            throw new NullPointerException(typeName);
        }

        return offset;
    }
    public static long getFieldOffsetOr(String typeName,String fieldName,String fieldName2){
        try{
            return getFieldOffset(typeName,fieldName);
        }catch (Exception e){
            return getFieldOffset(typeName,fieldName2);
        }
    }

    public static boolean hasFields(String typeName,String... fieldNames){
        for (int i = 0; i < fieldNames.length; i++) {
            String fieldName = fieldNames[i];
            if (!hasField(typeName,fieldName)){
                return false;
            }
        }
        return true;
    }
    public static boolean hasField(String typeName,String fieldName){
        List fieldList = (List) HotSpotTypeFields.get(typeName);
        if (fieldList != null){
            for (int i = 0; i < fieldList.size(); i++) {
                Map fieldInfo = (Map) fieldList.get(i);
                if (fieldInfo!=null && fieldName.equals(fieldInfo.get("fieldName"))){
                    return true;
                }
            }
        }
        return false;
    }
    public static void readHotSpotFlags(){
        String FlagVmName = "Flag";
        if (!hasType(FlagVmName)){
            FlagVmName = "JVMFlag";
        }

        int numFlags = unsafe.getInt(getFieldAddress(FlagVmName,"numFlags"));
        long typeField = 0;
        long nameField = 0;
        long addrField = 0;
        long kindField = 0;
        long _flagsField = 0;

        try {
            typeField = getFieldOffset(FlagVmName,"type");
            nameField = getFieldOffset(FlagVmName,"name");
            addrField = getFieldOffset(FlagVmName,"addr");
            kindField = getFieldOffset(FlagVmName,"kind");
        }catch (Exception e){
            typeField = getFieldOffset(FlagVmName,"_type");
            nameField = getFieldOffset(FlagVmName,"_name");
            addrField = getFieldOffset(FlagVmName,"_addr");
            _flagsField = getFieldOffset(FlagVmName,"_flags");
        }

        long address = unsafe.getAddress(getFieldAddress(FlagVmName,"flags"));

        String _typeTypeString = null;

        if ((_typeTypeString = getFieldType(FlagVmName,"_type"))==null){
            _typeTypeString = getFieldType(FlagVmName,"type");
        }

        for (int i = 0; i < (numFlags -1); i++) {
            Object type = null;
            if (_typeTypeString.contains("char*")){
                type = readCStringByPtr(address + typeField);
            }else {
                type = unsafe.getInt(address + typeField);
            }


            String name = readCStringByPtr(address + nameField);
            long addr = unsafe.getAddress(address + addrField);
            String kind = null;
            int flags = 0;

            if (kindField == 0){
                if (_flagsField != 0){
                    flags = unsafe.getShort(addr + _flagsField) & 0xFFFF;
                }
            }else {
                kind = readCStringByPtr(address + kindField);
            }

//            System.out.println("flag type : " + type+" name : "+name + " addr : "+addr+ " kind : "+kind +" flags :"+flags);

            LinkedHashMap flagInfo = new LinkedHashMap();

            flagInfo.put("type",type);
            flagInfo.put("name",name);
            flagInfo.put("addr",addr);
            flagInfo.put("kind",kind);
            flagInfo.put("flags",flags);

            HotSpotVMFlags.put(name,flagInfo);

            address = address + sizeOf(FlagVmName);
        }
    }
    public static boolean isCompressedOopsEnabled(){
        Map flagInfo = (Map) HotSpotVMFlags.get("UseCompressedOops");
        try {
            if (flagInfo != null){
                long addr = (Long) flagInfo.get("addr");
                return unsafe.getByte(addr) == 1? true :false;
            }
        }catch (Exception e){

        }
        return false;
    }
    public static long getArrayOopLength(long arrayOop){
        long lengthOffset = 0;
        if (isCompressedOopsEnabled()){
            lengthOffset = sizeOf("arrayOopDesc") - sizeOf("jint");
        } else if (javaVersion == 5){
            lengthOffset = sizeOf("arrayOopDesc") - getOopSize();
        }else {
            lengthOffset = sizeOf("arrayOopDesc");
        }
        return unsafe.getInt(arrayOop + lengthOffset) & 0xFFFFFF;
    }
    public static long getHeapOopSize(){

        if (isCompressedOopsEnabled()) {
            return sizeOf("jint");
        } else {
            return getOopSize();
        }
    }
    public static String readSymbol(long symbol){
        long _lengthField = 0; //str len
        long _bodyField = 0; //str

        int strlen = 0;

        if (hasType("symbolOopDesc")){
            _lengthField = getFieldOffset("symbolOopDesc","_length");
            _bodyField = getFieldOffset("symbolOopDesc","_body");

            strlen = unsafe.getShort(symbol + _lengthField);
        }else if (hasType("Symbol")){
            try {
                _lengthField = getFieldOffset("Symbol","_length");
                strlen = unsafe.getShort(symbol + _lengthField);
            }catch (Exception e){
                _lengthField = getFieldOffset("Symbol","_length_and_refcount");
                strlen = (unsafe.getInt(symbol + _lengthField) >> 16);
            }

            _bodyField = getFieldOffset("Symbol","_body");
        }else {
            throw new UnsupportedOperationException("Sym not found");
        }

        if (strlen == 0){
            return new String();
        }else {
            return  new String(readData(symbol + _bodyField, strlen));
        }
    }
    public static long alignUp(long size, long alignment) {
        return ((size + alignment) - 1) & ((alignment - 1) ^ -1);
    }
    private static long arrayHeaderSizeInBytes() {
        long headerSize = 0;
        if (isCompressedOopsEnabled()) {
            headerSize = sizeOf("arrayOopDesc");
        } else {
            headerSize = alignUp(sizeOf("arrayOopDesc") + sizeOf("jint"), HeapWordSize);
        }
        return headerSize;
    }
    private static long arrayHeaderSize() {

        return arrayHeaderSizeInBytes() / HeapWordSize;
    }
    public static long readCompOopAddressValue(long address) {
        long value = unsafe.getInt(address);
        if (value < 0 ){
            value = value & 0xFFFFFFFFL;
        }
        if (value != 0) {
            return narrowOopBase + (value << narrowOopShift);
        }
        return value;
    }
    public static long unpackCompOopAddress(int address) {
        long value = address;
        if (value < 0 ){
            value = value & 0xFFFFFFFFL;
        }
        if (value != 0) {
            return narrowOopBase + (value << narrowOopShift);
        }
        return value;
    }
    public static int compOopAddress(long address) {
        long value = address;
        int ret = 0;
        if (value != 0) {
            value = narrowOopBase - (value >> narrowOopShift);
            ret = (int) (value&0xFFFFFFFF);
        }
        return ret;
    }
    public static long getOopHandleAt(long arrayOop,long index) {
        long oopSize = sizeOf("oopDesc");
        long offset = arrayHeaderSize() * HeapWordSize + (getHeapOopSize() * index);
        if (isCompressedOopsEnabled()) {
            return readCompOopAddressValue(arrayOop + offset);
        }
        return unsafe.getAddress(arrayOop + offset);
    }
    public static long getObjectAddress(Object obj){
        if (obj == null){
            return 0;
        }
        try {
            Field field = currentClass.getDeclaredField("_calcObjectAddress");
            // verify
            HotSpot h =new HotSpot();
            h._calcObjectAddress = null;
            int check1 = unsafe.getInt(h,unsafe.objectFieldOffset(field));
            h._calcObjectAddress = obj;
            int check2 = unsafe.getInt(h,unsafe.objectFieldOffset(field));

            if (check1 !=0 || check1 == check2 || check2 == 0){
                h._calcObjectAddress = null;
                throw new UnsupportedOperationException("old hotspot version");
            }
            long address = 0;
            if (isCompressedOopsEnabled()){
                int compressedOops = unsafe.getInt(h,unsafe.objectFieldOffset(field));
                address = unpackCompOopAddress(compressedOops);
            }else {
                if (getOopSize() == 8){
                    address = unsafe.getLong(h,unsafe.objectFieldOffset(field));
                }else {
                    address = unsafe.getInt(h,unsafe.objectFieldOffset(field));
                }
            }
            h._calcObjectAddress = null;
            return address;
        } catch (NoSuchFieldException e) {
            throw new UnsupportedOperationException(e);
        }
    }
    public static LinkedList getMethodsOldVersionByInstanceKlass(long instanceKlass){
        long oopSize = sizeOf("oopDesc");
        LinkedList methods = new LinkedList();
        long array = unsafe.getAddress(instanceKlass + getFieldOffset(InstanceKlassTypeName,"_methods") + oopSize);

        if (array == 0){
            return methods;
        }
        long methodLength = getArrayOopLength(array);
        long constantPoolOopSize = hasType("ConstantPool")?sizeOf("ConstantPool"):sizeOf("constantPoolOopDesc");
        for (int i = 0; i < methodLength; i++) {
            long methodOopDesc = getOopHandleAt(array,i);
            if (methodOopDesc == 0 || methodOopDesc == 1){
                continue;
            }
            long constMethodOopDesc = unsafe.getAddress(methodOopDesc + getFieldOffset("methodOopDesc","_constMethod"));
            long constantPoolOop = 0;
            long _name_index = unsafe.getShort(constMethodOopDesc + getFieldOffset("constMethodOopDesc","_name_index"))& 0xffff;
            long _signature_index = unsafe.getShort(constMethodOopDesc + getFieldOffset("constMethodOopDesc","_signature_index"))& 0xffff;
            long methodNameSymbolAddress = 0;
            long methodSignatureSymbolAddress = 0;

            if(hasField("methodOopDesc","_constants")){
                constantPoolOop = unsafe.getAddress(methodOopDesc + getFieldOffset("methodOopDesc","_constants"));
                methodNameSymbolAddress = unsafe.getAddress(( constantPoolOop + getOopSize() * _name_index) + constantPoolOopSize);
                methodSignatureSymbolAddress = unsafe.getAddress(( constantPoolOop + getOopSize() * _signature_index) + constantPoolOopSize);
            }else {
                constantPoolOop = unsafe.getAddress(constMethodOopDesc + getFieldOffset("constMethodOopDesc","_constants"));
                methodNameSymbolAddress = unsafe.getAddress(( constantPoolOop + getOopSize() * _name_index) + constantPoolOopSize) ^ 1;
                methodSignatureSymbolAddress = unsafe.getAddress(( constantPoolOop + getOopSize() * _signature_index) + constantPoolOopSize) ^ 1;
            }

            String methodName = readSymbol(methodNameSymbolAddress);
            String methodSignature = readSymbol(methodSignatureSymbolAddress);

            long _code =   unsafe.getAddress(methodOopDesc + getFieldOffset("methodOopDesc","_code"));
            long _from_compiled_entry =   unsafe.getAddress(methodOopDesc + getFieldOffsetOr("methodOopDesc","_from_compiled_entry","_from_compiled_code_entry_point"));
            long _from_interpreted_entry =   unsafe.getAddress(methodOopDesc + getFieldOffsetOr("methodOopDesc","_from_interpreted_entry","_interpreter_entry"));
            int _access_flags =   unsafe.getInt(methodOopDesc + getFieldOffset("methodOopDesc","_access_flags"));


            LinkedHashMap methodInfo = new  LinkedHashMap();
            methodInfo.put("name", methodName);
            methodInfo.put("signature", methodSignature);
            methodInfo.put("_code", _code);
            methodInfo.put("_access_flags", _access_flags);
            methodInfo.put("_from_compiled_entry", _from_compiled_entry);
            methodInfo.put("_from_interpreted_entry", _from_interpreted_entry);
            methodInfo.put("name_index", _name_index);
            methodInfo.put("signature_index", _signature_index);
            methodInfo.put("address", methodOopDesc);
            methodInfo.put("constantPoolOop", constantPoolOop);
            methodInfo.put("slot", i);
            methods.add(methodInfo);
        }
        return methods;
    }
    public static LinkedList getMethodsNewVersionByInstanceKlass(long instanceKlass){
        LinkedList methods = new LinkedList();
        long oopSize = getOopSize();
        long constantPoolSize = sizeOf("ConstantPool");

        long _methodsField = getFieldOffset(InstanceKlassTypeName,"_methods");
        long arrayLengthField = getFieldOffset("Array","_length");
        long arrayDataField = getFieldOffset("Array","_data[0]");

        long _constMethodField = getFieldOffset("Method","_constMethod");
        long _codeField = getFieldOffset("Method","_code");
        long _from_interpreted_entryField = getFieldOffset("Method","_from_interpreted_entry");
        long _from_compiled_entryField = getFieldOffset("Method","_from_compiled_entry");
        long _access_flagsField = getFieldOffset("Method","_access_flags");


        long _constantsField = getFieldOffset("ConstMethod","_constants");
        long _name_indexField = getFieldOffset("ConstMethod","_name_index");
        long _signature_indexField = getFieldOffset("ConstMethod","_signature_index");

        long methodArray = unsafe.getAddress(instanceKlass + _methodsField);

        if (methodArray == 0){
            return methods;
        }

        long methodLength = unsafe.getInt(methodArray + arrayLengthField);

        for (int i = 0; i < methodLength; i++) {
            long mehtod = unsafe.getAddress( methodArray + arrayDataField + (i * oopSize));

            long constMethod = unsafe.getAddress(mehtod + _constMethodField);
            long _code = unsafe.getAddress(mehtod + _codeField);
            int _access_flags = unsafe.getInt(mehtod + _access_flagsField);
            long _from_compiled_entry = unsafe.getAddress(mehtod + _from_compiled_entryField);
            long _from_interpreted_entry = unsafe.getAddress(mehtod + _from_interpreted_entryField);

            long constantPool = unsafe.getAddress(constMethod + _constantsField);
            long name_index = unsafe.getShort(constMethod + _name_indexField) & 0xffff;
            long signature_index = unsafe.getShort(constMethod + _signature_indexField) & 0xffff;

            long nameSym = unsafe.getAddress(constantPool + constantPoolSize + (name_index * oopSize));
            long signatureSym = unsafe.getAddress(constantPool + constantPoolSize + (signature_index * oopSize));


            if (nameSym !=0 && signatureSym !=0){
                String methodName = readSymbol(nameSym);
                String methodSignature = readSymbol(signatureSym);

                LinkedHashMap methodInfo = new  LinkedHashMap();
                methodInfo.put("name", methodName);
                methodInfo.put("signature", methodSignature);
                methodInfo.put("_code", _code);
                methodInfo.put("_access_flags", _access_flags);
                methodInfo.put("_from_compiled_entry", _from_compiled_entry);
                methodInfo.put("_from_interpreted_entry", _from_interpreted_entry);
                methodInfo.put("name_index", name_index);
                methodInfo.put("signature_index", signature_index);
                methodInfo.put("address", mehtod);
                methodInfo.put("constantPoolOop", constantPool);
                methodInfo.put("slot", i);


                methods.add(methodInfo);
            }
        }
        return methods;
    }
    public static LinkedList getMethodsByInstanceKlass(long instanceKlass){
        if (instanceKlass == 0){
            return new LinkedList();
        }

        String methodsType = getFieldType(InstanceKlassTypeName,"_methods");
        if ("Array<Method*>*".equals(methodsType)){
            return getMethodsNewVersionByInstanceKlass(instanceKlass);
        }else {
            return getMethodsOldVersionByInstanceKlass(instanceKlass);
        }

    }
    public static LinkedList getAllLoadedClassesOldVersion(boolean readMethods) {

        LinkedList classes = new LinkedList();

        long tableSizeField   = getFieldOffset("BasicHashtable","_table_size");

        long bucketsField   = getFieldOffset("BasicHashtable","_buckets");

        long bucketSize = sizeOf("HashtableBucket");

        long _entry = getFieldOffset("HashtableBucket","_entry");

        long _dictionary = unsafe.getAddress(getFieldAddress("SystemDictionary","_dictionary"));

        long BasicHashtableEntry__next = getFieldOffset("BasicHashtableEntry","_next");


        if (_dictionary == 0){
            return classes;
        }

        int tblSize = unsafe.getInt(_dictionary + tableSizeField);

        long literalField = 0;

        try {
            literalField = getFieldOffset("IntptrHashtableEntry","_literal");
        }catch (Exception e){
            literalField = getFieldOffset("HashtableEntry","_literal");
        }

        long KlassNameField = getFieldOffset("Klass","_name"); //Symbol*
        long _class_loaderField = getFieldOffsetOr(InstanceKlassTypeName,"_class_loader","_class_loader_data"); //Symbol*

        long classLoaderData_class_loaderField = hasField("ClassLoaderData","_class_loader") ? getFieldOffset("ClassLoaderData","_class_loader") : -1;

        long oopSize = sizeOf("oopDesc");

        long klassSize = sizeOf("Klass");

        for (int index = 0; index < tblSize; index++) {
            long bucket = unsafe.getAddress(_dictionary + bucketsField) + bucketSize * index;
            bucket = unsafe.getAddress(bucket) + _entry;
            for (long probe = bucket; probe != 0; probe = unsafe.getAddress(probe + BasicHashtableEntry__next)) {
                long literal = unsafe.getAddress(probe + literalField);// InstanceKlass
                long symbol;
                long classloader;
                if (javaVersion >= 8){
                    symbol = unsafe.getAddress(KlassNameField + literal);
                    classloader = unsafe.getAddress(_class_loaderField + literal);
                    if (classLoaderData_class_loaderField != -1){ //ClassLoaderData
                        classloader = unsafe.getAddress(classloader  + classLoaderData_class_loaderField);
                    }
                }else {
                    symbol = unsafe.getAddress(KlassNameField + literal + oopSize);
                    classloader = unsafe.getAddress(_class_loaderField + literal + oopSize);
                }
                String className = readSymbol(symbol);

                if (className != null){
                    className = className.replace("/",".");
                }

                LinkedHashMap classInfo = new LinkedHashMap();
                classInfo.put("className",className);
                classInfo.put("classLoader",classloader);
                classInfo.put("instanceKlass",literal);
                if (readMethods){
                    classInfo.put("methods",getMethodsByInstanceKlass(literal));
                }
                classes.add(classInfo);

            }
        }
        return classes;
    }
    public static LinkedList getAllLoadedClassesNewVersion(boolean readMethods){
        LinkedList classes = new LinkedList();
        long _klassesField = getFieldOffset("ClassLoaderData","_klasses");
        long _class_loaderField = getFieldOffset("ClassLoaderData","_class_loader");
        long _nextField = getFieldOffset("ClassLoaderData","_next");

        long _next_linkField = getFieldOffset("Klass","_next_link");

        long klassNameField = getFieldOffset("Klass","_name"); //Symbol*

        long classIndex = 0;
        long classLoaderDataIndex = 0;
        for (long classLoaderData = unsafe.getAddress(getFieldAddress("ClassLoaderDataGraph","_head"));
             classLoaderData != 0; classLoaderData = unsafe.getAddress(classLoaderData + _nextField)){

            long  classloader = unsafe.getAddress(classLoaderData + _class_loaderField);

            for (long instanceKlass = unsafe.getAddress(classLoaderData + _klassesField);
                 instanceKlass != 0; instanceKlass = unsafe.getAddress(instanceKlass + _next_linkField)) {

                long classNameSym = unsafe.getAddress(instanceKlass + klassNameField);
                String className = readSymbol(classNameSym);
                if (className != null){
                    className = className.replace("/",".");
                }

                LinkedHashMap classInfo = new LinkedHashMap();
                classInfo.put("className",className);
                classInfo.put("classLoader",classloader);
                classInfo.put("instanceKlass",instanceKlass);
                if (readMethods){
                    classInfo.put("methods",getMethodsByInstanceKlass(instanceKlass));
                }
                classes.add(classInfo);
                classIndex++;
            }
            classLoaderDataIndex++;
        }
        return classes;
    }
    public static LinkedList getAllLoadedClasses(boolean readMethods){
        if (hasField("ClassLoaderData","_klasses")){
            return getAllLoadedClassesNewVersion(readMethods);
        }else {
            return getAllLoadedClassesOldVersion(readMethods);
        }
    }
    public static boolean isInvalidJvm(){
        try {
            if (unsafe.getAddress(getFieldAddress("SystemDictionary","_dictionary")) != 0){
                return false;
            }
        }catch (NullPointerException e){

        }

        try {
            if (unsafe.getAddress(getFieldAddress("ClassLoaderDataGraph","_head")) != 0){
                return false;
            }
        }catch (NullPointerException e){

        }


        return true;
    }
    public static long getKlass(Class clazz) {
        int _klass_offset = 0;
        int oopSize = getOopSize();
        if (hasField("java_lang_Class","_klass_offset")){
            _klass_offset = unsafe.getInt(getFieldAddress("java_lang_Class","_klass_offset"));
        }
        long classAddress = getObjectAddress(clazz);

        if (javaVersion < 8){
            LinkedList classList = getAllLoadedClasses(false);
            String clazzName = clazz.getName();
            for (int i = 0; i <classList.size(); i++) {
                Map classInfo = (Map) classList.get(i);
                String className = (String) classInfo.get("className");
                if (clazzName.equals(className)){
                    return (Long) classInfo.get("instanceKlass");
                }
            }
            return 0;
        }
        return unsafe.getLong(classAddress + _klass_offset);

    }
    public static long getKlassFromObject(Object obj) {

        long _klass_offset = unsafe.addressSize();
        if(hasType("oopDesc")){
            _klass_offset = getFieldOffsetOr("oopDesc","_metadata._klass","_klass");
        }
        long classAddress = 0;
        try {
            classAddress = getObjectAddress(obj);
        }catch (Throwable e){
            return 0;
        }
        long fakeAddress = unsafe.getAddress(classAddress + _klass_offset);

        if (unsafe.addressSize() == 8 && isCompressedOopsEnabled()){
            fakeAddress = unpackCompOopAddress((int) fakeAddress);
        }

        return fakeAddress;

    }
    public static long getKlassByClassName(String className) {
        Class clazz = null;
        try {
            clazz = Class.forName(className,true,Thread.currentThread().getContextClassLoader());
        }catch (ClassNotFoundException e){
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException classNotFoundException) {
                classNotFoundException.printStackTrace();
            }
        }
        if (clazz != null){
            return getKlass(clazz);
        }else {
            LinkedList classList = getAllLoadedClasses(false);
            for (int i = 0; i <classList.size(); i++) {
                Map classInfo = (Map) classList.get(i);
                String _className = (String) classInfo.get("className");
                if (className.equals(_className)){
                    return (Long) classInfo.get("instanceKlass");
                }
            }
        }
        return 0;

    }
    public static String getMethodSignature(Method method) {
        StringBuilder s = new StringBuilder();
        Class[] types = new Class[method.getParameterTypes().length + 1];
        String[] typeStrArr = new String[types.length];
        System.arraycopy(method.getParameterTypes(),0,types,0,types.length-1);
        types[types.length-1] = method.getReturnType();

        for (int i = 0; i < types.length; i++) {
            Class type = types[i];
            boolean isArray = type.isArray();
            if (isArray) {
                type = type.getComponentType();
            }

            if (int.class.equals(type)) {
                typeStrArr[i] = "I";
            }else if (void.class.equals(type)) {
                typeStrArr[i] = "V";
            }else if (boolean.class.equals(type)) {
                typeStrArr[i] = "Z";
            }else if (char.class.equals(type)) {
                typeStrArr[i] = "C";
            }else if (byte.class.equals(type)) {
                typeStrArr[i] = "B";
            }else if (short.class.equals(type)) {
                typeStrArr[i] = "S";
            }else if (float.class.equals(type)) {
                typeStrArr[i] = "F";
            }else if (long.class.equals(type)) {
                typeStrArr[i] = "J";
            }else if (double.class.equals(type)) {
                typeStrArr[i] = "D";
            }else {
                typeStrArr[i] ="L" + type.getName().replace(".","/")+";";
            }

            if (isArray){
                typeStrArr[i] = "[" + typeStrArr[i];
            }

        }

        s.append("(");

        for (int i = 0; (i < typeStrArr.length-1); i++) {
            s.append(typeStrArr[i]);
        }

        s.append(")");

        s.append(typeStrArr[typeStrArr.length -1]);

        return s.toString();
    }
    public static long getMethodByKlass(long klass,Method method){
        return getMethodByKlass(klass,method.getName(),getMethodSignature(method));
    }
    public static long getMethodByKlass(long klass,String methodName,String signature){
        List targetMethodList = getMethodsByInstanceKlass(klass);
        long methodHandle = 0;
        boolean find = false;
        for (int i = 0; i < targetMethodList.size(); i++) {
            Map targetMethodInfo = (Map) targetMethodList.get(i);
            String _methodName = (String) targetMethodInfo.get("name");
            String _signature = (String) targetMethodInfo.get("signature");
            if(methodName.equals(_methodName) && signature.equals(_signature)){
                methodHandle = (Long) targetMethodInfo.get("address");
                find = true;
                break;
            }
        }
        if (!find){
            methodHandle = 0;
        }
        return methodHandle;
    }
    public static Map getMethodInfoByKlass(long klass,String methodName,String signature){
        List targetMethodList = getMethodsByInstanceKlass(klass);
        long methodHandle = 0;
        for (int i = 0; i < targetMethodList.size(); i++) {
            Map targetMethodInfo = (Map) targetMethodList.get(i);
            String _methodName = (String) targetMethodInfo.get("name");
            String _signature = (String) targetMethodInfo.get("signature");
            if(methodName.equals(_methodName) && signature.equals(_signature)){
                return targetMethodInfo;
            }
        }
        return null;
    }
    public static void copyConstMethodField(long srcMethod, long destMethod,String fieldName){
        String methodType = hasType("Method")?"Method":"methodOopDesc";
        String constMethodType =  hasType("ConstMethod")?"ConstMethod":"constMethodOopDesc";
        long _constMethodField = getFieldOffset(methodType,"_constMethod");
        srcMethod = unsafe.getAddress(srcMethod + _constMethodField);
        destMethod = unsafe.getAddress(destMethod + _constMethodField);
        long offset = getFieldOffset(constMethodType,fieldName);
        String fieldType = getFieldType(constMethodType,fieldName);
        int fieldSize = (int) sizeOf(fieldType);
        switch(fieldSize){
            case 1:
                unsafe.putByte(destMethod + offset, unsafe.getByte(srcMethod + offset));
                return;
            case 2:
                unsafe.putShort(destMethod + offset, unsafe.getShort(srcMethod + offset));
                return;
            case 4:
                unsafe.putInt(destMethod + offset, unsafe.getInt(srcMethod + offset));
                return;
            case 8:
                unsafe.putLong(destMethod + offset, unsafe.getLong(srcMethod + offset));
                return;
            default:
                throw new IllegalArgumentException();
        }
    }
    public static void copyField(long srcMethod, long destMethod,String fieldName){
        String methodType =  hasType("methodOopDesc") ? "methodOopDesc" : "Method";
        long offset = getFieldOffset(methodType,fieldName);
        String fieldType = getFieldType(methodType,fieldName);
        int fieldSize = (int) sizeOf(fieldType);
        switch(fieldSize){
            case 1:
                unsafe.putByte(destMethod + offset, unsafe.getByte(srcMethod + offset));
                return;
            case 2:
                unsafe.putShort(destMethod + offset, unsafe.getShort(srcMethod + offset));
                return;
            case 4:
                unsafe.putInt(destMethod + offset, unsafe.getInt(srcMethod + offset));
                return;
            case 8:
                unsafe.putLong(destMethod + offset, unsafe.getLong(srcMethod + offset));
                return;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static void memCpy(byte[] src,long dest,int offset,int size){
        long tmp = unsafe.allocateMemory(size);
        for (int i = 0; i < size; i++) {
            unsafe.putByte(tmp + i,src[offset + i]);
        }
        unsafe.copyMemory(tmp,dest,size);
        unsafe.freeMemory(tmp);
    }
    public static void typeCpy(String type,long srcType,long destType){
        long typeSize = sizeOf(type);
        unsafe.copyMemory(srcType,destType,typeSize);
    }
    public static void typeSwap(String type,long srcType,long destType){
        long typeSize = sizeOf(type);
        long tmp = unsafe.allocateMemory(typeSize);

        String methodType = hasType("Method")?"Method":"methodOopDesc";
        String constMethodType =  hasType("ConstMethod")?"ConstMethod":"constMethodOopDesc";

        long methodIdField = getFieldOffset(constMethodType,hasField(constMethodType,"_method_idnum")?"_method_idnum":"_method_index");
        long _constMethodField = getFieldOffset(methodType,"_constMethod");

        unsafe.copyMemory(srcType,tmp,typeSize);
        unsafe.copyMemory(destType,srcType,typeSize);
        unsafe.copyMemory(tmp,destType,typeSize);
        unsafe.freeMemory(tmp);

    }
    public static boolean methodSwap(String type,long targetMethod,long newMethod){
        String methodType = hasType("Method")?"Method":"methodOopDesc";
        String constMethodType =  hasType("ConstMethod")?"ConstMethod":"constMethodOopDesc";
        if (targetMethod !=0 && newMethod !=0){
            typeSwap(methodType,targetMethod,newMethod);
            return true;
        }
        return false;
    }
    public static boolean methodHook(String type,long targetMethod,long newMethod){
        String methodType = hasType("Method")?"Method":"methodOopDesc";
        if (targetMethod !=0 && newMethod !=0){
            typeCpy(methodType,newMethod,targetMethod);
            return true;
        }
        return false;
    }


    public static LinkedHashMap getTypeFieldValues(long typeAddress,String typeName){
        LinkedHashMap result = new LinkedHashMap();
        String[] fieldNames = getFields(typeName);

        for (int i = 0; i < fieldNames.length; i++) {
            String fieldName = fieldNames[i].trim();
            String fieldType = getFieldType(typeName,fieldName);
            long offset = getFieldOffset(typeName,fieldName);
            long address = getFieldAddress(typeName,fieldName);

            //skip static field
            if (address!=0){
                continue;
            }

            // init value field
            LinkedHashMap value = new LinkedHashMap();
            value.put("type",fieldType);
            value.put("offset",offset);

            if (fieldType.endsWith("*") && !fieldType.equals("char*") || fieldType.equals("address") || fieldType.equals("uintptr_t")){
                long valueAddress = unsafe.getAddress(typeAddress + offset);
                value.put("value",valueAddress);
            }else {
                if ("char*".equals(fieldType)) {
                    long valueAddress = unsafe.getAddress(typeAddress + offset);
                    String valueString = null;
                    if (valueAddress != 0){
                        valueString = readCString(valueAddress);
                    }
                    value.put("valuePtr","0x" + Long.toHexString(valueAddress));
                    value.put("value",valueString);
                }else if ("u1".equals(fieldType) || "char".equals(fieldType) || "jbyte".equals(fieldType) || "jchar".equals(fieldType) || "volatile unsigned char".equals(fieldType)){
                    byte valueByte = unsafe.getByte(typeAddress + offset);
                    value.put("value",valueByte);
                }else if ("u2".equals(fieldType)){
                    int valueShort = unsafe.getShort(typeAddress + offset) & 0xffff;
                    value.put("value",valueShort);
                }else if ("u4".equals(fieldType) || "size_t".equals(fieldType) || "uint32_t".equals(fieldType) ){
                    int valueInt = unsafe.getShort(typeAddress + offset) & 0xffffffff;
                    value.put("value",valueInt);
                }else if ("u8".equals(fieldType) || "uint64_t".equals(fieldType)){
                    long valueLong = unsafe.getLong(typeAddress + offset) & 0xffffffffffffffffL;
                    value.put("value",valueLong);
                }else if ("long".equals(fieldType)){
                    long valueLong = unsafe.getLong(typeAddress + offset);
                    value.put("value",valueLong);
                }else if ("int".equals(fieldType) || "jint".equals(fieldType)){
                    long valueInt = unsafe.getInt(typeAddress + offset);
                    value.put("value",valueInt);
                }else if ("short".equals(fieldType) || "jshort".equals(fieldType)){
                    long valueShort = unsafe.getShort(typeAddress + offset);
                    value.put("value",valueShort);
                }else if ("bool".equals(fieldType) || "jboolean".equals(fieldType)){
                    boolean valueBool = unsafe.getByte(typeAddress + offset) != 0;
                    value.put("value",valueBool);
                }else if ("jfloat".equals(fieldType)){
                    float valueFloat = unsafe.getFloat(typeAddress + offset);
                    value.put("value",valueFloat);
                }else if ("jdouble".equals(fieldType)){
                    double valueDouble = unsafe.getDouble(typeAddress + offset);
                    value.put("value",valueDouble);
                }else {
                    value.put("value","UnsupportedType");
                }
            }
            result.put(fieldName,value);
        }
        return result;
    }
    public static long getCompiledCodeSize(long nmethod){
        long size = 0;
        String codeBlobType = "CodeBlob";
        if (hasFields(codeBlobType,"_code_begin","_code_end")){
            long _code_beginField = getFieldOffset(codeBlobType,"_code_begin");
            long _code_endField = getFieldOffset(codeBlobType,"_code_end");

            long _code_begin = unsafe.getAddress(nmethod + _code_beginField);
            long _code_end = unsafe.getAddress(nmethod + _code_endField);
            size = _code_end - _code_begin;
        }else if (hasFields(codeBlobType,"_data_offset","_code_offset") || hasFields(codeBlobType,"_data_offset","_instructions_offset")){
            long _data_offsetField = getFieldOffset(codeBlobType,"_data_offset");
            long _code_offsetField = getFieldOffsetOr(codeBlobType,"_code_offset","_instructions_offset");

            long _data_offset = unsafe.getInt(nmethod + _data_offsetField);
            long _code_offset = unsafe.getInt(nmethod + _code_offsetField);

            size = _data_offset - _code_offset;

        }else {
            throw new UnsupportedOperationException();
        }
        return size;
    }

    static long _shellcodeJit1;
    static long _shellcodeJit2;
    static long _shellcodeJit3;
    static long _shellcodeJit4;
    static long _shellcodeJit5;
    public static int runShellcodeFunctionByJit(){
        for (int i = 0; i < 5000; i++) {
            //用大量代码填充rwx区域 直到大于shellcode的长度
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);
            _shellcodeJit1++;_shellcodeJit2++;_shellcodeJit3++;_shellcodeJit4++;_shellcodeJit5++;
            Long.reverse(_shellcodeJit1);Long.reverse(_shellcodeJit2);Long.reverse(_shellcodeJit3);Long.reverse(_shellcodeJit4);Long.reverse(_shellcodeJit5);



        }
        return 123;
    }
    public static long currentTimeMillis2(){
        return 8888888;
    }

    public static void exit(Object this2,int status) {
        long abc = 0;
        for (int i = 0; i < 1000; i++) {
            abc++;
        }
        for (int i = 0; i < 1000; i++) {
            abc++;
        }
        for (int i = 0; i > 1000; i++) {
            abc--;
        }
        if (this2 == null){
            return;
        }
        System.out.println("Access hook exit");
        System.out.println("this ->" + this2);
        System.out.println("exit status ->" + status);
    }


    public static long testCall(long v){
        return 1234;
    }

    public static String getProperty(String key) {
        return "hacker";
    }

    public long getId() {
        return 88888888L;
    }

    /**
     *  直接替换方法的解释地址与编译地址
     * */
    public static void testReplaceAddress() throws Throwable{
        long hotspotClassHandle = getKlass(HotSpot.class);
        long runtimeClassHandle = getKlass(Runtime.class);

        boolean isHighVer = !hasType("methodOop");

        long exitMethodHandle = getMethodByKlass(runtimeClassHandle,"exit",getMethodSignature(Runtime.class.getMethod("exit", int.class)));

        long hotspotExitHandle = getMethodByKlass(hotspotClassHandle,"exit",getMethodSignature(HotSpot.class.getMethod("exit", Object.class, int.class)));


        System.out.println("Hotspot Class Address : 0x" + Long.toHexString(hotspotClassHandle));
        System.out.println("Runtime Class Address : 0x" + Long.toHexString(runtimeClassHandle));
        System.out.println("被hook的方法地址 Runtime->exit : 0x" + Long.toHexString(exitMethodHandle));
        //第一个参数指向this
        System.out.println("hook的方法地址 Hotspot->exit : 0x" + Long.toHexString(hotspotExitHandle));


        long _codeField = 0;
        long _from_interpreted_entryField = 0;
        long _from_compiled_entry_entryField = 0;
        long _access_flagsField = 0;
        if (isHighVer){
            _from_interpreted_entryField = getFieldOffset("Method","_from_interpreted_entry");
            _from_compiled_entry_entryField = getFieldOffset("Method","_from_compiled_entry");
            _codeField = getFieldOffset("Method","_code");
            _access_flagsField = getFieldOffset("Method","_access_flags");
        }else {
            _codeField = getFieldOffset("methodOopDesc","_code");
            _from_interpreted_entryField = getFieldOffsetOr("methodOopDesc","_from_interpreted_entry","_interpreter_entry");
            _from_compiled_entry_entryField = getFieldOffsetOr("methodOopDesc","_from_compiled_entry","_from_compiled_code_entry_point");
            _access_flagsField = getFieldOffset("methodOopDesc","_access_flags");
        }

        boolean compileFlag = false;

        long hotspotNMethodAddress = 0;

        for (int i = 0; i < 100000; i++) {
            exit(null,1);
            hotspotNMethodAddress = unsafe.getAddress(hotspotExitHandle + _codeField);
            if (hotspotNMethodAddress != 0){
                break;
            }
        }
        if (hotspotNMethodAddress == 0){
            System.out.println("未能成功编译方法 无法继续执行");
            return;
        }

        System.out.println("jit地址 : 0x" + Long.toHexString(unsafe.getAddress(hotspotExitHandle + _from_compiled_entry_entryField)));


        unsafe.putAddress(exitMethodHandle + _from_interpreted_entryField,unsafe.getAddress(hotspotExitHandle + _from_interpreted_entryField));
        unsafe.putAddress(exitMethodHandle + _from_compiled_entry_entryField,unsafe.getAddress(hotspotExitHandle + _from_compiled_entry_entryField));
        unsafe.putAddress(exitMethodHandle + _codeField,unsafe.getAddress(hotspotExitHandle + _codeField));



        //把断点数量设置大于0 禁止该方法经过其它编译器进行编译
        if (hasField("Method","_method_counters")){
            long _number_of_breakpointsField = getFieldOffset("MethodCounters","_number_of_breakpoints");
            long _method_countersField = getFieldOffset("Method","_method_counters");
            long _method_counters = unsafe.getAddress(hotspotExitHandle + _method_countersField);
            unsafe.putAddress(exitMethodHandle + _method_countersField,_method_counters);
            unsafe.putShort(_method_counters + _number_of_breakpointsField,(short) 111);
        }else if (hasField("methodOopDesc","_number_of_breakpoints")){
            long _number_of_breakpointsField = getFieldOffset("methodOopDesc","_number_of_breakpoints");
            unsafe.putShort(hotspotExitHandle + _number_of_breakpointsField , (short) 111);
            unsafe.putShort(hotspotExitHandle + _number_of_breakpointsField , (short) 111);
        }


        /**
         *
         * // Enumeration to distinguish tiers of compilation
         * enum CompLevel {
         *   CompLevel_any               = -1,
         *   CompLevel_all               = -1,
         *   CompLevel_none              = 0,         // Interpreter
         *   CompLevel_simple            = 1,         // C1
         *   CompLevel_limited_profile   = 2,         // C1, invocation & backedge counters
         *   CompLevel_full_profile      = 3,         // C1, invocation & backedge counters + mdo
         *   CompLevel_full_optimization = 4,         // C2 or Shark
         * **/
        if (isHighVer){
            //设置编译等级 目前c2编译器是最高的 禁止其它编译器再编译
            unsafe.putInt(unsafe.getAddress(hotspotExitHandle + _codeField) + getFieldOffset("nmethod","_comp_level"),4);
        }


        System.out.println("开始调用 Runtime.getRuntime().exit(0)");

        Runtime.getRuntime().exit(0);
        System.exit(0);
        System.out.println("调用结束 进程没有退出 成功hook!");
    }
    /**
     *  直接替换Native方法的解释地址与编译地址  并且调用旧的方法
     * */
    public static void testReplaceNativeAddressAndCallOld() throws Throwable{
        long hotspotClassHandle = getKlass(HotSpot.class);
        long systemClassHandle = getKlass(System.class);

        boolean isHighVer = !hasType("methodOop");

        long systemMethodHandle = getMethodByKlass(systemClassHandle,"currentTimeMillis",getMethodSignature(System.class.getMethod("currentTimeMillis")));

        long hotspotMethodHandle = getMethodByKlass(hotspotClassHandle,"currentTimeMillis2",getMethodSignature(HotSpot.class.getMethod("currentTimeMillis2")));


        System.out.println("Hotspot Class Address : 0x" + Long.toHexString(hotspotClassHandle));
        System.out.println("Runtime Class Address : 0x" + Long.toHexString(systemClassHandle));
        System.out.println("被hook的方法地址 System->currentTimeMillis : 0x" + Long.toHexString(systemMethodHandle));
        System.out.println("hook的方法地址 Hotspot->currentTimeMillis : 0x" + Long.toHexString(hotspotMethodHandle));


        long _codeField = 0;
        long _from_interpreted_entryField = 0;
        long _from_compiled_entry_entryField = 0;
        long _access_flagsField = 0;
        long _i2i_entryField = 0;
        if (isHighVer){
            _from_interpreted_entryField = getFieldOffset("Method","_from_interpreted_entry");
            _from_compiled_entry_entryField = getFieldOffset("Method","_from_compiled_entry");
            _codeField = getFieldOffset("Method","_code");
            _access_flagsField = getFieldOffset("Method","_access_flags");
            _i2i_entryField = getFieldOffset("Method","_i2i_entry");
        }else {
            _codeField = getFieldOffset("methodOopDesc","_code");
            _from_interpreted_entryField = getFieldOffsetOr("methodOopDesc","_from_interpreted_entry","_interpreter_entry");
            _from_compiled_entry_entryField = getFieldOffsetOr("methodOopDesc","_from_compiled_entry","_from_compiled_code_entry_point");
            _access_flagsField = getFieldOffset("methodOopDesc","_access_flags");
        }

        boolean compileFlag = false;

        long hotspotNMethodAddress = 0;

        for (int i = 0; i < 10000; i++) {
            currentTimeMillis2();
            System.currentTimeMillis();
            hotspotNMethodAddress = unsafe.getAddress(hotspotMethodHandle + _codeField);
            if (hotspotNMethodAddress != 0){
                break;
            }
        }

        //什么也不做
        if(hotspotNMethodAddress == 0 ){

        }

        System.out.println("jit地址 : 0x" + Long.toHexString(unsafe.getAddress(hotspotMethodHandle + _from_compiled_entry_entryField)));


        long system_from_interpreted_entry = unsafe.getAddress(systemMethodHandle + _from_interpreted_entryField);
        long system_from_compiled_entry_entry = unsafe.getAddress(systemMethodHandle + _from_compiled_entry_entryField);
        long system_code = unsafe.getAddress(systemMethodHandle + _codeField);
        long system_i2i_entry = unsafe.getAddress(systemMethodHandle + _i2i_entryField);

        unsafe.putAddress(systemMethodHandle + _from_interpreted_entryField,unsafe.getAddress(hotspotMethodHandle + _from_interpreted_entryField));
        unsafe.putAddress(systemMethodHandle + _from_compiled_entry_entryField,unsafe.getAddress(hotspotMethodHandle + _from_compiled_entry_entryField));
        unsafe.putAddress(systemMethodHandle + _codeField,unsafe.getAddress(hotspotMethodHandle + _codeField));
//        由于native方法和普通的java方法传参不同需要修适配器
        unsafe.putAddress(systemMethodHandle + _i2i_entryField,unsafe.getAddress(hotspotMethodHandle + _i2i_entryField));


        //禁止该方法经过其它编译器进行编译
        int newMethodAccessFlags = unsafe.getInt(hotspotMethodHandle + _access_flagsField);
//        newMethodAccessFlags  = newMethodAccessFlags | Modifier.NATIVE;

        unsafe.putInt((hotspotMethodHandle + _access_flagsField),newMethodAccessFlags);

        unsafe.putAddress(hotspotMethodHandle + _from_interpreted_entryField,system_from_interpreted_entry);
        unsafe.putAddress(hotspotMethodHandle + _from_compiled_entry_entryField,system_from_compiled_entry_entry);
        unsafe.putAddress(hotspotMethodHandle + _codeField,0);
//        //由于native方法和普通的java方法传参不同需要修适配器
        unsafe.putAddress(hotspotMethodHandle + _i2i_entryField,system_i2i_entry);


        for (int i = 0; i < 10000; i++) {
            currentTimeMillis2();
        }

        /**
         *
         * // Enumeration to distinguish tiers of compilation
         * enum CompLevel {
         *   CompLevel_any               = -1,
         *   CompLevel_all               = -1,
         *   CompLevel_none              = 0,         // Interpreter
         *   CompLevel_simple            = 1,         // C1
         *   CompLevel_limited_profile   = 2,         // C1, invocation & backedge counters
         *   CompLevel_full_profile      = 3,         // C1, invocation & backedge counters + mdo
         *   CompLevel_full_optimization = 4,         // C2 or Shark
         * **/
        if (isHighVer && hotspotNMethodAddress !=0){
            //设置编译等级 禁止其它编译器再编译
            unsafe.putInt(hotspotNMethodAddress + getFieldOffset("nmethod","_comp_level"),4);
        }

        System.out.println("开始调用 System.currentTimeMillis()");

        System.out.println(System.currentTimeMillis());
        System.out.println(currentTimeMillis2());

        System.out.println("调用结束 进程没有退出 成功hook!");
    }

    /**
     *  直接替换方法的内存
     * */
    public static void testReplaceMemory() throws Throwable{
        long hotspotClassHandle = getKlass(HotSpot.class);
        long systemClassHandle = getKlass(Thread.class);


        String methodType = hasType("methodOopDesc")?"methodOopDesc":"Method";

        long systemMethodHandle = getMethodByKlass(systemClassHandle,"getId",getMethodSignature(Thread.class.getMethod("getId")));

        long hotspotMethodHandle = getMethodByKlass(hotspotClassHandle,"getId",getMethodSignature(HotSpot.class.getMethod("getId")));



        System.out.println("Hotspot Class Address : 0x" + Long.toHexString(hotspotClassHandle));
        System.out.println("System Class Address : 0x" + Long.toHexString(systemClassHandle));
        System.out.println("被hook的方法地址 Runtime->exit : 0x" + Long.toHexString(systemMethodHandle));
        //第一个参数指向this
        System.out.println("hook的方法地址 Hotspot->exit : 0x" + Long.toHexString(hotspotMethodHandle));



        String constMethodType =  hasType("ConstMethod")?"ConstMethod":"constMethodOopDesc";
        long methodIdField = getFieldOffset(constMethodType,hasField(constMethodType,"_method_idnum")?"_method_idnum":"_method_index");
        long _constMethodField = getFieldOffset(methodType,"_constMethod");

        long targetConstMethod = unsafe.getAddress(systemMethodHandle + _constMethodField);
        long newConstMethod = unsafe.getAddress(hotspotMethodHandle + _constMethodField);

        short hotspot_method_idnum =  unsafe.getShort(newConstMethod + methodIdField);
        short system_method_idnum =  unsafe.getShort(targetConstMethod + methodIdField);


        unsafe.putAddress(systemMethodHandle + _constMethodField,newConstMethod + _constMethodField);
//        typeSwap(methodType,hotspotMethodHandle,systemMethodHandle);

//        unsafe.putShort(targetConstMethod + methodIdField,hotspot_method_idnum);
        unsafe.putShort(newConstMethod + methodIdField,system_method_idnum);


        long result = Thread.currentThread().getId();
        System.out.println("调用被hook方法 System.getProperty(\"java.home\") 返回 : " + result);


        if ("hacker".equals(result)){
            System.out.println("成功Hook!");
        }else {
            System.out.println("Hook失败!");
        }

    }

    /**
     * 直接交换方法的内存  并且调用旧的方法
     * */
    public static void testReplaceMemoryAndCallOld() throws Throwable{
        long hotspotClassHandle = getKlass(agentDEF.class);
        long systemClassHandle = getKlass(System.class);


        String methodType = hasType("methodOopDesc")?"methodOopDesc":"Method";

        long systemMethodHandle = getMethodByKlass(systemClassHandle,"getProperty",getMethodSignature(System.class.getMethod("getProperty", String.class)));

        long hotspotMethodHandle = getMethodByKlass(hotspotClassHandle,"getProperty",getMethodSignature(agentDEF.class.getMethod("getProperty", String.class)));


        System.out.println("Hotspot Class Address : 0x" + Long.toHexString(hotspotClassHandle));
        System.out.println("System Class Address : 0x" + Long.toHexString(systemClassHandle));
        System.out.println("被hook的方法地址 Runtime->exit : 0x" + Long.toHexString(systemMethodHandle));
        //第一个参数指向this
        System.out.println("hook的方法地址 Hotspot->exit : 0x" + Long.toHexString(hotspotMethodHandle));


        String constMethodType =  hasType("ConstMethod")?"ConstMethod":"constMethodOopDesc";
        long methodIdField = getFieldOffset(constMethodType,hasField(constMethodType,"_method_idnum")?"_method_idnum":"_method_index");
        long _constMethodField = getFieldOffset(methodType,"_constMethod");

        long targetConstMethod = unsafe.getAddress(systemMethodHandle + _constMethodField);
        long newConstMethod = unsafe.getAddress(hotspotMethodHandle + _constMethodField);

        short hotspot_method_idnum =  unsafe.getShort(newConstMethod + methodIdField);
        short system_method_idnum =  unsafe.getShort(newConstMethod + methodIdField);



        typeSwap(methodType,hotspotMethodHandle,systemMethodHandle);

        unsafe.putShort(targetConstMethod + methodIdField,hotspot_method_idnum);
        unsafe.putShort(newConstMethod + methodIdField,system_method_idnum);

        String result = System.getProperty("java.home");
        System.out.println("调用被hook方法 System.getProperty(\"java.home\") 返回 : " + result);
        System.out.println("调用原方法 HotSpot.getProperty(\"java.home\") 返回 : " + getProperty("java.home"));

        if ("hacker".equals(result)){
            System.out.println("成功Hook!");
        }else {
            System.out.println("Hook失败!");
        }

    }

    /**
     * 直接替换目标方法的Bytecode
     * */
    public static void testReplaceBytecode() throws Throwable{
        long hotspotClassHandle = getKlass(HotSpot.class);


        String methodType = hasType("methodOopDesc")?"methodOopDesc":"Method";
        String constMethodType = hasType("constMethodOopDesc")?"constMethodOopDesc":"ConstMethod";


        long hotspotMethodHandle = getMethodByKlass(hotspotClassHandle,"testCall",getMethodSignature(HotSpot.class.getMethod("testCall", long.class)));


        System.out.println("Hotspot Class Address : 0x" + Long.toHexString(hotspotClassHandle));
        System.out.println("被修改的方法地址 Hotspot->testCall : 0x" + Long.toHexString(hotspotMethodHandle));

        long constMethodOopDescSize = sizeOf(constMethodType);

        long _constMethodField = getFieldOffset(methodType,"_constMethod");
        long _codeField = getFieldOffset(methodType,"_code");
        long _code_sizeField = getFieldOffset(constMethodType,"_code_size");

        long _from_compiled_entry_entryField = getFieldOffsetOr(methodType,"_from_compiled_entry","_from_compiled_code_entry_point");

//        long _max_localsField = getFieldOffset(constMethodType,"_max_locals");
//        long _max_stackField = getFieldOffset(constMethodType,"_max_stack");


        long hotspotConstMethodHandle = unsafe.getAddress(hotspotMethodHandle + _constMethodField);

        // lload_0
        // lreturn
        byte[] bytecode = hexToByte("b2ad");

        int bytecodeSize = unsafe.getShort(hotspotConstMethodHandle + _code_sizeField) & 0xffff;

        System.out.println("目标方法bytecode大小: " + bytecodeSize);

        if (bytecodeSize < bytecode.length){
            System.out.println("目标方法bytecode太小 可以尝试复制ConstMethod的内存 然后扩大_code_size字段的大小");
            //hotspotConstMethodHandle = github.beichendream.AsmUtil.allocateAndUpdateBytecodeSize(hotspotConstMethodHandle,bytecode.length);
            //unsafe.putAddress(hotspotMethodHandle + _constMethodField,hotspotConstMethodHandle);
            //// github.beichendream.AsmUtil.replaceBodyAndFixConstantPool(hotspotConstMethodHandle,"return $1;");
            return;
        }

        for (int i = 0; i < bytecode.length; i++) {
            unsafe.putByte(hotspotConstMethodHandle + constMethodOopDescSize + i,bytecode[i]);
        }

        //取消jit编译  让修改后的代码立即生效
        unsafe.putAddress(hotspotMethodHandle + _codeField,0);
        unsafe.putAddress(hotspotMethodHandle + _from_compiled_entry_entryField,0);

//        unsafe.putShort(hotspotConstMethodHandle + _code_sizeField, (short) 2);

        //如果方法堆栈大小与目标相差太大记得修改堆栈大小
//        unsafe.putShort(hotspotConstMethodHandle + _max_stackField, (short) 2);
//        unsafe.putShort(hotspotConstMethodHandle + _max_localsField, (short) 2);

        //目标方法原本应该返回123456 现在我们修改了bytecode 它会返回我们传入的参数
        // 新的bytecode
        // lload_0
        // lreturn
        long result = testCall(778899);

        System.out.println("testCall result : " + result);

        if (778899 == result){
            System.out.println("修改bytecode成功");
        }
    }

    /**
     * 直接替换目标方法的_from_compiled_entry
     * */
    public static void testRunShellcode()throws Exception{
        String methodType = hasType("methodOopDesc")?"methodOopDesc":"Method";
        long instanceKlass = getKlass(HotSpot.class);
        long methodHandle = getMethodByKlass(instanceKlass,HotSpot.class.getMethod("runShellcodeFunctionByJit"));
        long _codeField = getFieldOffset(methodType,"_code");

        //nmethod
        long _code = 0;
        long _from_compiled_entry_entryField = getFieldOffsetOr(methodType,"_from_compiled_entry","_from_compiled_code_entry_point");

        long _from_compiled_entry = 0;

        byte[] shellcode = new byte[]{(byte) 0xc3};

        for (int i = 0; i < 20000; i++) {
            _code = unsafe.getAddress(methodHandle + _codeField);
            if (_code != 0){
                _from_compiled_entry = unsafe.getAddress(methodHandle + _from_compiled_entry_entryField);

                System.out.println("已被Jit优化现在执行shellcode  shellcodeAddress: 0x" + Long.toHexString(_from_compiled_entry));
                break;
            }else {
                System.out.println("执行次数: "+i+" 未被Jit生成优化汇编 开始下一次执行");
            }
            runShellcodeFunctionByJit();
        }
        if (_from_compiled_entry == 0 || _code == 0){
            System.out.println("_from_compiled_entry == 0x"+Long.toHexString(_from_compiled_entry)+" || _code == 0x"+Long.toHexString(_from_compiled_entry));
            System.out.println("_from_compiled_entry == 0 || _code == 0");
            return;
        }

        long codeSize = getCompiledCodeSize(_code);

        System.out.println("Java方法编译后的二进制代码大小 : " + codeSize);

        if (codeSize < shellcode.length){
            System.out.println("shellcode长度小于Java方法编译后的二进制代码大小 无法执行shellcode 请尝试增大方法体大小");
            return;
        }

        memCpy(shellcode,_from_compiled_entry,0,shellcode.length);
        int result = runShellcodeFunctionByJit();
        System.out.println("shellcode result : " + result);
        if (result != 123){
            System.out.println("运行shellcode成功");
        }
    }

    public static void aaa(){
        System.mapLibraryName("aaa");
    }

    public static void main(String[] args) throws Throwable{



//        testReplaceBytecode();
//        testReplaceAddress();
//        testReplaceMemory();
//        testReplaceMemoryAndCallOld();
//        testReplaceNativeAddressAndCallOld();
//        testRunShellcode();
        testReplaceBytecode();



    }



    public static String byteArrayToHexPrefix(byte[] bytes) {
        String strHex = "";
        StringBuilder sb = new StringBuilder();
        for (int n = 0; n < bytes.length; n++) {
            strHex = Integer.toHexString(bytes[n] & 0xFF);
            sb.append((strHex.length() == 1) ? "0" + strHex : strHex); // 每个字节由两个字符表示，位数不够，高位补0
        }
        return sb.toString().trim();
    }
    public static byte[] hexToByte(String hexStr) {
        byte[] data = hexStr.getBytes();
        int len = data.length;
        byte[] out = new byte[len / 2];
        for (int i = 0, j = 0; j < len; i++) {
            int f =  Character.digit(data[j++], 16) << 4;
            f |= Character.digit(data[j++], 16);
            out[i] = (byte)(f & 0xFF);
        }
        return out;
    }
}
