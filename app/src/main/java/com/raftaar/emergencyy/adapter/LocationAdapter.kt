package com.raftaar.emergencyy.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.raftaar.emergencyy.R
import com.raftaar.emergencyy.models.LocationModel

class LocationAdapter(
    private val locationList: MutableList<LocationModel>,
    private val onEditClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit,
    private val onItemClick: (LocationModel) -> Unit // Add this for item click
) : RecyclerView.Adapter<LocationAdapter.LocationViewHolder>() {

    private var lastPosition = -1

    inner class LocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val label: TextView = itemView.findViewById(R.id.location_label)
        val address: TextView = itemView.findViewById(R.id.location_address)
        val latitude: TextView = itemView.findViewById(R.id.location_latitude)
        val longitude: TextView = itemView.findViewById(R.id.location_longitude)
        val city: TextView = itemView.findViewById(R.id.location_city)
        val pincode: TextView = itemView.findViewById(R.id.location_pincode)
        val editButton: ImageView = itemView.findViewById(R.id.edit_button)
        val deleteButton: ImageView = itemView.findViewById(R.id.delete_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_location, parent, false) // Ensure the layout contains all required fields
        return LocationViewHolder(view)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        val location = locationList[position]
        holder.label.text = location.label
        holder.address.text = location.address
        holder.latitude.text = "Latitude: ${location.latitude}"
        holder.longitude.text = "Longitude: ${location.longitude}"
        holder.city.text = "City: ${location.city}"
        holder.pincode.text = "Pincode: ${location.pincode}"

        // Apply floating animation with delay based on position
        val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.float_animation)
        animation.startOffset = (position * 200).toLong() // Stagger the animations
        holder.itemView.startAnimation(animation)

        // Set up Edit button click listener
        holder.editButton.setOnClickListener {
            onEditClick(position)
        }

        // Set up Delete button click listener
        holder.deleteButton.setOnClickListener {
            onDeleteClick(position)
        }

        // Set up item click listener
        holder.itemView.setOnClickListener {
            onItemClick(location) // Trigger the callback with the clicked location
        }
    }
    override fun getItemCount(): Int = locationList.size
}
