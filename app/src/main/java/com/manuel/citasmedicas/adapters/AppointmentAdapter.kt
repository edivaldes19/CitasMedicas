package com.manuel.citasmedicas.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.manuel.citasmedicas.R
import com.manuel.citasmedicas.databinding.ItemAppointmentBinding
import com.manuel.citasmedicas.interfaces.OnAppointmentListener
import com.manuel.citasmedicas.models.Appointment

class AppointmentAdapter(
    private var appointmentList: MutableList<Appointment>,
    private val listener: OnAppointmentListener
) : RecyclerView.Adapter<AppointmentAdapter.ViewHolder>() {
    private lateinit var context: Context
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_appointment, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.root.animation = AnimationUtils.loadAnimation(context, R.anim.slide)
        val appointment = appointmentList[position]
        holder.setListener(appointment)
        holder.binding.tvIDAppointment.text =
            "${context.getString(R.string.appointment_id)}: ${appointment.idCita}"
        holder.binding.tvIDPatient.text =
            "${context.getString(R.string.patient_id)}: ${appointment.idPaciente}"
        holder.binding.tvAppointmentDate.text =
            "${context.getString(R.string.appointment_date)}: ${appointment.fechaCita}"
        holder.binding.tvAppointmentTime.text =
            "${context.getString(R.string.appointment_time)}: ${appointment.horaCita}"
    }

    override fun getItemCount() = appointmentList.size
    fun add(appointment: Appointment) {
        if (!appointmentList.contains(appointment)) {
            appointmentList.add(appointment)
            notifyItemInserted(appointmentList.size - 1)
        } else {
            update(appointment)
        }
    }

    fun update(appointment: Appointment) {
        val index = appointmentList.indexOf(appointment)
        if (index != -1) {
            appointmentList[index] = appointment
            notifyItemChanged(index)
        }
    }

    fun delete(appointment: Appointment) {
        val index = appointmentList.indexOf(appointment)
        if (index != -1) {
            appointmentList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(list: MutableList<Appointment>) {
        appointmentList = list
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemAppointmentBinding.bind(view)
        fun setListener(appointment: Appointment) {
            binding.root.setOnClickListener {
                listener.onClick(appointment)
            }
            binding.root.setOnLongClickListener {
                listener.onLongClick(appointment)
                true
            }
        }
    }
}