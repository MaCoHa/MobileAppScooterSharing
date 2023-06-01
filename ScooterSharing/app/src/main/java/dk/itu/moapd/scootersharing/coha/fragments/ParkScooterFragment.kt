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

import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import dk.itu.moapd.scootersharing.coha.R
import dk.itu.moapd.scootersharing.coha.databinding.FragmentParkScooterBinding
import dk.itu.moapd.scootersharing.coha.models.Scooter
import dk.itu.moapd.scootersharing.coha.utils.BUCKET_STORAGE
import dk.itu.moapd.scootersharing.coha.utils.GeoLocationService
import dk.itu.moapd.scootersharing.coha.utils.date_to_String


class ParkScooterFragment : Fragment() {
    companion object{
        private val TAG = ParkScooterFragment::class.qualifiedName
        private lateinit var chosenScooter: Scooter
    }

    // View binding for the fragment
    private var _binding: FragmentParkScooterBinding? = null

    // Binding for the fragment view
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // get the chosen scooter from Main
        chosenScooter = MainFragment.chosenScooter

        // update the scooters location and coordinates
        updateScooter()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for the fragment
        _binding = FragmentParkScooterBinding.inflate(inflater,container,false)


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            binding.scooterName.text = chosenScooter.name
            binding.scooterLocation.text = chosenScooter.location
            binding.scooterTimestamp.text = date_to_String(chosenScooter.timestamp)

            // get the picture take in CameraXPictureFragment and show it
            binding.ScooterImage.setImageURI(CameraXPictureFragment.uri)


            binding.ParkScooter.setOnClickListener {
                parkScooter()
            }

            binding.RetakePicture.setOnClickListener {
                findNavController().navigate(R.id.show_cameraXPictureFragment)
            }

        }


    }

    private fun parkScooter() {
        // update the scooters new location and coordinates on the firebase db.
        updateDBScooter()

        // uploade the picture take as the scooters new image.
        updateBuketDB()

        MainFragment.RentingScooter = false
        findNavController().navigate(R.id.show_mainFragment)

    }

    private fun updateBuketDB() {

        val storage = Firebase.storage(BUCKET_STORAGE)

        // Create a reference to the file in your app
        val file = CameraXPictureFragment.uri

        val imageRef = storage.reference.child("Images/${chosenScooter.name}.jpg")

        // Upload the file to Firebase Storage with the specified name
        val uploadTask = imageRef.putFile(file)

        // Listen for state changes, errors, and completion of the upload
        uploadTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG,"Picture upload successful")
            } else {
                Log.d(TAG,"Picture upload failed")
            }
        }



    }

    private fun updateScooter() {

        chosenScooter.timestamp = System.currentTimeMillis()
        chosenScooter.latitude = GeoLocationService.coordinates.latitude
        chosenScooter.longitude = GeoLocationService.coordinates.longitude
        chosenScooter.location = getCompleteAddressString(
            chosenScooter.latitude!!,
            chosenScooter.longitude!!
        )




    }

    private fun updateDBScooter() {

        val scooterKey = MainFragment.scooterId.toString()
        // create a update query to the scooter db
        val scooterRef = MainFragment.database.child("scooter").child(scooterKey)
        val updateMap = mapOf(
            "location" to chosenScooter.location,
            "timestamp" to chosenScooter.timestamp,
            "latitude" to  chosenScooter.latitude,
            "longitude" to chosenScooter.longitude
        )
        // send the query
        scooterRef.updateChildren(updateMap)
    }

    private fun getCompleteAddressString(lat:Double,long:Double): String {
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


}