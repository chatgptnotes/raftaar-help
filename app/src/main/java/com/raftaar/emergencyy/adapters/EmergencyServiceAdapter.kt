package com.raftaar.emergencyy.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.raftaar.emergencyy.R
import com.raftaar.emergencyy.models.EmergencyService

class EmergencyServiceAdapter(
    private var services: List<EmergencyService>,
    private val onCallClick: (EmergencyService) -> Unit
) : RecyclerView.Adapter<EmergencyServiceAdapter.ServiceViewHolder>() {

    class ServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.serviceName)
        val phoneText: TextView = view.findViewById(R.id.servicePhone)
        val descriptionText: TextView = view.findViewById(R.id.serviceDescription)
        val locationText: TextView = view.findViewById(R.id.serviceLocation)
        val availabilityText: TextView = view.findViewById(R.id.serviceAvailability)
        val callButton: MaterialButton = view.findViewById(R.id.callButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emergency_service, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = services[position]
        
        holder.nameText.text = service.name
        holder.phoneText.text = service.phoneNumber
        holder.descriptionText.text = service.description
        holder.locationText.text = service.location
        holder.availabilityText.text = if (service.isAvailable24x7) "24/7 Available" else ""
        
        holder.callButton.setOnClickListener { onCallClick(service) }
    }

    override fun getItemCount() = services.size

    fun updateServices(newServices: List<EmergencyService>) {
        services = newServices
        notifyDataSetChanged()
    }
} 