package com.example.javapoet;

import com.example.annotation_lib.HelloAnnotation;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * ********************************
 * 项目名称：
 *
 * @Author yangbinbing
 * 邮箱： 963416867@qq.com
 * 创建时间：  20:29
 * 用途
 * ********************************
 */
@AutoService(Processor.class)
public class HelloProcessor extends AbstractProcessor {

    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            if (annotation.getQualifiedName().toString().equals(HelloAnnotation.class.getCanonicalName())) {
                TypeSpec build = TypeSpec.classBuilder("HelloWorld")
                        .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                        .addMethod(getMethodSpec("hello1", "aaa"))
                        .addMethod(getMethodSpec("hello2", "bbb"))
                        .addMethod(getMethodSpec("hello3", "ccc"))
                        .build();
                JavaFile builder = JavaFile.builder("com.example.javapoetforandroid", build)
                        .build();
                try {
                    builder.writeTo(filer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    private  static MethodSpec  getMethodSpec(String methodStr,String returnStr){

        return MethodSpec.methodBuilder(methodStr)
                .addModifiers(Modifier.PUBLIC,Modifier.STATIC)
                .returns(String.class)
                .addStatement("return $S",returnStr)
                .build();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(HelloAnnotation.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
