/*
MIT License

Copyright (c) 2023 Mads Cornelius Hansen

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

 */
package dk.itu.moapd.scootersharing.coha.fragments

import android.content.Context
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import dk.itu.moapd.scootersharing.coha.R
import dk.itu.moapd.scootersharing.coha.databinding.FragmentUpdateRideBinding
import dk.itu.moapd.scootersharing.coha.utils.GeoLocationService
import dk.itu.moapd.scootersharing.coha.utils.date_to_String

/**
 * A fragment that allows the user to update a scooter's location.
 */
class UpdateRideFragment : Fragment() {

    // Binding for the fragment's layout
    private var _binding: FragmentUpdateRideBinding? = null
    private val binding get() = _binding!!


    private lateinit var scooterKey: String



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = MainFragment.auth
        database = MainFragment.database
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the fragment's layout
        _binding = FragmentUpdateRideBinding.inflate(inflater,container,false)


        auth.currentUser?.let {
            Log.d(TAG,"Creating the DB Query")
            val query = MainFragment.database
                .child("scooter")
                .orderByChild("timestamp")
                .limitToLast(1)


            query.addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onDataChange(datasnapshot: DataSnapshot) {
                    if (datasnapshot.exists()) {
                        // Deserialize the dataSnapshot into an instance of the ScooterDatabase data class

                        datasnapshot.children.forEach { childSnapshot ->

                            val firstChildKey = datasnapshot.children.first().key // get the key of the first child

                            scooterKey = firstChildKey!!

                            val name = childSnapshot.child("name").getValue(String::class.java)
                            val location = childSnapshot.child("location").getValue(String::class.java)
                            val timestamp = childSnapshot.child("timestamp").getValue(Long::class.java)
                            val latitude = childSnapshot.child("latitude").getValue(Double::class.java)
                            val longitude = childSnapshot.child("longitude").getValue(Double::class.java)
                            binding.editTextName.setText(name)
                            binding.editTextLocation.setText(getCompleteAddressString(latitude!!,
                                longitude!!
                            ))
                            Log.d(TAG, "Name: ${name}\nLocation: ${location}\nTimestamp: $timestamp")
                        }




                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d(TAG, error.toString())

                }

            }

            )

        }
        Log.d(TAG,"Last Scooter retrieved ")
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.apply {

            // Get references to the UI elements
            val scooterName = binding.editTextName
            val scooterLocation = binding.editTextLocation

            // Retrieve the current scooter from the database and update the UI

           scooterName.setText("temp")
           scooterLocation.setText("temp")

            // Set up the click listener for the update ride button
            binding.updateRideButton.setOnClickListener{



                // Update the scooter's location and timestamp
                val newLocation = scooterLocation.text.toString().trim()
                val newTimestamp = System.currentTimeMillis()

                val scooterRef = database.child("scooter").child(scooterKey)
                val updateMap = mapOf(
                    "location" to newLocation,
                    "timestamp" to newTimestamp,
                    "latitude" to GeoLocationService.coordinates.latitude,
                    "longitude" to GeoLocationService.coordinates.longitude
                )

                scooterRef.updateChildren(updateMap)

                Log.d(TAG,"Scooter updated ")
                // Reset the UI fields and hide the keyboard
                scooterLocation.text?.clear()
                view.hideKeyboard()

                // Show a snackbar with a message confirming the ride update
                showSnackbar(getString(R.string.Start_Ride, binding.editTextName.text.toString(), newLocation, date_to_String(newTimestamp)),view)


                // Navigate back to the main fragment
                findNavController().navigate(R.id.show_mainFragment)
            }
        }
    }
    fun getCompleteAddressString(lat:Double,long:Double): String {
        var strAdd = ""
        val geocoder = Geocoder(requireContext())
        geocoder.getFromLocation(lat, long, 1)
        try {
            val addresses = geocoder.getFromLocation(lat, long, 1)
            if ((addresses != null) && (addresses.size != 0)) {
                val returnedAddress = addresses[0]

                val address =
                    returnedAddress.getAddressLine(0)


                strAdd = address
                Log.w("My Current location address", address)
            } else {
                Log.w("My Current location address", "No Address returned!")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.w("My Current location address", "Cannot get Address!")
        }
        return strAdd
    }
    companion object {
        private val TAG = UpdateRideFragment::class.qualifiedName


        // The scooter that is currently being updated
        private lateinit var auth: FirebaseAuth
        private lateinit var database: DatabaseReference
    }


    /**
     * The `showMessage` function prints a message in Log with the `Scooter` instance information.
     */


    /**
     * Display a Snackbar with the given message
     *
     * @param message The message to be displayed
     * @param view The view to use for displaying the Snackbar
     * @param duration The duration of the Snackbar display nanoseconds
     */
    private fun showSnackbar(message: String, view: View) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
    }
    /**
     * Hides the keyboard from the view.
     * This method obtains an instance of the input method manager from the context and calls the
     * 'hideSoftInputFromWindow' method to hide the keyboard from the view.
     */
    private fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }
}