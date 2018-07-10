package com.xyfindables.sdk.sample

import android.os.Bundle
import android.widget.BaseAdapter
import android.widget.ListView

import com.crashlytics.android.Crashlytics
import com.xyfindables.core.XYBase
import com.xyfindables.core.XYPermissions

import io.fabric.sdk.android.Fabric

class XYFindablesSdkSampleActivity : XYAppBaseActivity() {
    private var adapter: BaseAdapter? = null
    private var listView: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        logInfo("onCreate")
        super.onCreate(savedInstanceState)
        val crashlytics = Crashlytics()
        Fabric.with(this, crashlytics, Crashlytics())
        XYBase.init(this)
        setContentView(R.layout.activity_xyfindables_sdk_sample)
        listView = findViewById(R.id.listview)
        adapter = XYDeviceAdapter(this)
        listView!!.adapter = adapter
    }

    override fun onResume() {
        logInfo("onResume")
        super.onResume()
        val permissions = XYPermissions(this)
        permissions.requestPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, "Location services are needed to access and track your finders.", XYPermissions.LOCATION_PERMISSIONS_REQ_CODE)
    }

    override fun onPause() {
        super.onPause()
    }
}
