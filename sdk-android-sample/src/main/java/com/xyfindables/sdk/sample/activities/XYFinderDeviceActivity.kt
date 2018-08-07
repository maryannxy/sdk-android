package com.xyfindables.sdk.sample.activities

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import com.xyfindables.sdk.devices.XY3BluetoothDevice
import com.xyfindables.sdk.devices.XY4BluetoothDevice
import com.xyfindables.sdk.devices.XYBluetoothDevice
import com.xyfindables.sdk.devices.XYFinderBluetoothDevice
import com.xyfindables.sdk.sample.R
import com.xyfindables.sdk.sample.fragments.*
import com.xyfindables.ui.XYBaseFragment
import kotlinx.android.synthetic.main.device_activity.*


/**
 * Created by arietrouw on 12/28/17.
 */

class XYFinderDeviceActivity : XYAppBaseActivity() {

    var device: XYBluetoothDevice? = null
    private lateinit var mSectionsPagerAdapter: SectionsPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deviceHash = intent.getIntExtra(EXTRA_DEVICEHASH, 0)
        logInfo("onCreate: $deviceHash")
        device = scanner.devices[deviceHash]
        if (device == null) {
            showToast("Failed to Find Device")
            finish()
        }
        setContentView(R.layout.device_activity)

        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        container.adapter = mSectionsPagerAdapter

        container.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabs))
        tabs.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(container))
    }


    override fun onStop() {
        super.onStop()
        device!!.removeListener(TAG)
    }

    private val xy3DeviceListener = object : XY3BluetoothDevice.Listener() {
        override fun entered(device: XYBluetoothDevice) {
            update()
            showToast("Entered")
        }

        override fun exited(device: XYBluetoothDevice) {
            update()
            showToast("Exited")
        }

        override fun detected(device: XYBluetoothDevice) {
            update()
        }

        override fun connectionStateChanged(device: XYBluetoothDevice, newState: Int) {
            if (newState == 2) {
                showToast("Connected")
                update()
            } else {
                showToast("Disconnected")
            }
        }

        override fun buttonSinglePressed() {
            showToast("Button Pressed: Single")
        }

        override fun buttonDoublePressed() {
            showToast("Button Pressed: Double")
        }

        override fun buttonLongPressed() {
            showToast("Button Pressed: Long")
        }
    }

    private val xy4DeviceListener = object : XY4BluetoothDevice.Listener() {
        override fun entered(device: XYBluetoothDevice) {
            update()
            showToast("Entered")
        }

        override fun exited(device: XYBluetoothDevice) {
            update()
            showToast("Exited")
        }

        override fun detected(device: XYBluetoothDevice) {
            update()
        }

        override fun connectionStateChanged(device: XYBluetoothDevice, newState: Int) {
            if (newState == 2) {
                showToast("Connected")
                update()
            } else {
                showToast("Disconnected")
            }
        }

        override fun buttonSinglePressed(device: XYFinderBluetoothDevice) {
            showToast("Button Pressed: Single")
        }

        override fun buttonDoublePressed(device: XYFinderBluetoothDevice) {
            showToast("Button Pressed: Double")
        }

        override fun buttonLongPressed(device: XYFinderBluetoothDevice) {
            showToast("Button Pressed: Long")
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (device != null) {
            if ((device as? XY4BluetoothDevice) != null) {
                device!!.addListener(TAG, xy4DeviceListener)
            } else {
                if ((device as? XY3BluetoothDevice) != null) {
                    device!!.addListener(TAG, xy3DeviceListener)
                }
            }
            //update()
        }

        //readUpdates()
        //enableButtonNotify()
    }

    fun showProgressSpinner() {
        progress_spinner.visibility = View.VISIBLE
    }

    fun hideProgressSpinner() {
        progress_spinner.visibility = View.GONE
    }

    fun update() {
        val frag = mSectionsPagerAdapter.getFragmentByPosition(container.currentItem)
        (frag as? XYAppBaseFragment)?.update()
    }

//    private fun readUpdates() {
//        launch(CommonPool) {
//            updateStayAwakeEnabledStates().await()
//            updateLockValue().await()
//            update()
//        }
//    }


    companion object {
        var EXTRA_DEVICEHASH = "DeviceHash"
        private val TAG = XYFinderDeviceActivity::class.java.simpleName
    }


    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        private val size = 9
        private var fragments: SparseArray<XYBaseFragment> = SparseArray(size)

        override fun getItem(position: Int): Fragment {
            lateinit var frag: XYBaseFragment

            when (position) {
                0 -> {
                    frag = InfoFragment.newInstance()
                }
                1 -> {
                    frag = AlertFragment.newInstance()
                }
                2 -> {
                    frag = BatteryFragment.newInstance()
                }
                3 -> {
                    frag = CurrentTimeFragment.newInstance()
                }
                4 -> {
                    frag = DeviceFragment.newInstance()
                }
                5 -> {
                    frag = GenericAccessFragment.newInstance()
                }
                6 -> {
                    frag = GenericAttributeFragment.newInstance()
                }
                7 -> {
                    frag = LinkLossFragment.newInstance()
                }
                8 -> {
                    frag = TxPowerFragment.newInstance()
                }
            }

            return frag
        }

        override fun getCount(): Int {
            return size
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val fragment = super.instantiateItem(container, position) as XYBaseFragment
            fragments.append(position, fragment)
            return fragment
        }

        fun getFragmentByPosition(position: Int): XYBaseFragment {
            return fragments.get(position)
        }
    }
}
