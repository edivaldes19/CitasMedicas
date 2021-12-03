package com.manuel.citasmedicas.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.manuel.citasmedicas.R
import com.manuel.citasmedicas.databinding.ItemPatientBinding
import com.manuel.citasmedicas.interfaces.OnPatientListener
import com.manuel.citasmedicas.models.Patient

class PatientAdapter(
    private var patientList: MutableList<Patient>,
    private val listener: OnPatientListener
) : RecyclerView.Adapter<PatientAdapter.ViewHolder>() {
    private lateinit var context: Context
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_patient, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.root.animation =
            AnimationUtils.loadAnimation(context, R.anim.fade_transition)
        val patient = patientList[position]
        holder.setListener(patient)
        holder.binding.tvIDPatient.text = "${context.getString(R.string.id)}: ${patient.id}"
        holder.binding.tvFullName.text =
            "${patient.nombre} ${patient.apellidoPaterno} ${patient.apellidoMaterno}"
        holder.binding.tvDateOfBirth.text =
            "${context.getString(R.string.date_of_birth)}: ${patient.fechaNacimiento}"
        holder.binding.tvSex.text = "${context.getString(R.string.sex)}: ${patient.sexo}"
        holder.binding.tvHeight.text = "${context.getString(R.string.height)}: ${patient.altura}"
        holder.binding.tvWeight.text = "${context.getString(R.string.weight)}: ${patient.peso}"
        holder.binding.tvAllergies.text =
            "${context.getString(R.string.allergies)}: ${patient.alergias}"
    }

    override fun getItemCount() = patientList.size
    fun add(patient: Patient) {
        if (!patientList.contains(patient)) {
            patientList.add(patient)
            notifyItemInserted(patientList.size - 1)
        } else {
            update(patient)
        }
    }

    fun update(patient: Patient) {
        val index = patientList.indexOf(patient)
        if (index != -1) {
            patientList[index] = patient
            notifyItemChanged(index)
        }
    }

    fun delete(patient: Patient) {
        val index = patientList.indexOf(patient)
        if (index != -1) {
            patientList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(list: MutableList<Patient>) {
        patientList = list
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemPatientBinding.bind(view)
        fun setListener(patient: Patient) {
            binding.root.setOnClickListener {
                listener.onClick(patient)
            }
            binding.root.setOnLongClickListener {
                listener.onLongClick(patient)
                true
            }
        }
    }
}