package com.example.mhg.Adapter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.mhg.R
import com.example.mhg.databinding.RvBleListBinding

class BLEListAdapter(private val onDeviceClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    val devices = mutableListOf<BluetoothDevice>()
    inner class viewHolder(view: View): RecyclerView.ViewHolder(view) {
        val tvBtName = view.findViewById<TextView>(R.id.tvBtName)
        val tvBtAddress = view.findViewById<TextView>(R.id.tvBtAddress)
        val clBle = view.findViewById<ConstraintLayout>(R.id.clBle)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvBleListBinding.inflate(inflater, parent, false)
        return viewHolder(binding.root)
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = devices[position]

        if (holder is viewHolder) {
            holder.tvBtName.text = currentItem.name.toString()
            holder.tvBtAddress.text = currentItem.address.toString()
            holder.clBle.setOnClickListener {
                onDeviceClick(currentItem)

            }
        }
    }
    override fun getItemCount(): Int {
        return devices.size
    }

    fun addDevice(device: BluetoothDevice) {
        devices.add(device)
        notifyDataSetChanged()
    }
}