package com.xyfindables.sdk.sample.activities

import android.os.Bundle
import android.widget.BaseAdapter
import com.xyfindables.core.XYBase
import com.xyfindables.core.XYPermissions
import com.xyfindables.sdk.sample.R
import com.xyfindables.sdk.sample.adapters.XYDeviceAdapter
import kotlinx.android.synthetic.main.activity_xyfindables_sdk_sample.*

class XYFindablesSdkSampleActivity : XYAppBaseActivity() {
    private var adapter: BaseAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        logInfo("onCreate")
        super.onCreate(savedInstanceState)
        XYBase.init(this)
        setContentView(R.layout.activity_xyfindables_sdk_sample)

        adapter = XYDeviceAdapter(this)
        listview!!.adapter = adapter
    }

    override fun onResume() {
        logInfo("onResume")
        super.onResume()
        val permissions = XYPermissions(this)
        permissions.requestPermission(android.Manifest.permission.ACCESS_FINE_LOCATION,
                "Location services are needed to connection and track your finders.",
                XYPermissions.LOCATION_PERMISSIONS_REQ_CODE)
        adapter?.notifyDataSetChanged()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        scanner.start()
    }
}
