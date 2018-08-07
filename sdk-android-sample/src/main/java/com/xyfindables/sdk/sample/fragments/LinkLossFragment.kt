package com.xyfindables.sdk.sample.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.xyfindables.sdk.sample.R


class LinkLossFragment : XYAppBaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_link_loss, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun update() {
        logInfo("update")
    }

    companion object {
        private const val TAG = "LinkLossFragment"

        fun newInstance() =
                LinkLossFragment()
    }

}
