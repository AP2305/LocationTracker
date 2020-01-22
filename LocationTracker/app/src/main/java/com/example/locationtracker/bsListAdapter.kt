package com.example.locationtracker

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.bs_listview.view.*

class bsListAdapter(private val context: Activity, private val name: ArrayList<String>):ArrayAdapter<String>(context,R.layout.bs_listview,name) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val inflater = context.layoutInflater
        val bsView = inflater.inflate(R.layout.bs_listview,null,true)

        bsView.list_item_text.text = name[position]


        return bsView
    }

}