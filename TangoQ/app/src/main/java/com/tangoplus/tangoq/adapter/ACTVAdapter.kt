package com.tangoplus.tangoq.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class ACTVAdapter (context: Context, resource: Int, private val items: List<Pair<Int, String>>) :
    ArrayAdapter<Pair<Int, String>>(context, resource, items) {

    override fun getItem(position: Int): Pair<Int, String> {
        return items[position]
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_dropdown_item_1line, parent, false)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = items[position].second
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent)
    }
    fun getStringValue(position: Int): String {
        return items[position].second
    }
}