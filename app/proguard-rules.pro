# App Keep Rules
-keep class com.chet.mi.** { *; }
-keep class com.hCore.** { *; }
-keep class black.** { *; }

-dontwarn top.niunaijun.**
-dontwarn black.**
-dontwarn com.hCore.**

-keep class com.chet.mi.BoxApplication { *; }
-keep class com.chet.mi.LogAct { *; }
-keep class com.chet.mi.MAct { *; }
-keep class com.chet.mi.utils.Downtwo { *; }
-keep class com.chet.mi.utils.AppManager { *; }
-keep class com.chet.mi.utils.Prefs { *; }
