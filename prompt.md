codemagic.io failed building android:

If you don't see a plugins block, your project was likely created with an older template version. In this case it is most likely defined in the top-level build.gradle file (/Users/builder/clone/android/build.gradle) by the following line in the dependencies block of the buildscript: "classpath 'com.android.tools.build:gradle:<version>'".

Checking the license for package Android SDK Build-Tools 33.0.1 in /usr/local/share/android-sdk/licenses
License for package Android SDK Build-Tools 33.0.1 accepted.
Preparing "Install Android SDK Build-Tools 33.0.1 v.33.0.1".
"Install Android SDK Build-Tools 33.0.1 v.33.0.1" ready.
Installing Android SDK Build-Tools 33.0.1 in /usr/local/share/android-sdk/build-tools/33.0.1
"Install Android SDK Build-Tools 33.0.1 v.33.0.1" complete.
"Install Android SDK Build-Tools 33.0.1 v.33.0.1" finished.
Checking the license for package Android SDK Platform 36 in /usr/local/share/android-sdk/licenses
License for package Android SDK Platform 36 accepted.
Preparing "Install Android SDK Platform 36 (revision 2)".
"Install Android SDK Platform 36 (revision 2)" ready.
Installing Android SDK Platform 36 in /usr/local/share/android-sdk/platforms/android-36
"Install Android SDK Platform 36 (revision 2)" complete.
"Install Android SDK Platform 36 (revision 2)" finished.
e: file:///Users/builder/clone/android/app/src/main/kotlin/com/translator/translator_keyboard/TranslatorInputMethodService.kt:63:34 None of the following candidates is applicable:
constructor(p0: Context, p1: Boolean): FlutterSurfaceView
constructor(p0: Context, p1: AttributeSet): FlutterSurfaceView
e: file:///Users/builder/clone/android/app/src/main/kotlin/com/translator/translator_keyboard/TranslatorInputMethodService.kt:64:54 Cannot infer type for this parameter. Please specify it explicitly.
e: file:///Users/builder/clone/android/app/src/main/kotlin/com/translator/translator_keyboard/TranslatorInputMethodService.kt:64:54 Unresolved reference. None of the following candidates is applicable because of a receiver type mismatch:
fun <T> T.apply(block: T.() -> Unit): T
[R|Contract description]
<
CallsInPlace(block, EXACTLY_ONCE) >
e: file:///Users/builder/clone/android/app/src/main/kotlin/com/translator/translator_keyboard/TranslatorInputMethodService.kt:64:60 Cannot infer type for this parameter. Please specify it explicitly.
e: file:///Users/builder/clone/android/app/src/main/kotlin/com/translator/translator_keyboard/TranslatorInputMethodService.kt:65:13 Unresolved reference 'attachToFlutterEngine'.

FAILURE: Build failed with an exception.

- What went wrong:
  Execution failed for task ':app:compileDebugKotlin'.

  > A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
  > Compilation error. See log for more details

- Try:
  > Run with --stacktrace option to get the stack trace.
  > Run with --info or --debug option to get more log output.
  > Run with --scan to get full insights.
  > Get more help at https://help.gradle.org.

BUILD FAILED in 2m 21s
Running Gradle task 'bundleDebug'... 142.7s
Gradle task bundleDebug failed with exit code 1

Build failed :|
Failed to build for Android
