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

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import dk.itu.moapd.scootersharing.coha.R
import dk.itu.moapd.scootersharing.coha.activities.MainActivity
import dk.itu.moapd.scootersharing.coha.databinding.FragmentScooterProfileBinding
import dk.itu.moapd.scootersharing.coha.models.Scooter
import dk.itu.moapd.scootersharing.coha.utils.BUCKET_STORAGE
import dk.itu.moapd.scootersharing.coha.utils.date_to_String


class ScooterProfileFragment : Fragment() {
    companion object{
        private val TAG = ScooterProfileFragment::class.qualifiedName
        private lateinit var chosenScooter:Scooter
    }

    // View binding for the fragment
    private var _binding: FragmentScooterProfileBinding? = null

    // Binding for the fragment view
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chosenScooter = MainFragment.chosenScooter


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for the fragment
        _binding = FragmentScooterProfileBinding.inflate(inflater, container, false)


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {


            binding.scooterName.text = chosenScooter.name
            binding.scooterLocation.text = chosenScooter.location
            binding.scooterTimestamp.text = date_to_String(chosenScooter.timestamp)

            // get the image URL
            val storage = Firebase.storage(BUCKET_STORAGE)
            val imageRef = storage.reference.child("Images/${chosenScooter.name}.jpg")


            // Download and set an image into the ImageView. via the URL
            imageRef.downloadUrl.addOnSuccessListener {
                // Picasso downloads the image and the converts it to a format it can display
                Picasso.get().load(it).into(binding.ScooterImage)
            }.addOnFailureListener{
                Log.e(TAG,"Retrieving from bucket failed ",it)
                // on Failure insert a default image.
                binding.ScooterImage.setImageResource(R.drawable.scootertemp)
            }

            binding.RentScooter.setOnClickListener {
                startRentingCheck()
            }


            binding.BackMainPage.setOnClickListener {
                findNavController().navigate(R.id.show_mainFragment)
            }

        }
    }

    private fun startRentingCheck() {
        // if the user is inside a geofence rent the scooter
        if (MainActivity.inGeofence) {
            startRenting()
        }else{
            // if not inside a geofence then call the debug override.
            debugOverride()
        }

    }

    private fun startRenting(){
        // create a AlertDialog to confirm the users choice.
        val title = "Rent Scooter ${chosenScooter.name}"
        val text = "Are you sure you want to rent ${chosenScooter.name} \n" +
                "At ${chosenScooter.location}?"

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(title)
        builder.setMessage(text)
        builder.setPositiveButton("Yes") { _, _ ->
            // set the renting checker to true
            MainFragment.RentingScooter = true

            // reset from any previous renting
            RentScooterFragment.running = false
            RentScooterFragment.wasRunning = false
            findNavController().navigate(R.id.show_rentScooterFragment)
        }
        builder.setNegativeButton("No") { _, _ ->
            return@setNegativeButton
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun debugOverride(){
        /*
            Create a AlertDialog with a debug override option
         */
        val title = "**** DEBUG ****"
        val text = "You are not inside a Geofence \n" +
                "This means that normally you cannot rent a scooter\n" +
                "Do you wish to force override this measure?"

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(title)
        builder.setMessage(text)
        builder.setPositiveButton("Yes") { _, _ ->
            startRenting()

        }
        builder.setNegativeButton("No") { _, _ ->
            return@setNegativeButton
        }
        val dialog = builder.create()
        dialog.show()

    }






}

