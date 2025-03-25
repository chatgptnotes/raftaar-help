package com.emergency.sevaapp.adapter

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.emergency.sevaapp.R
import com.emergency.sevaapp.models.Ride

class RidesAdapter(
    private val rideList: MutableList<Ride>,
//    private val onDeleteClick: (Int) -> Unit,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<RidesAdapter.RideViewHolder>() {

    class RideViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Updated IDs from your layout XML
        val tvTariff: TextView = view.findViewById(R.id.tvTariff)
        val tvDistance: TextView = view.findViewById(R.id.tvDistance)
        val tvPhone: TextView = view.findViewById(R.id.tvPhone)
        val tvLatitude: TextView = view.findViewById(R.id.tvLatitude)
        val tvLongitude: TextView = view.findViewById(R.id.tvLongitude)
        val tvHospitalId: TextView = view.findViewById(R.id.tvHospitalId)
        val tvRemark: TextView = view.findViewById(R.id.tvRemark)
        val tvDriverId: TextView = view.findViewById(R.id.tvDriverId)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvServiceType: TextView = view.findViewById(R.id.tvServiceType)
        val tvDriverIdNumber: TextView = view.findViewById(R.id.tvDriverIdNumber)
        val tvPatientId: TextView = view.findViewById(R.id.tvPatientId)
        val tvCreatedAt: TextView = view.findViewById(R.id.tvCreatedAt)
        val tvUpdatedAt: TextView = view.findViewById(R.id.tvUpdatedAt)
        val tvAddress: TextView = view.findViewById(R.id.tvAddress)
        val tvCity: TextView = view.findViewById(R.id.tvCity)
        val tvPinCode: TextView = view.findViewById(R.id.tvPinCode)
        val tvAssignedStaff: TextView = view.findViewById(R.id.tvAssignedStaff)
        val tvTelecallers: TextView = view.findViewById(R.id.tvTelecallers)
        val tvClicked: TextView = view.findViewById(R.id.tvClicked)
//        val deleteImageButton: ImageView = view.findViewById(R.id.deleteImageButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RideViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ride, parent, false)
        return RideViewHolder(view)
    }

    override fun onBindViewHolder(holder: RideViewHolder, position: Int) {
        val ride = rideList[position]

        // Formatting the text to make keys bold but values regular
        val hospitalName = ride.hospital?.hospital_name ?: "Not Available"
        val driverName = "${ride.driver?.first_name} ${ride.driver?.last_name}" ?: "Not Available"
        val serviceType = ride.driver?.service_type ?: "Not Available" // Access service type

        val driverNumber = ride.driver?.phone ?: "Not Available" // Access Driver Number
        val pickup = ride.address ?: "Not Available"
        val bookingDate = ride.created_at ?: "Not Available" // Booking Date

        // Apply bold styling using SpannableStringBuilder
        val spannableTariff = SpannableStringBuilder("Tariff: ")
        spannableTariff.setSpan(StyleSpan(Typeface.BOLD), 0, spannableTariff.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableTariff.append("${ride.tariff} INR")

        val spannableDistance = SpannableStringBuilder("Distance: ")
        spannableDistance.setSpan(StyleSpan(Typeface.BOLD), 0, spannableDistance.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableDistance.append("${ride.distance} km")

        // Use SpannableStringBuilder to append text with styles
        val spannableBookingDate = SpannableStringBuilder("Booking Date: ")
        spannableBookingDate.setSpan(StyleSpan(Typeface.BOLD), 0, spannableBookingDate.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableBookingDate.append(bookingDate) // Appending the value of booking date

        val spannableHospitalName = SpannableStringBuilder("Hospital Name: ")
        spannableHospitalName.setSpan(StyleSpan(Typeface.BOLD), 0, spannableHospitalName.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableHospitalName.append(hospitalName)

        val spannableDriverName = SpannableStringBuilder("Driver Name: ")
        spannableDriverName.setSpan(StyleSpan(Typeface.BOLD), 0, spannableDriverName.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableDriverName.append(driverName)

        val spannableDriverNumber = SpannableStringBuilder("Driver Number: ")
        spannableDriverNumber.setSpan(StyleSpan(Typeface.BOLD), 0, spannableDriverNumber.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableDriverNumber.append(driverNumber)

        // Check if status is "Pending"
        if (ride.status == 1) { // 1 = Pending
            holder.tvDriverIdNumber.visibility = View.VISIBLE
            holder.tvDriverIdNumber.text = spannableDriverNumber
            holder.tvDriverIdNumber.setTextColor(Color.BLACK) // Make it visually distinct

            holder.tvDriverIdNumber.setOnClickListener {
                makePhoneCall(holder.tvDriverIdNumber.context, driverNumber)
            }
        } else {
            holder.tvDriverIdNumber.visibility = View.GONE // Hide if not Pending
        }


        val spannableServiceType = SpannableStringBuilder("Ambulance Type: ")
        spannableServiceType.setSpan(StyleSpan(Typeface.BOLD), 0, spannableServiceType.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableServiceType.append(serviceType)

        val spannablePickup = SpannableStringBuilder("Pickup: ")
        spannablePickup.setSpan(StyleSpan(Typeface.BOLD), 0, spannablePickup.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannablePickup.append(pickup)

        // Apply bold to keys and normal to values
        holder.tvCreatedAt.text = spannableBookingDate
        holder.tvHospitalId.text = spannableHospitalName
        holder.tvDriverId.text = spannableDriverName
        holder.tvServiceType.text = spannableServiceType // Set the service type
        holder.tvDriverIdNumber.text = spannableDriverNumber // Set the service type
        holder.tvAddress.text = spannablePickup
        // Set values to UI elements
        holder.tvTariff.text = spannableTariff
        holder.tvDistance.text = spannableDistance

        // Change status text and color based on status value
        when (ride.status) {
            0 -> {
                holder.tvStatus.text = "Cancelled" // Display "Cancelled" for status 0
                holder.tvStatus.setTextColor(Color.RED) // Red for Cancelled
            }
            1 -> {
                holder.tvStatus.text = "Pending" // Display "Pending" for status 1
                holder.tvStatus.setTextColor(Color.rgb(255, 165, 0)) // Orange for Pending
            }
            2 -> {
                holder.tvStatus.text = "Completed" // Display "Completed" for status 2
                holder.tvStatus.setTextColor(Color.GREEN) // Green for Completed
            }
            else -> {
                holder.tvStatus.text = "Unknown" // Default text for other status values
                holder.tvStatus.setTextColor(Color.GRAY) // Default gray color
            }
        }

        // Binding the remaining data without bold formatting
        holder.tvPhone.text = "Phone: ${ride.phone ?: "Not Available"}"
        holder.tvLatitude.text = "Latitude: ${ride.latitude ?: "Not Available"}"
        holder.tvLongitude.text = "Longitude: ${ride.longitude ?: "Not Available"}"
        holder.tvRemark.text = "Remark: ${ride.remark ?: "Not Available"}"
        holder.tvPatientId.text = "Patient ID: ${ride.patient_id}"
        holder.tvUpdatedAt.text = "Updated At: ${ride.updated_at}"
        holder.tvCity.text = "City: ${ride.city ?: "Not Available"}"
        holder.tvPinCode.text = "Pin Code: ${ride.pin_code ?: "Not Available"}"
        holder.tvAssignedStaff.text = "Assigned Staff ID: ${ride.assigned_staff_id ?: "Not Available"}"
        holder.tvTelecallers.text = "Telecallers: ${ride.telecallers ?: "Not Available"}"
        holder.tvClicked.text = "Clicked: ${ride.clicked}"
//        holder.tvDriverIdNumber.text = "Driver Number: ${ride.driver?.phone ?: "Not Available"}"
    }

    private fun makePhoneCall(context: Context, phoneNumber: String) {
        val callIntent = Intent(Intent.ACTION_DIAL)
        callIntent.data = Uri.parse("tel:$phoneNumber")
        context.startActivity(callIntent)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            context.startActivity(callIntent)
        } else {
            Toast.makeText(context, "Permission required to make a call", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = rideList.size
}
