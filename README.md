# XY SDK for Android

Add it to your build.gradle with:
```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```
and:

```gradle
dependencies {
    compile 'com.xyfindables:xy-android-sdk:{latest version}'
}
```
## Using the SDK

Initialize the XYSDKManager, XYApiSession, and XYSmartScan with the app context:

```java
XYSDKManager manager = new XYSDKManager(MyApplication.getAppContext());
/* Initialize the API Session */
XYApiSession.Instance().init(this, XY_API_TOKEN, BuildConfig.VERSION_NAME);
/* Initialize the XY Bluetooth Scanner */
XYSmartScan.Instance().init(this);
```
Add a listener for the XYSmartScanner:

```java
XYSmartScan.Instance().addListener(TAG_NAME, new XYSmartScan.Listener(){
            public void entered(XYDevice device) {

            }

            public void exited(XYDevice device) {

            }

            public void detected(XYDevice device) {

            }

            public void buttonPressed(@NonNull XYDevice device, XYDevice.ButtonType buttonType) {

            }

            public void buttonRecentlyPressed(XYDevice device, XYDevice.ButtonType buttonType) {

            }
        });
```
