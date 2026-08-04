# Begin: Debug Proguard rules

-dontobfuscate                              #난독화를 수행하지 않도록 함
-keepattributes SoureFile,LineNumberTable   #소스파일, 라인 정보 유지
# End: Debug ProGuard rules

 # With R8 full mode generic signatures are stripped for classes that are not
 # kept. Suspend functions are wrapped in continuations where the type argument
 # is used.
 -keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
