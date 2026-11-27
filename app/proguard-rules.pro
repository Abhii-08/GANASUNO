# ganasuno – release shrinking is off by default; keep extractor classes if you turn it on.
-keep class org.schabi.newpipe.extractor.** { *; }
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**
-dontwarn javax.script.**
-dontwarn org.mozilla.classfile.**
