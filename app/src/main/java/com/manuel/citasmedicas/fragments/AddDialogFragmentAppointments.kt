package com.manuel.citasmedicas.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.manuel.citasmedicas.R
import com.manuel.citasmedicas.databinding.FragmentDialogAddAppointmentsBinding
import com.manuel.citasmedicas.interfaces.OnAppointmentSelected
import com.manuel.citasmedicas.models.Appointment
import com.manuel.citasmedicas.utils.Constants
import com.manuel.citasmedicas.utils.TextWatchers
import java.text.SimpleDateFormat
import java.util.*

class AddDialogFragmentAppointments : DialogFragment(), DialogInterface.OnShowListener,
    AdapterView.OnItemClickListener {
    private var binding: FragmentDialogAddAppointmentsBinding? = null
    private var positiveButton: Button? = null
    private var negativeButton: Button? = null
    private var appointment: Appointment? = null
    private var patientId = ""
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            binding = FragmentDialogAddAppointmentsBinding.inflate(LayoutInflater.from(context))
            binding?.let { view ->
                TextWatchers.validateFieldsAsYouType(
                    activity,
                    view.etIDAppointment,
                    view.etAppointmentDate,
                    view.etAppointmentTime
                )
                view.etAppointmentDate.setOnClickListener {
                    val builder = MaterialDatePicker.Builder.datePicker()
                    val picker = builder.build()
                    picker.addOnPositiveButtonClickListener { timeInMilliseconds ->
                        val date =
                            SimpleDateFormat(Constants.DATE_PATTERN, Locale.getDefault()).apply {
                                timeZone = TimeZone.getTimeZone(Constants.TIME_ZONE)
                            }.format(timeInMilliseconds)
                        view.etAppointmentDate.setText(date)
                    }
                    picker.show(parentFragmentManager, picker.toString())
                }
                view.etAppointmentTime.setOnClickListener {
                    MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_24H).setHour(12)
                        .setMinute(0).setTitleText(getString(R.string.appointment_time)).build()
                        .apply {
                            addOnPositiveButtonClickListener {
                                onTimeSelected(
                                    this.hour,
                                    this.minute
                                )
                            }
                        }.show(parentFragmentManager, Constants.TIME_PICKER_LABEL)
                }
                val arrayAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    getAllPatientIds()
                )
                view.atvIDPatient.setAdapter(arrayAdapter)
                view.atvIDPatient.onItemClickListener = this
                val builder =
                    MaterialAlertDialogBuilder(activity).setTitle(getString(R.string.add_appointment))
                        .setPositiveButton(getString(R.string.add), null)
                        .setNegativeButton(getString(R.string.cancel), null).setView(view.root)
                val dialog = builder.create()
                dialog.setOnShowListener(this)
                return dialog
            }
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onShow(dialogInterface: DialogInterface?) {
        initializeAppointment()
        val dialog = dialog as? AlertDialog
        dialog?.let { alertDialog ->
            positiveButton = alertDialog.getButton(Dialog.BUTTON_POSITIVE)
            negativeButton = alertDialog.getButton(Dialog.BUTTON_NEGATIVE)
            appointment?.let { positiveButton?.setText(getString(R.string.update)) }
            positiveButton?.setOnClickListener {
                binding?.let { view ->
                    enableAllInterface(false)
                    if (!theyAreEmpty()) {
                        if (appointment == null) {
                            val appointment = Appointment(
                                idCita = view.etIDAppointment.text.toString().trim(),
                                idPaciente = view.atvIDPatient.text.toString().trim(),
                                fechaCita = view.etAppointmentDate.text.toString().trim(),
                                horaCita = view.etAppointmentTime.text.toString().trim(),
                            )
                            create(appointment, view.etIDAppointment.text.toString().trim())
                        } else {
                            appointment?.apply {
                                idCita = view.etIDAppointment.text.toString().trim()
                                idPaciente = view.atvIDPatient.text.toString().trim()
                                fechaCita = view.etAppointmentDate.text.toString().trim()
                                horaCita = view.etAppointmentTime.text.toString().trim()
                                update(this)
                            }
                        }
                    } else {
                        enableAllInterface(true)
                        Snackbar.make(
                            view.root,
                            getString(R.string.there_are_still_empty_fields),
                            Snackbar.LENGTH_SHORT
                        ).setTextColor(Color.YELLOW).show()
                    }
                }
            }
            negativeButton?.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun initializeAppointment() {
        appointment = (activity as? OnAppointmentSelected)?.getAppointmentSelected()
        appointment?.let { appointment ->
            binding?.let { view ->
                dialog?.setTitle(getString(R.string.update_appointment))
                patientId = appointment.idPaciente.toString()
                view.etIDAppointment.isEnabled = false
                view.etIDAppointment.setText(appointment.idCita)
                view.atvIDPatient.setText(appointment.idPaciente, false)
                view.etAppointmentDate.setText(appointment.fechaCita)
                view.etAppointmentTime.setText(appointment.horaCita)
            }
        }
    }

    private fun create(appointment: Appointment, documentId: String) {
        val db = Firebase.firestore
        db.collection(Constants.COLL_APPOINTMENTS).document(documentId).set(appointment)
            .addOnSuccessListener {
                Toast.makeText(
                    activity,
                    getString(R.string.appointment_added_or_updated),
                    Toast.LENGTH_SHORT
                ).show()
                dismiss()
            }.addOnFailureListener {
                Snackbar.make(
                    binding!!.root,
                    getString(R.string.failed_to_add_appointment),
                    Snackbar.LENGTH_SHORT
                ).setTextColor(Color.YELLOW).show()
            }.addOnCompleteListener {
                enableAllInterface(true)
            }
    }

    private fun update(appointment: Appointment) {
        val db = Firebase.firestore
        appointment.idCita?.let { id ->
            db.collection(Constants.COLL_APPOINTMENTS).document(id).set(appointment)
                .addOnSuccessListener {
                    Toast.makeText(
                        activity,
                        getString(R.string.appointment_updated),
                        Toast.LENGTH_SHORT
                    ).show()
                    dismiss()
                }.addOnFailureListener {
                    Snackbar.make(
                        binding!!.root,
                        getString(R.string.failed_to_update_appointment),
                        Snackbar.LENGTH_SHORT
                    ).setTextColor(Color.YELLOW).show()
                }.addOnCompleteListener {
                    enableAllInterface(true)
                }
        }
    }

    private fun getAllPatientIds(): MutableList<String> {
        val listOfIds = mutableListOf<String>()
        val db = Firebase.firestore
        db.collection(Constants.COLL_PATIENTS).get().addOnSuccessListener { result ->
            for (document in result) {
                listOfIds.add(document.id)
            }
        }.addOnFailureListener {
            Snackbar.make(
                binding!!.root,
                getString(R.string.failed_to_get_all_patient_ids),
                Snackbar.LENGTH_SHORT
            ).setTextColor(Color.YELLOW).show()
        }
        return listOfIds
    }

    private fun enableAllInterface(enable: Boolean) {
        binding?.let { view ->
            with(view) {
                etIDAppointment.isEnabled = enable
                atvIDPatient.isEnabled = enable
                etAppointmentDate.isEnabled = enable
                etAppointmentTime.isEnabled = enable
            }
        }
    }

    private fun theyAreEmpty(): Boolean {
        binding?.let { view ->
            with(view) {
                return etIDAppointment.text.isNullOrEmpty() ||
                        atvIDPatient.text.isNullOrEmpty() ||
                        etAppointmentDate.text.isNullOrEmpty() ||
                        etAppointmentTime.text.isNullOrEmpty()
            }
        }
        return false
    }

    private fun onTimeSelected(hour: Int, minute: Int) {
        val hourAsText = if (hour < 10) "0$hour" else hour
        val minuteAsText = if (minute < 10) "0$minute" else minute
        val showTime = "$hourAsText:$minuteAsText"
        binding?.etAppointmentTime?.setText(showTime)
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        patientId = parent?.getItemAtPosition(position).toString().trim()
    }
}