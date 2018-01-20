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
import static org.mini.jnibuilder.Util.isPointer;
import static org.mini.jnibuilder.Util.isTypes;

/**
 *
 * @author gust
 */
public class GL_java_2_c {

    public static void main(String[] args) {
        GL_java_2_c gt = new GL_java_2_c();
        gt.buildC();
    }

    String[] path = {"src/org/mini/gl/GL.java", "org_mini_gl_GL_", "org/mini/gl/GL", "../jni_gl.c"};

    String[] ignore_list = {"",
        "",};

    String C_BODY_HEADER
            =//
            "#include <stdio.h>\n"
            + "#include <string.h>\n"
            + "#include \"deps/include/glad/glad.h\"\n"
            + "#include \"deps/include/GLFW/glfw3.h\"\n"
            + "#include \"deps/include/linmath.h\"\n"
            + "\n"
            + "#include \"../mini_jvm/jvm/jvm.h\"\n"
            + "#include \"jni_gui.h\"\n"
            + "\n";

    String TOOL_FUNC
            = //
            "s32 count_GLFuncTable() {\n"
            + "    return sizeof(method_gl_table) / sizeof(java_native_method);\n"
            + "}\n"
            + "\n"
            + "__refer ptr_GLFuncTable() {\n"
            + "    return &method_gl_table[0];\n"
            + "}";
    String FUNC_TABLE_HEADER = "static java_native_method method_gl_table[] = {\n\n";
    String FUNC_TABLE_FOOTER = "};\n\n";

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
            bw.write(C_BODY_HEADER);
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
                    String funcBodyCode = new String(FUNC_BODY_TEMPLATE.getBytes());
                    funcBodyCode = funcBodyCode.replace(PKG_NAME, path[1]);
                    funcBodyCode = funcBodyCode.replace(METHOD_NAME, methodName);
                    String funcTableLine = new String(FUNC_TABLE_TEMPLATE.getBytes());
                    funcTableLine = funcTableLine.replace(PKG_NAME, path[1]);
                    funcTableLine = funcTableLine.replace(METHOD_NAME, methodName);
                    funcTableLine = funcTableLine.replace(PKG_PATH, path[2]);

                    //process return 
                    String returnCode = "", pushCode = "", javaReturnCode = "", releaseMemCode = "";
                    boolean nativeReturnIsPointer = isPointer(nativeReurnType);//最后一个是*号

                    if (!VOID.equals(returnType)) {
                        if ("int".equals(returnType)) {
                            returnCode = nativeReurnType + " _re_val = ";
                            pushCode += "s32 ret_value = *((s32*)&_re_val);";
                            pushCode += "env->push_int(runtime->stack, ret_value);";
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
                            returnCode = nativeReurnType + " _re_val = ";
                            pushCode += "s64 ret_value = *((s64*)&_re_val);";
                            pushCode += "env->push_long(runtime->stack, ret_value);";
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
                            pushCode = "if (_cstr) {\n"
                                    + "        Instance *jstr = env->jstring_create_cstr(_cstr, runtime);\n"
                                    + "        env->push_ref(runtime->stack, jstr);\n"
                                    + "    } else {\n"
                                    + "        env->push_ref(runtime->stack, NULL);\n"
                                    + "    }";
                            javaReturnCode = "Ljava/lang/String;";
                        } else if (returnType.contains("[]")) {
                            String cType = "", jvmType = "", jvmDesc = "";
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
                                jvmType = "DATATYPE_FLOAT";
                                jvmDesc = "[F";
                            } else if ("byte[]".equals(returnType)) {
                                cType = "c8";
                                jvmType = "DATATYPE_BYTE";
                                jvmDesc = "[B";
                            } else {
                                System.out.println(" " + lineNo + " return type:" + returnType + " in :" + whole);
                            }

                            //impl
                            String entryType = nativeReurnType;//计算实体字节数，不能算指针大小
                            returnCode = nativeReurnType + " _re_val = ";
                            if (nativeReturnIsPointer) {
                                pushCode += cType + "* _ptr_re_val = (" + cType + "*)_re_val;\n";
                                entryType = nativeReurnType.substring(0, nativeReurnType.length() - 1);
                            } else {
                                pushCode += cType + "* _ptr_re_val = (" + cType + "*)&_re_val;\n";
                            }
                            pushCode += "    if (_ptr_re_val) {\n"
                                    + "        s32 bytes = sizeof(" + entryType + ");\n"
                                    + "        s32 j_t_bytes = sizeof(" + cType + ");\n"
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
                    funcBodyCode = funcBodyCode.replace(RETURN_TYPE, returnCode);
                    funcBodyCode = funcBodyCode.replace(PUSH_RESULT, pushCode);
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
                        String curArgvType = "(" + nativeArgvs[nativei] + ")";
                        String curArgvName = "";

                        if ("int".equals(argvType)) {
                            varCode += "    s32 " + argvName + " = env->localvar_getInt(runtime, pos++);\n";
                            curArgvName = argvName;
                            javaArgvCode += "I";
                            if (!isPointer(nativeArgvs[nativei])) {
                                curArgvType = "(" + nativeArgvs[nativei] + ")";
                            }
                        } else if ("short".equals(argvType)) {
                            varCode += "    s32 " + argvName + " = env->localvar_getInt(runtime, pos++);\n";
                            curArgvName = argvName;
                            javaArgvCode += "S";
                        } else if ("byte".equals(argvType)) {
                            varCode += "    s32 " + argvName + " = env->localvar_getInt(runtime, pos++);\n";
                            curArgvName = argvName;
                            javaArgvCode += "B";
                        } else if ("boolean".equals(argvType)) {
                            varCode += "    s32 " + argvName + " = env->localvar_getInt(runtime, pos++);\n";
                            curArgvName = argvName;
                            javaArgvCode += "Z";

                        } else if ("long".equals(argvType)) {
                            varCode += "    intptr_t " + argvName + " = env->localvar_getLong_2slot(runtime, pos);pos += 2;\n";
                            curArgvName = argvName;
                            javaArgvCode += "J";
                        } else if ("float".equals(argvType)) {
                            varCode += "    Int2Float " + argvName + ";" + argvName + ".i = env->localvar_getInt(runtime, pos++);\n";
                            curArgvName = argvName + ".f";
                            javaArgvCode += "F";
                        } else if ("double".equals(argvType)) {
                            varCode += "    Long2Double " + argvName + ";" + argvName + ".l = env->localvar_getLong_2slot(runtime, pos);pos += 2;\n";
                            curArgvName = argvName + ".d";
                            javaArgvCode += "D";
                        } else if ("String".equals(argvType)) {
                            varCode += "    Instance *" + argvName + " = env->localvar_getRefer(runtime, pos++);\n";
                            varCode += "    __refer ptr_" + argvName + " = NULL;\n";
                            varCode += "    Utf8String *u_" + argvName + ";\n";
                            varCode += "    if(" + argvName + "){\n";
                            varCode += "        u_" + argvName + " = env->utf8_create();\n";
                            varCode += "        env->jstring_2_utf8(" + argvName + ", u_" + argvName + ");\n";
                            varCode += "        ptr_" + argvName + " = env->utf8_cstr(u_" + argvName + ");\n";
                            varCode += "    }\n";
                            curArgvName = "(ptr_" + argvName + ")";
                            releaseMemCode += "env->utf8_destory(u_" + argvName + ");";
                            javaArgvCode += "Ljava/lang/String;";
                        } else if ("String[]".equals(argvType) || "String...".equals(argvType)) {
                            varCode += "    Instance *" + argvName + " = env->localvar_getRefer(runtime, pos++);\n";
                            varCode += "    CStringArr *ptr_" + argvName + " = NULL;\n";
                            varCode += "    if(" + argvName + "){\n";
                            varCode += "        ptr_" + argvName + " = env->cstringarr_create(" + argvName + ");\n";
                            varCode += "    }\n";
                            curArgvName = "(ptr_" + argvName + "->arr_body)";
                            if ("String...".equals(argvType)) {
                                curArgvType = "/*todo Despair for runtime parse unlimited para*/";
                            }
                            releaseMemCode += "env->cstringarr_destory(ptr_" + argvName + ");";
                            javaArgvCode += "[Ljava/lang/String;";

                        } else if ("Object[]".equals(argvType) || "Object...".equals(argvType)) {
                            varCode += "    Instance *" + argvName + " = env->localvar_getRefer(runtime, pos++);\n";
                            varCode += "    ReferArr *ptr_" + argvName + " = NULL;\n";
                            varCode += "    if(" + argvName + "){\n";
                            varCode += "        ptr_" + argvName + " = env->referarr_create(" + argvName + ");\n";
                            varCode += "    }\n";
                            curArgvName = "(ptr_" + argvName + "->arr_body)";
                            if ("Object...".equals(argvType)) {
                                curArgvType = "/*todo Despair for runtime parse unlimited para*/";
                            }
                            releaseMemCode += "env->referarr_destory(ptr_" + argvName + ");";
                            javaArgvCode += "[Ljava/lang/Object;";

                        } else if (argvType.indexOf("[]") > 0 || argvType.indexOf("Object") >= 0) {
                            varCode += "    Instance *" + argvName + " = env->localvar_getRefer(runtime, pos++);\n";
                            varCode += "    int offset_" + argvName + " = env->localvar_getInt(runtime, pos++);\n";
                            varCode += "    __refer ptr_" + argvName + " = NULL;\n";
                            varCode += "    if(" + argvName + "){\n";
                            varCode += "        ptr_" + argvName + " = " + argvName + "->arr_body + offset_" + argvName + ";\n";
                            varCode += "    }\n";
                            if (!isPointer(nativeArgvs[nativei])) {
                                //curArgvType = "*(" + nativeArgvs[nativei] + "*)";
                            }
                            curArgvName = "(ptr_" + argvName + ")";
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
                            } else if (argvType.startsWith("Object")) {
                                javaArgvCode += "Ljava/lang/Object;I";
                            } else if (argvType.startsWith("boolean")) {
                                javaArgvCode += "[ZI";
                            } else {
                                System.out.println(" " + lineNo + " array type:" + returnType + " in :" + whole);
                            }
                            i++;
                        } else {
                            System.out.println(" " + lineNo + " argv type:" + returnType + " in :" + whole);
                        }
                        nativeArgvCode += curArgvType + curArgvName;
                    }
                    funcBodyCode = funcBodyCode.replace(GET_VAR, varCode);
                    funcBodyCode = funcBodyCode.replace(NATIVE_ARGV, nativeArgvCode);
                    funcBodyCode = funcBodyCode.replace(RELEASE_MEM, releaseMemCode);
                    funcTableLine = funcTableLine.replace(JAVA_ARGV, javaArgvCode);

                    if (!isTypes(ignore_list, methodName)) {
                        bw.write(funcBodyCode);
                        funcTable.add(funcTableLine);
                    }

                }
            }
            bw.write("\n\n\n");
            bw.write(FUNC_TABLE_HEADER);
            for (String s : funcTable) {
                bw.write(s + "\n");
            }

            bw.write(FUNC_TABLE_FOOTER);
            bw.write(TOOL_FUNC);
        } catch (Exception ex) {
            Logger.getLogger(NK_java_2_c.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
                bw.close();
            } catch (IOException ex) {
                Logger.getLogger(NK_java_2_c.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("success.");
    }

}
