    -optimizationpasses 5
-allowaccessmodification
-dontpreverify
-dontshrink
-dontoptimize

# Minificação de Nomes
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keep class its.madruga.** {
    *;
}

# Remoção de Código Não Utilizado
-keep class its.madruga.** {
    *;
}
-keep class * {
    public private *;
}
-keepattributes *Annotation*

# Ofuscação de Strings
-keepclassmembers class its.madruga.** {
    *;
}

# Manter a classe javax.lang.model.element.Modifier
-keepattributes *Annotation*,Signature
-keep public class javax.lang.model.element.Modifier { *; }

# Suprimir avisos relacionados a javax.lang.model.element.Modifier
-dontwarn javax.lang.model.element.Modifier
-dontwarn javax.naming.Binding
-dontwarn javax.naming.NamingEnumeration
-dontwarn javax.naming.NamingException
-dontwarn javax.naming.directory.Attribute
-dontwarn javax.naming.directory.Attributes
-dontwarn javax.naming.directory.DirContext
-dontwarn javax.naming.directory.InitialDirContext
-dontwarn javax.naming.directory.SearchControls
-dontwarn javax.naming.directory.SearchResult

# Manter as anotações da biblioteca Error Prone
-keepattributes *Annotation*,Signature
-keep public class com.google.errorprone.annotations.** { *; }
