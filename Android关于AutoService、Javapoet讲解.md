# [Android关于AutoService、Javapoet讲解]

# 一、上篇文章提到自定义processor中用到AutoService

文章中我们用到了AutoService, 使用@AutoService(Processor.class)，编译后

 

![img](https://images2017.cnblogs.com/blog/337690/201712/337690-20171220165946725-878963271.png)

```
AutoService会自动在META-INF文件夹下生成Processor配置信息文件，该文件里就是实现该服务接口的具体实现类。而当外部程序装配这个模块的时候，
就能通过该jar包META-INF/services/里的配置文件找到具体的实现类名，并装载实例化，完成模块的注入。
基于这样一个约定就能很好的找到服务接口的实现类，而不需要再代码里制定，方便快捷。应用依赖如下：
compile ``'com.google.auto.service:auto-service:1.0-rc2'
```

　　

# 二、javapoet常用api

JavaPoet是square推出的开源java代码生成框架，提供Java Api生成.java源文件。这个框架功能非常有用，我们可以很方便的使用它根据注解、数据库模式、协议格式等来对应生成代码。通过这种自动化生成代码的方式，可以让我们用更加简洁优雅的方式要替代繁琐冗杂的重复工作。引用依赖：

```
compile ``'com.squareup:javapoet:1.7.0'
```

　

该项目结构如下：

![img](https://images2017.cnblogs.com/blog/337690/201712/337690-20171220160126928-1261366836.png)

 

相关类介绍

| JavaFile       | A Java file containing a single top level class   | 用于构造输出包含一个顶级类的Java文件 |
| -------------- | ------------------------------------------------- | ------------------------------------ |
| TypeSpec       | A generated class, interface, or enum declaration | 生成类，接口，或者枚举               |
| MethodSpec     | A generated constructor or method declaration     | 生成构造函数或方法                   |
| FieldSpec      | A generated field declaration                     | 生成成员变量或字段                   |
| ParameterSpec  | A generated parameter declaration                 | 用来创建参数                         |
| AnnotationSpec | A generated annotation on a declaration           | 用来创建注解                         |

在JavaPoet中，JavaFile是对.java文件的抽象，TypeSpec是类/接口/枚举的抽象，MethodSpec是方法/构造函数的抽象，FieldSpec是成员变量/字段的抽象。这几个类各司其职，但都有共同的特点，提供内部Builder供外部更多更好地进行一些参数的设置以便有层次的扩展性的构造对应的内容

 

常用api:

- addStatement() 方法负责分号和换行
- beginControlFlow() + endControlFlow() 需要一起使用，提供换行符和缩进。
- addCode() 以字符串的形式添加内
- returns 添加返回值类型
- .constructorBuilder() 生成构造器函数
- .addAnnotation 添加注解
- addSuperinterface 给类添加实现的接口
- superclass 给类添加继承的父类
- ClassName.bestGuess(“类全名称”) 返回ClassName对象，这里的类全名称表示的类必须要存在，会自动导入相应的包
- ClassName.get(“包名”，”类名”) 返回ClassName对象，不检查该类是否存在
- TypeSpec.interfaceBuilder(“HelloWorld”)生成一个HelloWorld接口
- MethodSpec.constructorBuilder() 构造器
- addTypeVariable(TypeVariableName.get(“T”, typeClassName)) 
  会给生成的类加上泛型

占位符

- $L代表的是字面量
- $S for Strings
- $N for Names(我们自己生成的方法名或者变量名等等)
- $T for Types

 

# 三、javapoet的使用

```java
package com.example.helloworld;
 
public final class HelloWorld {
  public static void main(String[] args) {
    System.out.println("Hello, JavaPoet!");
  }
}
```

　　

上面的代码我们可以调用javapoet的api方法去生成：

```
MethodSpec main = MethodSpec.methodBuilder("main")
    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
    .returns(void.class)
    .addParameter(String[].class, "args")
    .addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet!")
    .build();
 
TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
    .addMethod(main)
    .build();
 
JavaFile javaFile = JavaFile.builder("com.example.helloworld", helloWorld)
    .build();
 
javaFile.writeTo(System.out);

```

　可以看出，addModifiers对方法的修饰约束，addParameter添加方法参数 ，addStatement方法体，returns返回值，最后写入java文件中

代码和控制流程大多数JavaPoet的API使用普通的旧的不可变的Java对象。也有建设者，方法链和可变参数使API友好。JavaPoet为类和接口（TypeSpec），fields（FieldSpec），方法和构造函数（MethodSpec），参数（ParameterSpec）和注释（AnnotationSpec）提供模型。　

```
MethodSpec main = MethodSpec.methodBuilder("main")
    .addCode(""
        + "int total = 0;\n"
        + "for (int i = 0; i < 10; i++) {\n"
        + "  total += i;\n"
        + "}\n")
    .build();
```

　　

则会生成下面的代码

```
void main() {
  int total = 0;
  for (int i = 0; i < 10; i++) {
    total += i;
  }
}
```

　　

我们可以子自定义方法去调用

```
private MethodSpec computeRange(String name, int from, int to, String op) {
  return MethodSpec.methodBuilder(name)
      .returns(int.class)
      .addStatement("int result = 0")
      .beginControlFlow("for (int i = " + from + "; i < " + to + "; i++)")
      .addStatement("result = result " + op + " i")
      .endControlFlow()
      .addStatement("return result")
      .build();
}
```

　　

调用上面的方法后computeRange("multiply10to20", 10, 20, "*") 生成下面的java代码

```
int multiply10to20() {
  int result = 0;
  for (int i = 10; i < 20; i++) {
    result = result * i;
  }
  return result;
}
```

　　

对于$T泛型 ，我们的Java程序员喜欢我们的类型：他们让我们的代码更容易理解。JavaPoet在船上。它具有丰富的内置支持类型，包括自动生成`import` 语句。只是`$T`用来引用类型：

```
package com.example.helloworld;
 
import java.util.Date;
 
public final class HelloWorld {
  Date today() {
    return new Date();
  }
}
```

　　

要生成上面代码 可以用下面的javapoet去实现

```
MethodSpec today = MethodSpec.methodBuilder("today")
    .returns(Date.class)
    .addStatement("return new $T()", Date.class)
    .build();
 
TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
    .addMethod(today)
    .build();
 
JavaFile javaFile = JavaFile.builder("com.example.helloworld", helloWorld)
    .build();
 
javaFile.writeTo(System.out);
```

　　

JavaPoet支持`import static`。它通过明确收集类型成员名称来完成。见下面代码：

```java
JavaFile.builder("com.example.helloworld", hello)
    .addStaticImport(hoverboard, "createNimbus")
    .addStaticImport(namedBoards, "*")
    .addStaticImport(Collections.class, "*")
    .build();
```

　　

如果我们想生成构造Constructors，也很简单：

```java
public class HelloWorld {
  private final String greeting;
 
  public HelloWorld(String greeting) {
    this.greeting = greeting;
  }
}
```

　　

要实现上面java代码，用javapoet去实现，如下：

```java
MethodSpec flux = MethodSpec.constructorBuilder()
    .addModifiers(Modifier.PUBLIC)
    .addParameter(String.class, "greeting")
    .addStatement("this.$N = $N", "greeting", "greeting")
    .build();
 
TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addModifiers(Modifier.PUBLIC)
    .addField(String.class, "greeting", Modifier.PRIVATE, Modifier.FINAL)
    .addMethod(flux)
    .build();
```

　　

还可定义枚举类

```java
TypeSpec helloWorld = TypeSpec.enumBuilder("Roshambo")
    .addModifiers(Modifier.PUBLIC)
    .addEnumConstant("ROCK")
    .addEnumConstant("SCISSORS")
    .addEnumConstant("PAPER")
    .build();
 
 
public enum Roshambo {
  ROCK,
 
  SCISSORS,
 
  PAPER
}
```

　　

还有匿名内部类的实现

```java
TypeSpec comparator = TypeSpec.anonymousClassBuilder("")
    .addSuperinterface(ParameterizedTypeName.get(Comparator.class, String.class))
    .addMethod(MethodSpec.methodBuilder("compare")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(String.class, "a")
        .addParameter(String.class, "b")
        .returns(int.class)
        .addStatement("return $N.length() - $N.length()", "a", "b")
        .build())
    .build();
 
TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addMethod(MethodSpec.methodBuilder("sortByLength")
        .addParameter(ParameterizedTypeName.get(List.class, String.class), "strings")
        .addStatement("$T.sort($N, $L)", Collections.class, "strings", comparator)
        .build())
    .build();
This generates a method that contains a class that contains a method:
 
void sortByLength(List<String> strings) {
  Collections.sort(strings, new Comparator<String>() {
    @Override
    public int compare(String a, String b) {
      return a.length() - b.length();
    }
  });
}
```

　　

也可创建注解：

```java
MethodSpec toString = MethodSpec.methodBuilder("toString")
    .addAnnotation(Override.class)
    .returns(String.class)
    .addModifiers(Modifier.PUBLIC)
    .addStatement("return $S", "Hoverboard")
    .build();
 
  //结果如下：
  @Override
  public String toString() {
    return "Hoverboard";
  }
```

　　

创建注释javadoc

```java
MethodSpec dismiss = MethodSpec.methodBuilder("dismiss")
    .addJavadoc("Hides {@code message} from the caller's history. Other\n"
        + "participants in the conversation will continue to see the\n"
        + "message in their own history unless they also delete it.\n")
    .addJavadoc("\n")
    .addJavadoc("<p>Use {@link #delete($T)} to delete the entire\n"
        + "conversation for all participants.\n", Conversation.class)
    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
    .addParameter(Message.class, "message")
    .build();
 
 
  /**
   * Hides {@code message} from the caller's history. Other
   * participants in the conversation will continue to see the
   * message in their own history unless they also delete it.
   *
   * <p>Use {@link #delete(Conversation)} to delete the entire
   * conversation for all participants.
   */
  void dismiss(Message message);
```

　　

用法很多，还可以用FieldSpec.builder创建属性变量，ParameterSpec创建方法参数