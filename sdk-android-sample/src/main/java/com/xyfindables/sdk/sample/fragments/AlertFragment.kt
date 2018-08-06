package com.xyfindables.sdk.sample.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.xyfindables.sdk.sample.R
import kotlinx.android.synthetic.main.fragment_alert.*

//TODO - this is server only?
class AlertFragment : XYAppBaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_alert, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_refresh.setOnClickListener {
            logInfo("refresh clicked")
            update()
        }
    }

    override fun onResume() {
        super.onResume()
        logInfo("onResume: AlertFragment")
        //update()
    }

    override fun update() {
        logInfo("update")
    }

    companion object {
        private const val TAG = "AlertFragment"

        @JvmStatic
        fun newInstance() =
                AlertFragment()
    }
}
