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
import com.manuel.citasmedicas.adapters.AppointmentAdapter
import com.manuel.citasmedicas.databinding.ActivityAppointmentsBinding
import com.manuel.citasmedicas.fragments.AddDialogFragmentAppointments
import com.manuel.citasmedicas.interfaces.OnAppointmentListener
import com.manuel.citasmedicas.interfaces.OnAppointmentSelected
import com.manuel.citasmedicas.models.Appointment
import com.manuel.citasmedicas.utils.ConnectionReceiver
import com.manuel.citasmedicas.utils.Constants

class AppointmentsActivity : AppCompatActivity(), OnAppointmentListener, OnAppointmentSelected,
    ConnectionReceiver.ReceiverListener {
    private lateinit var binding: ActivityAppointmentsBinding
    private lateinit var appointmentAdapter: AppointmentAdapter
    private var appointmentSelected: Appointment? = null
    private var appointmentList = mutableListOf<Appointment>()
    private var listenerRegistration: ListenerRegistration? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppointmentsBinding.inflate(layoutInflater)
        setContentView(binding.root)
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.appointment_menu, menu)
        val menuItem = menu?.findItem(R.id.action_search)
        val searchView = menuItem?.actionView as SearchView
        searchView.queryHint = getString(R.string.search_appointment)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = mutableListOf<Appointment>()
                for (appointment in appointmentList) {
                    if (newText!!.lowercase() in appointment.idCita.toString().lowercase()) {
                        filteredList.add(appointment)
                    }
                }
                appointmentAdapter.updateList(filteredList)
                if (filteredList.isNullOrEmpty()) {
                    binding.tvWithoutResults.visibility = View.VISIBLE
                    binding.tvTotalElements.visibility = View.GONE
                } else {
                    binding.tvWithoutResults.visibility = View.GONE
                    updateTheTotalNumberOfItems(
                        getString(R.string.appointments_found),
                        filteredList.size
                    )
                }
                if (newText.isNullOrEmpty()) {
                    updateTheTotalNumberOfItems(getString(R.string.appointments), filteredList.size)
                }
                return false
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    override fun onClick(appointment: Appointment) {
        appointmentSelected = appointment
        AddDialogFragmentAppointments().show(
            supportFragmentManager,
            AddDialogFragmentAppointments::class.java.simpleName
        )
    }

    override fun onLongClick(appointment: Appointment) {
        MaterialAlertDialogBuilder(this).setTitle(getString(R.string.delete))
            .setMessage(
                "¿${getString(R.string.are_you_sure_of)} ${getString(R.string.delete).lowercase()} la ${
                    getString(
                        R.string.appointment
                    ).lowercase()
                } ${appointment.idCita}?"
            )
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                appointment.idCita?.let { id ->
                    val db = Firebase.firestore
                    val reference = db.collection(Constants.COLL_APPOINTMENTS)
                    reference.document(id).delete().addOnSuccessListener {
                        Toast.makeText(
                            this,
                            getString(R.string.appointment_removed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }.addOnFailureListener {
                        Snackbar.make(
                            binding.root,
                            getString(R.string.failed_to_remove_appointment),
                            Snackbar.LENGTH_SHORT
                        ).setTextColor(Color.YELLOW).show()
                    }
                }
            }.setNegativeButton(getString(R.string.cancel), null).show()
    }

    override fun getAppointmentSelected() = appointmentSelected
    override fun onNetworkChange(isConnected: Boolean) = showNetworkErrorSnackBar(isConnected)
    private fun setupRecyclerView() {
        appointmentAdapter = AppointmentAdapter(appointmentList, this)
        binding.rvAppointments.apply {
            layoutManager = LinearLayoutManager(this@AppointmentsActivity)
            adapter = this@AppointmentsActivity.appointmentAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy < 0) {
                        binding.eFabNewAppointment.show()
                    } else if (dy > 0) {
                        binding.eFabNewAppointment.hide()
                    }
                }
            })
        }
    }

    private fun setupButtons() {
        binding.eFabNewAppointment.setOnClickListener {
            appointmentSelected = null
            AddDialogFragmentAppointments().show(
                supportFragmentManager,
                AddDialogFragmentAppointments::class.java.simpleName
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
        val reference = db.collection(Constants.COLL_APPOINTMENTS)
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
                val appointment = documentChange.document.toObject(Appointment::class.java)
                appointment.idCita = documentChange.document.id
                when (documentChange.type) {
                    DocumentChange.Type.ADDED -> {
                        appointmentAdapter.add(appointment)
                        updateTheTotalNumberOfItems(
                            getString(R.string.appointments),
                            appointmentAdapter.itemCount
                        )
                    }
                    DocumentChange.Type.MODIFIED -> {
                        appointmentAdapter.update(appointment)
                        updateTheTotalNumberOfItems(
                            getString(R.string.appointments),
                            appointmentAdapter.itemCount
                        )
                    }
                    DocumentChange.Type.REMOVED -> {
                        appointmentAdapter.delete(appointment)
                        updateTheTotalNumberOfItems(
                            getString(R.string.appointments),
                            appointmentAdapter.itemCount
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