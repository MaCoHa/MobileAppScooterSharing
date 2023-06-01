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

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import dk.itu.moapd.scootersharing.coha.R
import dk.itu.moapd.scootersharing.coha.adapters.CustomFireBaseAdapter
import dk.itu.moapd.scootersharing.coha.databinding.FragmentMainBinding
import dk.itu.moapd.scootersharing.coha.models.Scooter
import dk.itu.moapd.scootersharing.coha.utils.DATABASE_URL
import dk.itu.moapd.scootersharing.coha.utils.SwipeToOpenCallback


/**
 * Fragment to display a list of rides and options to start or update a ride.
 */
class MainFragment : Fragment() {

    companion object {
        // Tag for logging
        private val TAG = MainFragment::class.qualifiedName
        private lateinit var adapter: CustomFireBaseAdapter
        lateinit var auth: FirebaseAuth
        lateinit var database: DatabaseReference
        // chosen scooter is the scooter chosen by the user on swipe or on QR scan
        var chosenScooter: Scooter = Scooter()
        // If the user is renting a scooter this is true
        var RentingScooter: Boolean = false
        // This is the chosenScooters unique database ID
        var scooterId : String? = null



    }
    // View binding for the fragment
    private var _binding: FragmentMainBinding? = null

    // Binding for the fragment view
    private val binding get() = _binding!!



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth.
        auth = FirebaseAuth.getInstance()
        database = Firebase.database(DATABASE_URL).reference

        // Check if the user is not logged and redirect her/him to the LoginActivity.
        if (auth.currentUser == null)
            startLoginFragment()


    }




    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate the layout for the fragment
        _binding = FragmentMainBinding.inflate(inflater,container,false)

        auth.currentUser?.let {
            Log.d(TAG,"Creating the DB Query")
            val query = database.child("scooter")
                .orderByChild("timestamp")

            // create a qurey for all scooters and insert them into a firebase recycler view.
            val options = FirebaseRecyclerOptions.Builder<Scooter>()
                .setQuery(query,Scooter::class.java)
                .setLifecycleOwner(this)
                .build()

            adapter = CustomFireBaseAdapter(options)
        }
        Log.d(TAG,"Setting up the CustomFireBaseAdapter ")




        // Set the layout manager for the RecyclerView
        binding.recyclerRidesView.layoutManager = LinearLayoutManager(context)
        binding.recyclerRidesView.addItemDecoration(
            DividerItemDecoration(context,DividerItemDecoration.VERTICAL)
        )

        if (adapter != null){
            binding.recyclerRidesView.adapter = adapter
        }



        // is call in left or right swipe
        val swipeToOpenCallback = object : SwipeToOpenCallback(){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                Log.d(TAG, "User Swiped to open")

                // If a scooter is all ready being rented then navigate the user directly to the renting page
                if (RentingScooter) {
                    findNavController().navigate(R.id.show_rentScooterFragment)
                } else {

                    // get the values of scooter on the swiped scooter
                    val scooterRef = adapter.getRef(viewHolder.absoluteAdapterPosition)
                    // Get the scooter ID
                    scooterId = scooterRef.key

                    scooterRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val scooter = snapshot.getValue(Scooter::class.java)

                            // Use the scooter object
                            Log.d(TAG, "Swiped scooter name: ${scooter?.name}")
                            if (scooter != null) {

                                chosenScooter = scooter

                            }
                            findNavController().navigate(R.id.show_scooterProfileFragment)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(TAG, "Error getting scooter object: ${error.message}")
                        }
                    })
                }
            }

              }

        // add the swipe function to the RecyclerView
        val itemTouchHelper = ItemTouchHelper(swipeToOpenCallback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerRidesView)



        // Return the root view of the fragment
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Clean up the view binding
        _binding = null


    }


    override fun onStart() {
        super.onStart()

        // Check if the user is not logged and redirect her/him to the LoginActivity.
        if (auth.currentUser == null)
            startLoginFragment()


    }

    private fun startLoginFragment() {
        Log.d(TAG, "Navigate user to Login")
        findNavController().navigate(R.id.show_loginFragment)
    }



}
