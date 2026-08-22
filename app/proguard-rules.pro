# Retrofit interfaces are consumed reflectively, so their generic signatures and
# annotations must survive shrinking.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Moshi generates adapters named <Dto>JsonAdapter and looks them up by name.
-keep class com.berco.postapp.data.remote.dto.** { *; }
-keep class **JsonAdapter { *; }
