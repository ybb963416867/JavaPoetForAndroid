# Android(Java)代码生成技术--JavaPoet自动实现依赖注入

## 前言

相信大家在平常的开发中，依赖注入这个词没少听说过吧，比如做安卓开发的，使用的Butterknife、Greendao等等第三方库，都是使用的一种叫做编译期代码即时生成的技术，然后我们可以利用编译生成的类来辅助我们的开发，减少我们的工作量，这个技术听上去感觉挺高大上的，编译期间代码生成，这该怎么做到啊，好像从来没有从哪听说编译还能生成代码的，下面让我们来看看这门神奇的技术！

## 编译期代码生成原理

首先在这之前，我们可能或多或少了解到一个叫JavaPoet的技术，首先我们从它开始，我们来到它的源码，你会发现它好像并没有做什么高深的事情，总共才十几个类，而且大多数只是做了一些类的封装，提供一些接口方便使用，然后还提供了一个工具类供我们使用，如图，它的源码仅仅只有下面几个类而已

![img](https://img-blog.csdn.net/20180724093300756?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2hxOTQyODQ1MjA0/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

这时候就迷了，难道编译期生成代码不是这个库帮我们完成的吗？
准确的说确实不是的，那它的功能是什么呢？它其实只是完成了我们所说的一半的功能，就是代码生成，而且是简单易懂的代码生成，因为它做了封装，对一些常用的代码生成需求基本都提供了相应的方法，大大减少了代码复杂度。那和我们想要的需求来比，少了一个编译期间生成，那这个怎么做呢？

其实要实现编译期间做一些事情，这个工作Java已经帮我们做好了，我们先来看一个类AbstractProcessor，这个类可能平常不会用到，但是也没关系，我们简单了解一下它，首先你可以把它理解为一个抽象的注解处理器，它位于javax.annotation.processing.AbstractProcessor;下面，是用于在编译时扫描和处理注解的类，我们要实现类似ButterKnife这样的功能，首先定义一个自己的注解处理器，然后继承它即可，如下

```
@AutoService(Processor.class)
public class HelloProcessor extends AbstractProcessor {


}
```

可以看到我们需要实现一个叫process的抽象方法，这个方法里的内容就会在编译期间执行。
然后，怎么让jvm在编译期间调用我们自己写的这个注解处理器呢，有一个快捷办法就是使用谷歌的开源库auto，然后使用它提供的AutoService注解来实现，另外一种办法就是自己手动去创建指定的文件夹，然后配置我们的注解处理器的路径。

做完上述工作后，在编译时，jvm就会扫描到所有的AbstractProcessor 的实现类，这里也就是MyProcessor ，然后调用实现实现类的process方法，执行相应的操作，然后我们生成代码的工作就可以写在process这里，然后具体的生成代码的方法再借助JavaPoet工具来简化操作。
因为我们生成代码的工具是在编译期间执行的，最后生成的java代码会和普通的java代码一起编译，当做普通的类来使用，所以不会影响到最后程序运行时的性能，最多只是编译速度慢了点，因为要生成额外的类和代码。

至此我们知道了怎么在编译期间生成代码，基本的实现思路也有了，不过还有一个问题存在。

怎么使用注解处理器来处理一个注解，从而实现类似@BindView的效果呢？首先我们当然要自定义一个注解，具体自定义注解不了解的可以去学下，挺简单的，内容不多，由于不是本章重点，所以不作过多说明，当然也可以就在文末的demo链接里下载源码学习，在定义了一个注解之后，我们先看到我们的MyProcessor 注解处理器的 process() 方法的annotations 参数，这个参数就是在编译的时候，用来存储扫描到的所有的非元注解（非元注解也就是自定义注解，元注解一共四个，除了元注解，剩下的就是自定义注解），然后我们遍历这个集合，取到我们自定义注解时，再执行相应的逻辑。具体的代码如下，其中XXXAnnotation就是你的自定义注解的名称

```
for (TypeElement element : annotations) {
    if (element.getQualifiedName().toString().equals(XXXAnnotation.class.getCanonicalName())) {
        //执行你的逻辑
    }
}
```

然后我们还需要重写AbstractProcessor 类的getSupportedAnnotationTypes() 方法和getSupportedSourceVersion() 方法，getSupportedAnnotationTypes() 方法用来指定该注解处理器是用来处理哪个注解的，getSupportedSourceVersion() 方法用来指定java版本，一般给值为SourceVersion.latestSupported()。

完成以上工作后，对于自定义注解作用的对象，编译期间就会自动执行相应process() 里的逻辑，比如生成辅助类，这个辅助类其实就可以理解为依赖类，这个编译期间通过注解生成辅助类的过程，就相当于实现了注入。
到这里，一整个依赖注入的思路和实现方法已经全部打通。
下面我们来动手实现一个小例子吧！

## 动手实现

首先我们新建一个Android工程，按照默认的配置就好，新建完毕后，会有个默认的app的module，我们暂且不管它，然后直接新建一个java module，新建方式为file -> New -> New Module
然后选择Java Libary，这里给libary取名为javapoet

然后在module对应的build.gradle下，加入下面两个依赖

```
    implementation 'com.squareup:javapoet:1.11.1'
    implementation 'com.google.auto.service:auto-service:1.0-rc6'
    annotationProcessor 'com.google.auto.service:auto-service:1.0-rc6'
```

第一个依赖是javaPoet的依赖，第二个是Google开源库auto的依赖

最后的样式如下

```
apply plugin: 'java-library'

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation 'com.squareup:javapoet:1.11.1'
    implementation 'com.google.auto.service:auto-service:1.0-rc6'
    annotationProcessor 'com.google.auto.service:auto-service:1.0-rc6'
    implementation project(path: ':libannotation')
}

sourceCompatibility = "7"
targetCompatibility = "7"
```

上面提到让jvm加载我们自己的注解处理器有两种方式，这里我们先试一下用谷歌的这个开源库

这个module用来编写我们的注解处理器的实现类，先放着，待会再写。
继续新建一个java module，我这里取名为annotation_lib，然后在里面新建一个自定义注解，内容如下

```
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface HelloAnnotation {

}
```

@Retention(RetentionPolicy.CLASS)表示我们定义的这个注解会留存在class字节码文件中 ，但是在运行时是没有的，@Target(ElementType.TYPE) 表示我们的这个注解作用对象是类，然后我们看下整个目录的样子，


TestGenerator是一个我写的测试JavaPoet的测试类，可以忽略

现在我们开始写代码，首先在javapoet的build.gradle中加入libannotation module的依赖，如下

```java
implementation project(path: ':annotation_lib')
```

然后在javapoet module中新建HelloProcessor类，用AutoService标注它，然后继承AbstractProcessor方法，重写相应的方法，具体怎么写，原理里解释的比较详细，然后注释里我作了详细说明，如下



```java
package com.example.javapoat_lib;

import com.example.libannotation.HelloAnnotation;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

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
                TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addMethod(getMethodSpec("hello1", "hello"))
                        .addMethod(getMethodSpec("hello2","java"))
                        .addMethod(getMethodSpec("hello3","Poet"))
                        .build();
                try {
                    JavaFile javaFile = JavaFile.builder("com.example.interview", helloWorld)
                            .addFileComment("This codes are generated automatically. Do not modify!")
                            .build();
                    javaFile.writeTo(filer);
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }
        return true;
    }

    private static MethodSpec getMethodSpec(String methodStr, String returnStr) {
        return MethodSpec.methodBuilder(methodStr)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(String.class)
                .addStatement("return $S", returnStr)
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

```
}

上述代码中，一定不要忘了加AutoService的注解，通过这个注解，我们的HelloProcessor注解处理器相当于执行了一个注册的过程，这样才会被jvm在编译时加载，代码生成部分最终生成的文件代码长下面这个样子，可以结合注释对着体会下，还是很方便的，有了javapoet之后

```java
// This codes are generated automatically. Do not modify!
package com.example.interview;

import java.lang.String;

public final class HelloWorld {
  public static String hello1() {
    return "hello";
  }

  public static String hello2() {
    return "java";
  }

  public static String hello3() {
    return "Poet";
  }
}

```

ok，代码方面准备完毕。
接下来我们为app module添加依赖，如下

//必须使用annotationProcessor，而不是implementation
//annotationProcessor修饰的，最终不会打包到apk中，可理解为在编译时执行的

```java
annotationProcessor project(':javapoet')
implementation project(':annotation_lib')
```



然后我们手动编译一下项目，build -> make project，然后我们来到熟悉的MainActivity，首先使用我们的自定义注解标注MainActivity，要标在class上面，因为我们自定义的注解作用范围是Type，然后用一个TextView简单显示一下注入的HelloWorld类的几个静态方法，如下



```java
@HelloAnnotation
public class MainActivity extends AppCompatActivity {
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    TextView tv=findViewById(R.id.tv);
    String str1="";
    str1+=HelloWorld.hello1()+" ";
    str1+=HelloWorld.hello2()+" ";
    str1+=HelloWorld.hello3();
    tv.setText(str1);
     }
}
```
编译运行，看到Hello Java Poet字样即表示成功！

然后我们上面提到，除了使用Google开源库auto实现注册注解处理器外，还可以使用手动配置的方式，手动配置的方式如下

在自定义注解处理器的module中，这里也就是javapoet的module中，在main目录下创建出resources目录，然后在resources目录下创建出META-INF目录，然后在META-INF目录下创建出services目录，然后在services目录下创建一个名为javax.annotation.processing.Processor 的文件，在该文件中声明我们的注解处理器：

参考：原文链接：https://blog.csdn.net/hq942845204/article/details/81185693