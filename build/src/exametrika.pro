-basedirectory <build.dir>/lib
-dontwarn org.apache.**,com.exametrika.impl.profiler.boot.AgentStackProbeInterceptor
-dontnote

-applymapping <build.dir>/exametrika.map
-printmapping <build.dir>/exametrika-out.map

-useuniqueclassmembernames
-dontshrink
-dontoptimize
-dontpreverify

-repackageclasses 'com.exametrika.exa'
-allowaccessmodification

-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-keepattributes RuntimeVisibleAnnotations

-adaptresourcefilecontents META-INF/**, WEB-INF/web.xml

-injars com.exametrika.agent.jar
-injars com.exametrika.server.jar
-injars com.exametrika.tester.jar
-injars web.api.war
-injars web.ui.war

-outjars ../../protected

-libraryjars <java.home>/lib/rt.jar
-libraryjars asm-5.0.3.jar
-libraryjars asm-commons-5.0.3.jar
-libraryjars icu4j-53_1.jar
-libraryjars javax.mail.jar
-libraryjars lucene-analyzers-common-4.9.0.jar
-libraryjars lucene-analyzers-icu-4.9.0.jar
-libraryjars lucene-core-4.9.0.jar
-libraryjars lucene-queries-4.9.0.jar
-libraryjars lucene-queryparser-4.9.0.jar
-libraryjars sigar-1.6.4.jar
-libraryjars tomcat-embed-core-7.0.59.jar
-libraryjars tools.jar
-libraryjars trove-3.0.3.jar
-libraryjars com.exametrika.common.jar
-libraryjars <root.dir>/metrics.jvm.bridge/lib/jms-api.jar
-libraryjars exaa.jar
-libraryjars com.exametrika.boot.jar

-keep public class com.exametrika.api.**
{
  	public protected *;
}

-keep public class com.exametrika.spi.**
{
	public protected *;
}

-keepclasseswithmembernames class * 
{
    native <methods>;
}

-keepclassmembers enum * 
{
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers public class * 
{
    public static void main(java.lang.String[]);
    public static void premain(java.lang.String, java.lang.instrument.Instrumentation);
    public static void agentmain(java.lang.String, java.lang.instrument.Instrumentation);
}

-keep class org.apache.juli.logging.**
{
  	*;
}

-keepclassmembers class com.exametrika.impl.profiler.boot.* 
{
	public static <methods>;
	public static java.lang.Object[] methods;
}

-keepclassmembers class com.exametrika.impl.metrics.jvm.boot.* 
{
	public static <methods>;
}