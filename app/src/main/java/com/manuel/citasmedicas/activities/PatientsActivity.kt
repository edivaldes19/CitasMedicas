package com.manuel.citasmedicas.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.manuel.citasmedicas.R
import com.manuel.citasmedicas.adapters.PatientAdapter
import com.manuel.citasmedicas.databinding.ActivityPatientsBinding
import com.manuel.citasmedicas.fragments.AddDialogFragmentPatients
import com.manuel.citasmedicas.interfaces.OnPatientListener
import com.manuel.citasmedicas.interfaces.OnPatientSelected
import com.manuel.citasmedicas.models.Patient
import com.manuel.citasmedicas.utils.ConnectionReceiver
import com.manuel.citasmedicas.utils.Constants

class PatientsActivity : AppCompatActivity(), OnPatientListener, OnPatientSelected,
    ConnectionReceiver.ReceiverListener {
    private lateinit var binding: ActivityPatientsBinding
    private lateinit var patientAdapter: PatientAdapter
    private var patientSelected: Patient? = null
    private var patientList = mutableListOf<Patient>()
    private var listenerRegistration: ListenerRegistration? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_CitasMedicas)
        super.onCreate(savedInstanceState)
        binding = ActivityPatientsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = getString(R.string.patients)
        setupRecyclerView()
        setupButtons()
        checkInternetConnection()
    }

    override fun onResume() {
        super.onResume()
        setupFirestoreInRealtime()
    }

    override fun onPause() {
        super.onPause()
        listenerRegistration?.remove()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.patient_menu, menu)
        val menuItem = menu?.findItem(R.id.action_search)
        val searchView = menuItem?.actionView as SearchView
        searchView.queryHint = getString(R.string.search_patient)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = mutableListOf<Patient>()
                for (patient in patientList) {
                    if (newText!!.lowercase() in patient.nombre.toString().lowercase()) {
                        filteredList.add(patient)
                    }
                }
                patientAdapter.updateList(filteredList)
                if (filteredList.isNullOrEmpty()) {
                    binding.tvWithoutResults.visibility = View.VISIBLE
                    binding.tvTotalElements.visibility = View.GONE
                } else {
                    binding.tvWithoutResults.visibility = View.GONE
                    updateTheTotalNumberOfItems(
                        getString(R.string.patients_found),
                        filteredList.size
                    )
                }
                if (newText.isNullOrEmpty()) {
                    updateTheTotalNumberOfItems(getString(R.string.patients), filteredList.size)
                }
                return false
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_see_medical_appointments) {
            startActivity(Intent(this, AppointmentsActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(patient: Patient) {
        patientSelected = patient
        AddDialogFragmentPatients().show(
            supportFragmentManager,
            AddDialogFragmentPatients::class.java.simpleName
        )
    }

    override fun onLongClick(patient: Patient) {
        MaterialAlertDialogBuilder(this).setTitle(getString(R.string.delete))
            .setMessage("¿${getString(R.string.are_you_sure_of)} ${getString(R.string.delete).lowercase()} a ${patient.nombre} ${patient.apellidoPaterno} ${patient.apellidoMaterno}?")
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                patient.id?.let { id ->
                    val db = Firebase.firestore
                    val reference = db.collection(Constants.COLL_PATIENTS)
                    reference.document(id).delete().addOnSuccessListener {
                        Toast.makeText(
                            this,
                            getString(R.string.patient_removed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }.addOnFailureListener {
                        Snackbar.make(
                            binding.root,
                            getString(R.string.failed_to_remove_patient),
                            Snackbar.LENGTH_SHORT
                        ).setTextColor(Color.YELLOW).show()
                    }
                }
            }.setNegativeButton(getString(R.string.cancel), null).show()
    }

    override fun getPatientSelected() = patientSelected
    override fun onNetworkChange(isConnected: Boolean) = showNetworkErrorSnackBar(isConnected)
    private fun setupRecyclerView() {
        patientAdapter = PatientAdapter(patientList, this)
        binding.rvPatients.apply {
            layoutManager = LinearLayoutManager(this@PatientsActivity)
            adapter = this@PatientsActivity.patientAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy < 0) {
                        binding.eFabNewPatient.show()
                    } else if (dy > 0) {
                        binding.eFabNewPatient.hide()
                    }
                }
            })
        }
    }

    private fun setupButtons() {
        binding.eFabNewPatient.setOnClickListener {
            patientSelected = null
            AddDialogFragmentPatients().show(
                supportFragmentManager,
                AddDialogFragmentPatients::class.java.simpleName
            )
        }
    }

    private fun checkInternetConnection() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Constants.ACTION_INTENT)
        registerReceiver(ConnectionReceiver(), intentFilter)
        ConnectionReceiver.receiverListener = this
        val manager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = manager.activeNetworkInfo
        val isConnected = networkInfo != null && networkInfo.isConnectedOrConnecting
        showNetworkErrorSnackBar(isConnected)
    }

    private fun showNetworkErrorSnackBar(isConnected: Boolean) {
        if (!isConnected) {
            Snackbar.make(
                binding.root,
                getString(R.string.no_network_connection),
                Snackbar.LENGTH_INDEFINITE
            ).setTextColor(Color.WHITE)
                .setAction(getString(R.string.go_to_settings)) { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
                .show()
        }
    }

    private fun setupFirestoreInRealtime() {
        val db = Firebase.firestore
        val reference = db.collection(Constants.COLL_PATIENTS)
        listenerRegistration = reference.addSnapshotListener { querySnapshot, error ->
            if (error != null) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.failed_to_query_the_data),
                    Snackbar.LENGTH_SHORT
                ).setTextColor(Color.YELLOW).show()
                return@addSnapshotListener
            }
            for (documentChange in querySnapshot!!.documentChanges) {
                val patient = documentChange.document.toObject(Patient::class.java)
                patient.id = documentChange.document.id
                when (documentChange.type) {
                    DocumentChange.Type.ADDED -> {
                        patientAdapter.add(patient)
                        updateTheTotalNumberOfItems(
                            getString(R.string.patients),
                            patientAdapter.itemCount
                        )
                    }
                    DocumentChange.Type.MODIFIED -> {
                        patientAdapter.update(patient)
                        updateTheTotalNumberOfItems(
                            getString(R.string.patients),
                            patientAdapter.itemCount
                        )
                    }
                    DocumentChange.Type.REMOVED -> {
                        patientAdapter.delete(patient)
                        updateTheTotalNumberOfItems(
                            getString(R.string.patients),
                            patientAdapter.itemCount
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateTheTotalNumberOfItems(tag: String, size: Int) {
        binding.tvTotalElements.apply {
            visibility = View.VISIBLE
            text = "$tag: $size"
        }
    }
}