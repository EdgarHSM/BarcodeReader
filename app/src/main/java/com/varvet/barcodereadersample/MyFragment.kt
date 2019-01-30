package com.varvet.barcodereadersample

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView

class MyFragment : DialogFragment() {


    companion object {
        fun newInstance(title: String, list: Array<String?>): MyFragment {
            val fragment = MyFragment()
            val args = Bundle()
            args.putString("title", title)
            args.putStringArray("list", list)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fraglayout, container)

        //items = arrayOf("Dennis Ritchie", "Rodney Brooks", "Sergey Brin", "Larry Page", "Cynthia Breazeal", "Jeffrey Bezos", "Berners-Lee Tim", "Centaurus A", "Virgo Stellar Stream")
        var items = arguments["list"]
        val myListView = rootView.findViewById(R.id.myListView) as ListView
        //with arrayadapter you have to pass a textview as a resource, and that is simple_list_item_1
        myListView!!.adapter = ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1, items as Array<out String>)

        this.dialog.setTitle(arguments["title"].toString())


        return rootView
    }
}