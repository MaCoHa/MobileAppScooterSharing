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
import com.google.firebase.database.DatabaseReference
import dk.itu.moapd.scootersharing.coha.R
import dk.itu.moapd.scootersharing.coha.databinding.FragmentStartRideBinding
import dk.itu.moapd.scootersharing.coha.models.Scooter
import dk.itu.moapd.scootersharing.coha.utils.GeoLocationService


/**
 * A fragment that allows users to start a ride by adding a scooter with its name and location.
 */
class StartRideFragment : Fragment() {
    private var _binding: FragmentStartRideBinding? = null

    // Use backing property to avoid null safety checks every time binding is used.
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate the layout for this fragment using view binding.
        _binding = FragmentStartRideBinding.inflate(inflater, container, false)
        auth = MainFragment.auth
        database = MainFragment.database
        return binding.root

    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Avoid memory leaks by setting the binding to null when the view is destroyed.
        _binding = null

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        binding.apply {

            val scooterName = binding.editTextName
            val scooterLocation = binding.editTextLocation




            scooterLocation.setText(getCompleteAddressString())

            binding.startRideButton.setOnClickListener{

                // Check if scooter name and location are not empty.
                if (scooterName.text.toString().isNotEmpty() &&
                    scooterLocation.text.toString().isNotEmpty()) {

                    // Get the name and location of the new scooter from the text fields.
                    val name = scooterName.text.toString().trim()
                    val location = scooterLocation.text.toString().trim()

                    // Create a new scooter object with the current time as its creation time.
                    scooter = Scooter(name, location, System.currentTimeMillis())


                    val dbscooter = Scooter(scooter.name, scooter.location, scooter.timestamp,GeoLocationService.coordinates.latitude,GeoLocationService.coordinates.longitude)
                    auth.currentUser?.let { user ->
                        val uid = database.child("scooter")
                            .child(user.uid)
                            .push()
                            .key

                        // Insert the object in the database.
                        uid?.let {
                            database.child("scooter")
                                .child(it)
                                .setValue(dbscooter)
                        }
                    }




                    // Reset the UI fields.
                    scooterName.text?.clear()
                    scooterLocation.text?.clear()

                    // Hide the keyboard.
                    view.hideKeyboard()

                    // Show a success message to the user.
                    showSnackbar(getString(R.string.Start_Ride, scooter.name, scooter.location, scooter.time_to_String()), view, 2000)
                    showMessage()

                    // Navigate back to the main fragment.
                    findNavController().navigate(R.id.show_mainFragment)
                }
            }
        }
    }

    companion object {
        private val TAG = StartRideFragment::class.qualifiedName
        // Lateinit variable to hold the newly added scooter.
        lateinit var scooter: Scooter
        private lateinit var auth: FirebaseAuth
        private lateinit var database: DatabaseReference
    }


    /**
     * The `showMessage` function prints a message in Log with the `Scooter` instance information.
     */
    private fun showMessage(){
        Log.d(TAG, scooter.toString())
    }

    /**
     * Display a Snackbar with the given message
     *
     * @param message The message to be displayed
     * @param view The view to use for displaying the Snackbar
     * @param duration The duration of the Snackbar display nanoseconds
     */
    private fun showSnackbar(message: String, view: View, duration: Int = Snackbar.LENGTH_LONG) {
        Snackbar.make(view, message, duration).show()
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

    fun getCompleteAddressString(): String {
        var strAdd = ""
        val geocoder = Geocoder(requireContext())
        geocoder.getFromLocation(GeoLocationService.coordinates.latitude, GeoLocationService.coordinates.longitude, 1)
        try {
            val addresses = geocoder.getFromLocation(GeoLocationService.coordinates.latitude, GeoLocationService.coordinates.longitude, 1)
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

}