
cd provider
"%ANDROID_HOME%/platform-tools/adb" shell am instrument -w android.tests.provider/android.tests.provider.RcsApiProviderInstrumentationTestRunner

cd ..

cd signature
%ANDROID_HOME%/platform-tools/adb shell am instrument -w android.tests.sigtest/android.tests.sigtest.InstrumentationRunner


pause