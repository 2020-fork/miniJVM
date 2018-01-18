/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.jnibuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.mini.jnibuilder.Build_glah_h_2_GL_java.isTypes;

/**
 *
 * @author gust
 */
public class Build_NK_java_2_nkjni_c {

    public static void main(String[] args) {
        Build_NK_java_2_nkjni_c gt = new Build_NK_java_2_nkjni_c();
        gt.buildC();
    }

    String[] path = {"src/org/mini/nk/NK.java", "org_mini_nk_NK_", "org/mini/nk/NK", "./nkjni.c"};

    String FUNC_BODY_TEMPLATE
            = //
            "int ${PKG_NAME}${METHOD_NAME}(Runtime *runtime, Class *clazz) {\n"
            + "    JniEnv *env = runtime->jnienv;\n"
            + "    s32 pos = 0;\n"
            + "    \n${GET_VAR}\n"
            + "    ${RETURN_TYPE}${METHOD_NAME}(${NATIVE_ARGV});\n"
            + "    ${PUSH_RESULT}\n"
            + "    ${RELEASE_MEM}\n"
            + "    return 0;\n"
            + "}\n\n";
    String PKG_NAME = "${PKG_NAME}";
    String PKG_PATH = "${PKG_PATH}";
    String METHOD_NAME = "${METHOD_NAME}";
    String GET_VAR = "${GET_VAR}";
    String RETURN_TYPE = "${RETURN_TYPE}";
    String NATIVE_ARGV = "${NATIVE_ARGV}";
    String JAVA_ARGV = "${JAVA_ARGV}";
    String JAVA_RETURN = "${JAVA_RETURN}";
    String PUSH_RESULT = "${PUSH_RESULT}";
    String RELEASE_MEM = "${RELEASE_MEM}";

    String VOID = "void";

    String FUNC_TABLE_TEMPLATE = "{\"${PKG_PATH}\",  \"${METHOD_NAME}\",  \"(${JAVA_ARGV})${JAVA_RETURN}\",  ${PKG_NAME}${METHOD_NAME}},";

    void buildC() {
        BufferedReader br = null;
        BufferedWriter bw = null;
        List<String> funcTable = new ArrayList();
        try {
            File ifile = new File(path[0]);
            br = new BufferedReader(new FileReader(ifile));
            System.out.println("open input file:" + ifile.getAbsolutePath());
            File ofile = new File(path[3]);
            bw = new BufferedWriter(new FileWriter(ofile));
            System.out.println("open output file:" + ofile.getAbsolutePath());
            String line, whole;
            String header = "public static native";
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                line = line.trim();
                whole = new String(line.getBytes());
                if (line.startsWith(header)) {

                    String nativeArgvType = line.substring(line.indexOf("//") + 2, line.lastIndexOf("//")).trim();
                    String nativeReurnType = line.substring(line.lastIndexOf("//") + 2).trim();
                    String[] nativeArgvs = nativeArgvType.split(",");
                    line = line.substring(header.length()).trim();
                    String returnType = line.substring(0, line.indexOf(' ')).trim();
                    line = line.substring(returnType.length()).trim();
                    String methodName = line.substring(0, line.indexOf('('));
                    line = line.substring(line.indexOf('(') + 1, line.indexOf(')')).trim();
                    String[] argvs = line.split(",");
                    //
                    String output = new String(FUNC_BODY_TEMPLATE.getBytes());
                    output = output.replace(PKG_NAME, path[1]);
                    output = output.replace(METHOD_NAME, methodName);
                    String funcTableLine = new String(FUNC_TABLE_TEMPLATE.getBytes());
                    funcTableLine = funcTableLine.replace(PKG_NAME, path[1]);
                    funcTableLine = funcTableLine.replace(METHOD_NAME, methodName);
                    funcTableLine = funcTableLine.replace(PKG_PATH, path[2]);

                    //process return 
                    String returnCode = "", pushCode = "", javaReturnCode = "", releaseMemCode = "";
                    boolean nativeReturnIsPointer = nativeReurnType.lastIndexOf('*') == nativeReurnType.length();//最后一个是*号
                    if (nativeReturnIsPointer) {
                        nativeReurnType = nativeReurnType.substring(0, nativeReurnType.length() - 1);
                    }
                    if (!VOID.equals(returnType)) {
                        if ("int".equals(returnType)) {
                            returnCode = "s32 ret_value = (s32)";
                            pushCode = "env->push_int(runtime->stack, ret_value);";
                            javaReturnCode = "I";
                        } else if ("float".equals(returnType)) {
                            returnCode = "f64 ret_value = (f64)";
                            pushCode = "env->push_float(runtime->stack, ret_value);";
                            javaReturnCode = "D";
                        } else if ("byte".equals(returnType)) {
                            returnCode = "s8 ret_value = (s8)";
                            pushCode = "env->push_int(runtime->stack, ret_value);";
                            javaReturnCode = "B";
                        } else if ("short".equals(returnType)) {
                            returnCode = "s16 ret_value = (s16)";
                            pushCode = "env->push_int(runtime->stack, ret_value);";
                            javaReturnCode = "S";
                        } else if ("boolean".equals(returnType)) {
                            returnCode = "u8 ret_value = (u8)";
                            pushCode = "env->push_int(runtime->stack, ret_value);";
                            javaReturnCode = "Z";
                        } else if ("long".equals(returnType)) {
                            returnCode = "s64 ret_value = (s64)(intptr_t)";
                            pushCode = "env->push_long(runtime->stack, ret_value);";
                            javaReturnCode = "J";
                        } else if ("double".equals(returnType)) {
                            returnCode = "f64 ret_value = (f64)";
                            pushCode = "env->push_double(runtime->stack, ret_value);";
                            javaReturnCode = "F";
                        } else if ("String".equals(returnType)) {
                            if (nativeReturnIsPointer) {
                                returnCode = "c8* _cstr = (c8*)";
                            } else {
                                returnCode = "c8* _cstr = (c8*)&";
                            }
                            pushCode = "if (cstr) {\n"
                                    + "        Instance *jstr = createJavaString(runtime, _cstr);\n"
                                    + "        env->push_ref(runtime->stack, jstr);\n"
                                    + "    } else {\n"
                                    + "        env->push_ref(runtime->stack, NULL);\n"
                                    + "    }";
                            javaReturnCode = "Ljava/lang/String;";
                        } else if (returnType.contains("[]")) {
                            String cType = "", jvmType = "", jvmDesc="";
                            if ("long[]".equals(returnType)) {
                                cType = "s64";
                                jvmType = "DATATYPE_LONG";
                                jvmDesc = "[J";
                            } else if ("int[]".equals(returnType)) {
                                cType = "s32";
                                jvmType = "DATATYPE_INT";
                                jvmDesc = "[I";
                            } else if ("float[]".equals(returnType)) {
                                cType = "f32";
                                jvmType = "DATATYPE_INT";
                                jvmDesc = "[F";
                            } else if ("byte[]".equals(returnType)) {
                                cType = "c8";
                                jvmType = "DATATYPE_LONG";
                                jvmDesc = "[B";
                            } else {
                                System.out.println(" " + lineNo + " return type:" + returnType + " in :" + whole);
                            }
                            
                            //impl
                            if (nativeReturnIsPointer) {
                                returnCode = cType + "* _ptr_re_val = (" + cType + "*)";
                            } else {
                                returnCode = cType + "* _ptr_re_val = (" + cType + "*)&";
                            }
                            pushCode = "if (cstr) {\n"
                                    + "        s32 bytes=sizeof(" + nativeReurnType + ")\n"
                                    + "        s32 j_t_bytes=sizeof(" + cType + ")\n"
                                    + "        Instance *_arr = env->jarray_create(bytes / j_t_bytes, " + jvmType + ", NULL);\n"
                                    + "        memcpy(_arr->arr_body, _ptr_re_val,bytes);\n"
                                    + "        env->push_ref(runtime->stack, _arr);\n"
                                    + "    } else {\n"
                                    + "        env->push_ref(runtime->stack, NULL);\n"
                                    + "    }";
                            javaReturnCode = jvmDesc;
                        } else {
                            System.out.println(" " + lineNo + " return type:" + returnType + " in :" + whole);
                        }
                    } else {
                        javaReturnCode = "V";
                    }
                    output = output.replace(RETURN_TYPE, returnCode);
                    output = output.replace(PUSH_RESULT, pushCode);
                    funcTableLine = funcTableLine.replace(JAVA_RETURN, javaReturnCode);

                    //process body
                    String varCode = "";
                    String nativeArgvCode = "";
                    String javaArgvCode = "";
                    for (int i = 0, nativei = 0; i < argvs.length; i++, nativei++) {
                        String argv = argvs[i].trim();
                        if (argv.length() == 0) {
                            continue;
                        }
                        String[] tmps = argv.trim().split(" ");
                        String argvType = tmps[0].trim();
                        String argvName = tmps[1].trim();
                        if (nativei >= nativeArgvs.length) {
                            int debug = 1;
                        }

                        //
                        nativeArgvCode += nativeArgvCode.length() > 0 ? ", " : "";
                        nativeArgvs[nativei] = "(" + nativeArgvs[nativei] + ")";
                        String[] POINTER_TYPE = {"(GLsync)", "(GLDEBUGPROC)", "(GLDEBUGPROCKHR)"};
                        if (isTypes(POINTER_TYPE, nativeArgvs[nativei])) {
                            nativeArgvs[nativei] += "(intptr_t)";
                        }
                        nativeArgvCode += nativeArgvs[nativei];
                        if ("int".equals(argvType)) {
                            varCode += "    s32 " + argvName + " = env->localvar_getInt(runtime, pos++);\n";
                            nativeArgvCode += argvName;
                            javaArgvCode += "I";
                        } else if ("short".equals(argvType)) {
                            varCode += "    s32 " + argvName + " = env->localvar_getInt(runtime, pos++);\n";
                            nativeArgvCode += argvName;
                            javaArgvCode += "S";
                        } else if ("byte".equals(argvType)) {
                            varCode += "    s32 " + argvName + " = env->localvar_getInt(runtime, pos++);\n";
                            nativeArgvCode += argvName;
                            javaArgvCode += "B";
                        } else if ("boolean".equals(argvType)) {
                            varCode += "    s32 " + argvName + " = env->localvar_getInt(runtime, pos++);\n";
                            nativeArgvCode += argvName;
                            javaArgvCode += "Z";
                        } else if ("long".equals(argvType)) {
                            varCode += "    s64 " + argvName + " = getParaLong(runtime, pos);pos += 2;\n";
                            nativeArgvCode += argvName;
                            javaArgvCode += "J";
                        } else if ("float".equals(argvType)) {
                            varCode += "    Int2Float " + argvName + ";" + argvName + ".i = env->localvar_getInt(runtime, pos++);\n";
                            nativeArgvCode += argvName + ".f";
                            javaArgvCode += "F";
                        } else if ("double".equals(argvType)) {
                            varCode += "    Long2Double " + argvName + ";" + argvName + ".l = getParaLong(runtime, pos);pos += 2;\n";
                            nativeArgvCode += argvName + ".d";
                            javaArgvCode += "D";
                        } else if ("String".equals(argvType)) {
                            varCode += "    Instance *" + argvName + " = env->localvar_getRefer(runtime, pos++);\n";
                            varCode += "    __refer ptr_" + argvName + " = NULL;\n";
                            varCode += "    if(" + argvName + "){\n";
                            varCode += "        Utf8String *u_" + argvName + " = env->utf8_create();\n";
                            varCode += "        env->jstring_2_utf8(" + argvName + ", u_" + argvName + ");\n";
                            varCode += "        ptr_" + argvName + " = env->utf8_cstr(u_" + argvName + ");\n";
                            varCode += "    }\n";
                            nativeArgvCode += "(ptr_" + argvName + ")";
                            releaseMemCode += "env->utf8_destory(source_utf8);";
                            javaArgvCode += "Ljava/lang/String;";
                        } else if (argvType.indexOf("[]") > 0 || "Object".equals(argvType)) {
                            varCode += "    Instance *" + argvName + " = env->localvar_getRefer(runtime, pos++);\n";
                            varCode += "    __refer ptr_" + argvName + " = NULL;\n";
                            varCode += "    if(" + argvName + "){\n";
                            varCode += "        ptr" + argvName + " = " + argvName + "->arr_body" + ";\n";
                            varCode += "    }\n";
                            nativeArgvCode += "(ptr" + i + ")";
                            if (argvType.startsWith("int")) {
                                javaArgvCode += "[II";
                            } else if (argvType.startsWith("short")) {
                                javaArgvCode += "[SI";
                            } else if (argvType.startsWith("byte")) {
                                javaArgvCode += "[BI";
                            } else if (argvType.startsWith("long")) {
                                javaArgvCode += "[JI";
                            } else if (argvType.startsWith("float")) {
                                javaArgvCode += "[FI";
                            } else if (argvType.startsWith("double")) {
                                javaArgvCode += "[DI";
                            } else if (argvType.startsWith("String")) {
                                javaArgvCode += "Ljava/lang/String;";
                            } else if (argvType.startsWith("Object")) {
                                javaArgvCode += "Ljava/lang/Object;";
                            } else if (argvType.startsWith("boolean")) {
                                javaArgvCode += "[ZI";
                            } else {
                                System.out.println(" " + lineNo + " array type:" + returnType + " in :" + whole);
                            }
                        } else {
                            System.out.println(" " + lineNo + " argv type:" + returnType + " in :" + whole);
                        }
                    }
                    output = output.replace(GET_VAR, varCode);
                    output = output.replace(NATIVE_ARGV, nativeArgvCode);
                    output = output.replace(RELEASE_MEM, releaseMemCode);
                    bw.write(output);

                    funcTableLine = funcTableLine.replace(JAVA_ARGV, javaArgvCode);
                    funcTable.add(funcTableLine);
                }
            }
            bw.write("\n\n\n");
            for (String s : funcTable) {
                bw.write(s + "\n");
            }
        } catch (Exception ex) {
            Logger.getLogger(Build_NK_java_2_nkjni_c.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
                bw.close();
            } catch (IOException ex) {
                Logger.getLogger(Build_NK_java_2_nkjni_c.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("success.");
    }

}
