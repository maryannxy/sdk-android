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
/* Initialize SDK */
XYSDKManager manager = new XYSDKManager(MyApplication.getAppContext());
/* Initialize the XY Bluetooth Scanner */
XYSmartScan.Instance().init(this);
```
Add a listener for the XYSmartScanner:

```java
        XYSmartScan.instance.addListener(String.valueOf(this.hashCode()), new XYSmartScan.Listener() {
            @Override
            public void entered(XYDevice device) {
            }

            @Override
            public void exited(XYDevice device) {
            }

            @Override
            public void detected(XYDevice device) {
            }

            @Override
            public void buttonPressed(XYDevice device, XYDevice.ButtonType buttonType) {
            }

            @Override
            public void buttonRecentlyPressed(XYDevice device, XYDevice.ButtonType buttonType) {
            }

            @Override
            public void statusChanged(XYSmartScan.Status status) {
            }

            @Override
            public void updated(XYDevice device) {
            }
        });
```
Start the bluetooth scanner

```java
XYSmartScan.instance.startAutoScan(context, 2000/*interval*/, 1000/*duration*/);
```
