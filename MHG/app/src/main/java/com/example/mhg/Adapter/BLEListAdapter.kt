package com.example.mhg.Adapter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.ContentValues
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.mhg.R
import com.example.mhg.ReportGoalFragment

class BLEListAdapter(private val deviceList: List<ReportGoalFragment.BluetoothDeviceInfo>, private val listener:onDeviceClickListener) :
        RecyclerView.Adapter<BLEListAdapter.ViewHolder>() {
            interface onDeviceClickListener {
                fun onDeviceClick(device: ReportGoalFragment.BluetoothDeviceInfo)
                fun onDeviceLongClick(device: ReportGoalFragment.BluetoothDeviceInfo) : Boolean
            }
    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var tv_device_name: TextView = v.findViewById(R.id.tvBtName)
        var tv_mac_address: TextView = v.findViewById(R.id.tvBtAddress)
        var tv_rssi: TextView = v.findViewById(R.id.tvBleSearched)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDeviceClick(deviceList[position])
                }
            }
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDeviceLongClick(deviceList[position])
                }
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BLEListAdapter.ViewHolder {
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.rv_ble_list, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: BLEListAdapter.ViewHolder, position: Int) {
        val item = deviceList[position]
        holder.tv_device_name.text = item.device_name
        holder.tv_mac_address.text = item.mac_address
        holder.tv_rssi.text = item.rssi
    }

    override fun getItemCount(): Int {
        return deviceList.size
    }

}
