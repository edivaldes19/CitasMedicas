package com.manuel.citasmedicas.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.manuel.citasmedicas.R
import com.manuel.citasmedicas.databinding.FragmentDialogAddPatientsBinding
import com.manuel.citasmedicas.interfaces.OnPatientSelected
import com.manuel.citasmedicas.models.Patient
import com.manuel.citasmedicas.utils.Constants
import com.manuel.citasmedicas.utils.TextWatchers
import java.text.SimpleDateFormat
import java.util.*

class AddDialogFragmentPatients : DialogFragment(), DialogInterface.OnShowListener,
    RadioGroup.OnCheckedChangeListener {
    private var binding: FragmentDialogAddPatientsBinding? = null
    private var positiveButton: Button? = null
    private var negativeButton: Button? = null
    private var patient: Patient? = null
    private var sex = ""
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            binding = FragmentDialogAddPatientsBinding.inflate(LayoutInflater.from(context))
            binding?.let { view ->
                TextWatchers.validateFieldsAsYouType(
                    activity,
                    view.etID,
                    view.etName,
                    view.etLastName,
                    view.etMothersLastName,
                    view.etDateOfBirth,
                    view.etHeight,
                    view.etWeight,
                    view.etAllergies
                )
                view.etDateOfBirth.setOnClickListener {
                    val builder = MaterialDatePicker.Builder.datePicker()
                    val picker = builder.build()
                    picker.addOnPositiveButtonClickListener { timeInMilliseconds ->
                        val date =
                            SimpleDateFormat(Constants.DATE_PATTERN, Locale.getDefault()).apply {
                                timeZone = TimeZone.getTimeZone(Constants.TIME_ZONE)
                            }.format(timeInMilliseconds)
                        view.etDateOfBirth.setText(date)
                    }
                    picker.show(parentFragmentManager, picker.toString())
                }
                view.rgSex.setOnCheckedChangeListener(this)
                val builder =
                    MaterialAlertDialogBuilder(activity).setTitle(getString(R.string.add_patient))
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
        initializePatient()
        val dialog = dialog as? AlertDialog
        dialog?.let { alertDialog ->
            positiveButton = alertDialog.getButton(Dialog.BUTTON_POSITIVE)
            negativeButton = alertDialog.getButton(Dialog.BUTTON_NEGATIVE)
            patient?.let { positiveButton?.setText(getString(R.string.update)) }
            positiveButton?.setOnClickListener {
                binding?.let { view ->
                    enableAllInterface(false)
                    if (!theyAreEmpty()) {
                        if (view.rbMan.isChecked || view.rbWoman.isChecked) {
                            if (patient == null) {
                                val patient = Patient(
                                    id = view.etID.text.toString().trim(),
                                    nombre = view.etName.text.toString().trim(),
                                    apellidoPaterno = view.etLastName.text.toString().trim(),
                                    apellidoMaterno = view.etMothersLastName.text.toString().trim(),
                                    fechaNacimiento = view.etDateOfBirth.text.toString().trim(),
                                    sexo = sex,
                                    altura = view.etHeight.text.toString().trim(),
                                    peso = view.etWeight.text.toString().trim(),
                                    alergias = view.etAllergies.text.toString().trim(),
                                )
                                create(patient, view.etID.text.toString().trim())
                            } else {
                                patient?.apply {
                                    id = view.etID.text.toString().trim()
                                    nombre = view.etName.text.toString().trim()
                                    apellidoPaterno = view.etLastName.text.toString().trim()
                                    apellidoMaterno = view.etMothersLastName.text.toString().trim()
                                    fechaNacimiento = view.etDateOfBirth.text.toString().trim()
                                    sexo = sex
                                    altura = view.etHeight.text.toString().trim()
                                    peso = view.etWeight.text.toString().trim()
                                    alergias = view.etAllergies.text.toString().trim()
                                    update(this)
                                }
                            }
                        } else {
                            enableAllInterface(true)
                            Snackbar.make(
                                view.root,
                                getString(R.string.you_must_select_a_gender),
                                Snackbar.LENGTH_SHORT
                            ).setTextColor(Color.YELLOW).show()
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

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        binding?.let { view ->
            when (checkedId) {
                R.id.rbMan -> sex = view.rbMan.text.toString().trim()
                R.id.rbWoman -> sex = view.rbWoman.text.toString().trim()
            }
        }
    }

    private fun initializePatient() {
        patient = (activity as? OnPatientSelected)?.getPatientSelected()
        patient?.let { patient ->
            binding?.let { view ->
                dialog?.setTitle(getString(R.string.update_patient))
                view.etID.isEnabled = false
                view.etID.setText(patient.id)
                view.etName.setText(patient.nombre)
                view.etLastName.setText(patient.apellidoPaterno)
                view.etMothersLastName.setText(patient.apellidoMaterno)
                view.etDateOfBirth.setText(patient.fechaNacimiento)
                sex = patient.sexo.toString().trim()
                view.rbMan.isChecked = patient.sexo.equals(getString(R.string.men))
                view.rbWoman.isChecked = patient.sexo.equals(getString(R.string.woman))
                view.etHeight.setText(patient.altura)
                view.etWeight.setText(patient.peso)
                view.etAllergies.setText(patient.alergias)
            }
        }
    }

    private fun create(patient: Patient, documentId: String) {
        val db = Firebase.firestore
        db.collection(Constants.COLL_PATIENTS).document(documentId).set(patient)
            .addOnSuccessListener {
                Toast.makeText(
                    activity,
                    getString(R.string.patient_added_or_updated),
                    Toast.LENGTH_SHORT
                ).show()
                dismiss()
            }.addOnFailureListener {
                Snackbar.make(
                    binding!!.root,
                    getString(R.string.failed_to_add_patient),
                    Snackbar.LENGTH_SHORT
                ).setTextColor(Color.YELLOW).show()
            }.addOnCompleteListener {
                enableAllInterface(true)
            }
    }

    private fun update(patient: Patient) {
        val db = Firebase.firestore
        patient.id?.let { id ->
            db.collection(Constants.COLL_PATIENTS).document(id).set(patient).addOnSuccessListener {
                Toast.makeText(activity, getString(R.string.patient_updated), Toast.LENGTH_SHORT)
                    .show()
                dismiss()
            }.addOnFailureListener {
                Snackbar.make(
                    binding!!.root,
                    getString(R.string.failed_to_update_patient),
                    Snackbar.LENGTH_SHORT
                ).setTextColor(Color.YELLOW).show()
            }.addOnCompleteListener {
                enableAllInterface(true)
            }
        }
    }

    private fun enableAllInterface(enable: Boolean) {
        binding?.let { view ->
            with(view) {
                etID.isEnabled = enable
                etName.isEnabled = enable
                etLastName.isEnabled = enable
                etMothersLastName.isEnabled = enable
                etDateOfBirth.isEnabled = enable
                rbMan.isEnabled = enable
                rbWoman.isEnabled = enable
                etHeight.isEnabled = enable
                etWeight.isEnabled = enable
                etAllergies.isEnabled = enable
            }
        }
    }

    private fun theyAreEmpty(): Boolean {
        binding?.let { view ->
            with(view) {
                return etID.text.isNullOrEmpty() ||
                        etName.text.isNullOrEmpty() ||
                        etLastName.text.isNullOrEmpty() ||
                        etMothersLastName.text.isNullOrEmpty() ||
                        etDateOfBirth.text.isNullOrEmpty() ||
                        etHeight.text.isNullOrEmpty() ||
                        etWeight.text.isNullOrEmpty() ||
                        etAllergies.text.isNullOrEmpty()
            }
        }
        return false
    }
}