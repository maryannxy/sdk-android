package com.xyfindables.sdk.sample.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.xyfindables.sdk.sample.R


class GenericAttributeFragment : XYAppBaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_generic_attribute, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun update() {
        logInfo("update")
    }

    companion object {
        private const val TAG = "GenericAttributeFragment"

        fun newInstance() =
                GenericAttributeFragment()
    }
}
