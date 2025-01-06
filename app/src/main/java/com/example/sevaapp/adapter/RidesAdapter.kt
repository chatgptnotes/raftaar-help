package com.example.sevaapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sevaapp.R
import com.example.sevaapp.models.Ride
class RidesAdapter(
    private val rideList: MutableList<Ride>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<RidesAdapter.RideViewHolder>() {

    class RideViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRideDate: TextView = view.findViewById(R.id.tvRideDate)
        val tvRideStatus: TextView = view.findViewById(R.id.tvRideStatus)
        val tvRideType: TextView = view.findViewById(R.id.tvRideType)
        val tvRideStartLocation: TextView = view.findViewById(R.id.tvRideStartLocation)
        val tvRideEndLocation: TextView = view.findViewById(R.id.tvRideEndLocation)
        val deleteImageButton: ImageView = view.findViewById(R.id.deleteImageButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RideViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ride, parent, false)
        return RideViewHolder(view)
    }

    override fun onBindViewHolder(holder: RideViewHolder, position: Int) {
        val ride = rideList[position]
        holder.tvRideDate.text = "${ride.date} ${ride.time}"
        holder.tvRideStatus.text = ride.status
        holder.tvRideType.text = "${ride.vehicle} · CRM ${ride.crm}"
        holder.tvRideStartLocation.text = "Pickup: ${ride.pickupLocation}"
        holder.tvRideEndLocation.text = "Drop: ${ride.dropLocation}"

        holder.deleteImageButton.setOnClickListener {
            onDeleteClick(position)
        }
    }

    override fun getItemCount(): Int = rideList.size
}
