package com.example.mhg.Bluetooth

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.example.mhg.R
import com.example.mhg.databinding.RvBluetoothListBinding
import com.example.mhg.databinding.RvHorizontalListBinding


class LeDeviceListAdapter: RecyclerView.Adapter<LeDeviceListAdapter.BluetoothViewHolder>() {
    private val devices = mutableListOf<Pair<String, String>>()

    fun addDevice(device: Pair<String, String>) {
        if (!devices.contains(device)) {
            devices.add(device)
            notifyDataSetChanged() // 데이터가 변경되었음을 알립니다.
        }
    }
    inner class BluetoothViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceName: TextView = view.findViewById<TextView>(R.id.tvBtName)
        val deviceAddress : TextView = view.findViewById(R.id.tvBtAddress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BluetoothViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvBluetoothListBinding.inflate(inflater, parent, false)
        return BluetoothViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: BluetoothViewHolder, position: Int) {
        val currentItem = devices[position]
        holder.deviceName.text = currentItem.first
        holder.deviceAddress.text = currentItem.second
    }

    override fun getItemCount(): Int {
        return devices.size
    }
}