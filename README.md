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
    compile 'com.xyfindables:sdk-android:{latest version}'
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

## Using different Classes offered by XY SDK for Android

### XYDevice

#### Definitions

```java
static final int BATTERYLEVEL_INVALID = 0;
static final int BATTERYLEVEL_CHECKED = -1;
static final int BATTERYLEVEL_NOTCHECKED = -2;
static final int BATTERYLEVEL_SCHEDULED = -3;

static final HashMap<UUID, XYDevice.Family> uuid2family;

static final HashMap<XYDevice.Family, UUID> family2uuid;

static final HashMap<XYDevice.Family, String> family2prefix;

static Comparator<XYDevice> Comparator = new Comparator<XYDevice>() {
        @Override
        public int compare(XYDevice lhs, XYDevice rhs) {
            return lhs._id.compareTo(rhs._id);
	}
};
```
Used to compare the ids of devices.  
Returns a 0 if they are the same and a positive number is the previous device's id is greater than the next device.  
It will return a negative number if the previous device's id is lower than the next device.  
This can be used to sort devices by id.  

```java
public final static Comparator<XYDevice> DistanceComparator = new Comparator<XYDevice>() {
        @Override
        public int compare(XYDevice lhs, XYDevice rhs) {
            return Integer.compare(lhs.getRssi(), rhs.getRssi());
        }
};
```
Used to compare distances of devices.  
This can be used to sort devices by distances from host device.  

```java
static final int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;
static final int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
static final int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;
```

#### XYDevice

```java
XYDevice(String id);
```

Starts an instance of XYDevice.

##### Parameters

String id - a unique id to identify a device.

##### Example

```java
XYDevice device = new XYDevice("uniqueId");
```

#### getInstanceCount

```java
static int getInstanceCount();
```

Returns the number of XYDevice instances.

##### Example

```java
XYDevice.getInstanceCount();
```

#### stayConnected

```java
void stayConnected(final Context context, boolean value);
```

Turns the Stay Connected feature of a device on or off.  

##### Parameters

final Context context - the context where this method is called.  

boolean value - true turns Stay Connected on and false turns it off.  

##### Example

```java
XYDevice device = new XYDevice("id");
device.stayConnected(currentActivity.this, true);
```

#### getDistance

```java
double getDistance();
```

Returns distance between device and host device.

##### Example

```java
XYDevice device = new XYDevice("id");
device.getDistance();
```

#### getBeaconAddress

```java
String getBeaconAddress();
```

Returns the address of a beacon.

##### Example

```java
XYDevice device = new XYDevice("id");
device.getBeaconAddress();
```

#### getFirmwareVersion

```java
String getFirmwareVersion();
```

Returns the firmware version of a device.

##### Example

```java
XYDevice device = new XYDevice("id");
device.getFirmwareVersion();
```

#### isConnected

```java
boolean isConnected();
```

Returns true if device is connected.  
Returns false if device is not connected.  

##### Example

```java
XYDevice device = new XYDevice("id");
device.isConnected();
```

#### getBatteryLevel

```java
int getBatteryLevel();
```

Returns the battery level of a beacon as an int from 0 to 100.  

##### Example

```java
XYDevice device = new XYDevice("id");
device.getBatteryLevel();
```

#### checkBatteryAndVersionInFuture

```java
void checkBatteryAndVersionInFuture(final Context context);
```

Starts a timer to check the battery and version 6-12 minutes after the function is called.  
This can be used when a user is signing up or first logging in so that load speeds are faster.  

##### Parameters

final Context context - current context where this method is called.  

##### Example

```java
XYDevice device = new XYDevice("id");
device.checkBatteryAndVersionInFuture(currentActivity.this);
```

#### checkBattery

```java
void checkBattery(final Context context)
```

Checks the battery level.

##### Parameters

final Context context - current context where this method is called.

##### Example

```java
XYDevice device = new XYDevice("id");
device.checkBattery(currentActivity.this);
```

#### checkVersion

```java
void checkVersion(final Context context)
```

Checks the version of firmware.

##### Parameters

final Context context - current context where this method is called.

##### Example

```java
XYDevice device = new XYDevice("id");
device.checkVersion(currentActivity.this);
```

#### getId

```java
String getId();
```

Returns the beacon id string.

##### Example

```java
XYDevice device = new XYDevice("id");
device.getId();
```

#### isUpdateSignificant

```java
boolean isUpdateSignificant();
```

Returns true is the update will be significant (long time since last update).

##### Example

```java
XYDevice device = new XYDevice("id");
device.isUpdateSignificant();
```

#### getFamily

```java
Family getFamily();
```

Returns the family of a device. (ie: XY3 or Mobile)

##### Example

```java
XYDevice device = new XYDevice("id");
device.getFamily();
```

#### getProximity

```java
Proximity getProximity();
```

Returns a proximity describing distance between beacon and host device. (ie: Far, Medium, Near, etc)

##### Example

```java
XYDevice device = new XYDevice("id");
device.getProximity();
```

#### addListener

```java
void addListener(String key, Listener listener);
```

Used to add a listener which can use methods from the XYDevice class.

##### Parameters

String key - key to identify where listener is attached to.  
Listener listener - see Listener interface.  

##### Example

```java
XYDevice device = new XYDevice("id");
device.addListener("key", listener);
```

#### removeListener

```java
void removeListener(String key);
```

Used to remove a listener from the XYDevice class.

##### Parameters

String key - key to identify where listener is attached to.

##### Example

```java
XYDevice device = new XYDevice("id");
device.removeListener("key");
```

#### Family

```java
enum Family {
        Unknown,
        XY1,
        XY2,
        XY3,
        Mobile,
        Gps,
        Near
}
```

Family types for devices.

#### ButtonType

```java
public enum ButtonType {
        None,
        Single,
        Double,
        Long
}
```

Button types for ifttt and zapier.

#### Proximity

```java
public enum Proximity {
        None,
        OutOfRange,
        VeryFar,
        Far,
        Medium,
        Near,
        VeryNear,
        Touching
}
```

Proximity values for how close a beacon is to a KeepNear device.

#### Listener

```java
public interface Listener {
	void entered(final XYDevice device);

        void exited(final XYDevice device);

        void detected(final XYDevice device);

        void buttonPressed(final XYDevice device, final ButtonType buttonType);

        void buttonRecentlyPressed(final XYDevice device, final ButtonType buttonType);

        void connectionStateChanged(final XYDevice device, int newState);

        void updated(final XYDevice device);
}
```

An interface for listeners to use with the XYDevice class.

