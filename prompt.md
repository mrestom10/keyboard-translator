codemagic.io failed building android:

Alternatively, use the flag "--android-skip-build-dependency-validation" to bypass this check.

Potential fix: Your project's AGP version is typically defined in the plugins block of the `settings.gradle` file (/Users/builder/clone/android/settings.gradle), by a plugin with the id of com.android.application.
If you don't see a plugins block, your project was likely created with an older template version. In this case it is most likely defined in the top-level build.gradle file (/Users/builder/clone/android/build.gradle) by the following line in the dependencies block of the buildscript: "classpath 'com.android.tools.build:gradle:<version>'".

Warning: Flutter support for your project's Kotlin version (1.9.22) will soon be dropped. Please upgrade your Kotlin version to a version of at least 2.1.0 soon.
Alternatively, use the flag "--android-skip-build-dependency-validation" to bypass this check.

Potential fix: Your project's KGP version is typically defined in the plugins block of the `settings.gradle` file (/Users/builder/clone/android/settings.gradle), by a plugin with the id of org.jetbrains.kotlin.android.
If you don't see a plugins block, your project was likely created with an older template version, in which case it is most likely defined in the top-level build.gradle file (/Users/builder/clone/android/build.gradle) by the ext.kotlin_version property.

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

FAILURE: Build failed with an exception.

- What went wrong:
  Execution failed for task ':app:bundleDebugResources'.

  > A failure occurred while executing com.android.build.gradle.internal.res.Aapt2ProcessResourcesRunnable
  > Android resource linking failed

       /Users/builder/clone/build/app/intermediates/bundle_manifest/debug/AndroidManifest.xml:20: error: resource mipmap/ic_launcher (aka com.translator.translator_keyboard:mipmap/ic_launcher) not found.
       error: failed processing manifest.

- Try:
  > Run with --stacktrace option to get the stack trace.
  > Run with --info or --debug option to get more log output.
  > Run with --scan to get full insights.
  > Get more help at https://help.gradle.org.

BUILD FAILED in 1m 40s
Running Gradle task 'bundleDebug'... 101.7s
Gradle task bundleDebug failed with exit code 1

Build failed :|
Failed to build for Android
