package com.xyfindables.sdk.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.BaseAdapter
import android.widget.ListView

import com.crashlytics.android.Crashlytics
import com.xyfindables.core.XYBase
import com.xyfindables.core.XYPermissions
import com.xyfindables.sdk.XYDevice
import com.xyfindables.sdk.XYSmartScan
import com.xyfindables.ui.XYBaseActivity

import io.fabric.sdk.android.Fabric

class XYFindablesSdkSampleActivity : XYBaseActivity() {
    private var _adapter: BaseAdapter? = null
    private var _listView: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val crashlytics = Crashlytics()
        Fabric.with(this, crashlytics, Crashlytics())
        XYBase.init(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_xyfindables_sdk_sample)
        _listView = findViewById(R.id.listview)
        _adapter = XYDeviceAdapter(this)
        _listView!!.adapter = _adapter
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        val permissions = XYPermissions(this)
        permissions.requestPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, "Location services are needed to access and track your finders.", XYPermissions.LOCATION_PERMISSIONS_REQ_CODE)
        XYSmartScan.instance.startAutoScan(this, 0, 30000)
    }

    companion object {

        private val TAG = XYFindablesSdkSampleActivity::class.java.simpleName
    }
}
