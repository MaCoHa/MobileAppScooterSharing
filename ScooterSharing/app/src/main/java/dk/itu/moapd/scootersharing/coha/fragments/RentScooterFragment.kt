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
import android.os.Handler
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
import dk.itu.moapd.scootersharing.coha.databinding.FragmentRentScooterBinding
import dk.itu.moapd.scootersharing.coha.models.Scooter
import dk.itu.moapd.scootersharing.coha.utils.BUCKET_STORAGE
import dk.itu.moapd.scootersharing.coha.utils.date_to_String
import java.util.*


class RentScooterFragment : Fragment() {

    companion object {
        private val TAG = RentScooterFragment::class.qualifiedName
        private lateinit var chosenScooter: Scooter
        private var price = 0
        var startTime = 0L
        var elapsedTime = 0L

        // Is the stopwatch running?
        var running = false
        var wasRunning = false

    }





    // View binding for the fragment
    private var _binding: FragmentRentScooterBinding? = null

    // Binding for the fragment view
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {

            // Get the previous state of the stopwatch
            // if the activity has been
            // destroyed and recreated.
            startTime = savedInstanceState
                .getLong("startTime")
            running = savedInstanceState
                .getBoolean("running")
            wasRunning = savedInstanceState
                .getBoolean("wasRunning")
        }


        chosenScooter = MainFragment.chosenScooter
    }
    override fun onSaveInstanceState(
        savedInstanceState: Bundle
    ) {
        savedInstanceState
            .putLong("startTime", startTime)
        savedInstanceState
            .putBoolean("running", running)
        savedInstanceState
            .putBoolean("wasRunning", wasRunning)
    }

    override fun onPause() {
        super.onPause()
        wasRunning = running
        running = false
    }

    // If the activity is resumed,
    // start the stopwatch
    // again if it was running previously.
    override fun onResume() {
        super.onResume()
        if (wasRunning) {
            running = true
        }else{
            startTime = System.currentTimeMillis()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for the fragment
        _binding = FragmentRentScooterBinding.inflate(inflater, container, false)


        // start the timer
        runTimer()
        // if the timer is not running start it.
        if (!running){
            running = true
        }


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

            binding.EndRentScooter.setOnClickListener {
                endRentingCheck()
            }

        }
    }


    private fun endRentingCheck() {

        // if the user is inside a geofence end the ride
        if (MainActivity.inGeofence) {
            endRenting()
        }else{
            // if not inside a geofence then call the debug override.
            debugOverride()
        }
    }

    private fun endRenting() {

            /*
                Make an AlertDialog to confirm the users choice.
             */
            val title = "End Renting Scooter ${chosenScooter.name}"
            val text = "Are you sure you want to stop rent ${chosenScooter.name} \n" +
                    "At ${chosenScooter.location}? \n"

            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(title)
            builder.setMessage(text)
            builder.setPositiveButton("Yes") { _, _ ->
                // stop the timer
                stopTimer()

                callEndRenting()


            }
            builder.setNegativeButton("No") { _, _ ->
                return@setNegativeButton
            }
            val dialog = builder.create()
            dialog.show()







    }

    private fun callEndRenting() {
        findNavController().navigate(R.id.show_cameraXPictureFragment)
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
            endRenting()
        }
        builder.setNegativeButton("No") { _, _ ->
            return@setNegativeButton
        }
        val dialog = builder.create()
        dialog.show()


    }

    fun stopTimer() {
        running = false
    }


    private fun runTimer() {

        val timeView = binding.Counter
        val priceView = binding.Price

        val handler = Handler()


        // handler is a seperate thread that keeps running
        handler.post(object : Runnable {
            override fun run() {
                //convert the elapsedTime to hours, minutes and seconds
                val hours = elapsedTime / 3600000
                val minutes = (elapsedTime / 60000) % 60
                val seconds = (elapsedTime / 1000) % 60

                val time = String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)

                // calculate and increase the price
                price = (seconds * 0.5 + 10).toInt()
                priceView.text = "Price::${price} kr"

                timeView.text = time

                if (running) {
                    elapsedTime = System.currentTimeMillis() - startTime
                }

                handler.postDelayed(this, 1000)
            }
        })
    }


}

