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
package dk.itu.moapd.scootersharing.coha.activities


import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.location.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import dk.itu.moapd.scootersharing.coha.R
import dk.itu.moapd.scootersharing.coha.databinding.ActivityMainBinding
import dk.itu.moapd.scootersharing.coha.fragments.*
import dk.itu.moapd.scootersharing.coha.utils.GeoFenceFile
import dk.itu.moapd.scootersharing.coha.utils.GeoLocationService
import dk.itu.moapd.scootersharing.coha.utils.GeofenceBroadcastReceiver
import java.util.*


/**
 * MainActivity is an `AppCompatActivity` subclass that provides the functionality for the main screen
 * of the application. It has GUI elements such as `EditText` for entering scooter name and location and
 * a `Button` for starting the ride.
 * The `showMessage` function prints a message in Logcat and the `onCreate` method is responsible for
 * initializing the GUI elements and setting up the click listener for the start ride button.
 */
class MainActivity : AppCompatActivity() {


    companion object {
        /**
         * A private constant `TAG` used for logging purposes.
         */
        private val TAG = MainActivity::class.qualifiedName

        /**
         * A mutable lateinit property `coordinates` of type Location to hold the current location.
         */
        lateinit var coordinates: Location

        /**
         * A mutable lateinit property `geoServices` of type GeoLocationService to handle location services.
         */
        lateinit var geoServices: GeoLocationService

        /**
         * A mutable lateinit property `geofencefile` of type GeoFenceFile to handle geo-fencing services.
         */
        lateinit var geofencefile: GeoFenceFile


        /**
         * Flag that indicates whether the device is currently inside the geofence or not
         */
        var inGeofence : Boolean = false



    }

    /**
     * Initialization of the navigation controller for the application
     */
    private lateinit var navController: NavController

    /**
     * Initialization of the geofencing client for the application
     */
    private lateinit var geofencingClient: GeofencingClient

    /**
     * Initialization of the bottom navigation view for the application
     */
    private lateinit var bottomNav : BottomNavigationView

    /**
     * Initialization of the binding for the main activity layout
     */
    private lateinit var binding: ActivityMainBinding


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?){

        // Set up the system UI to use the entire screen
        WindowCompat.setDecorFitsSystemWindows(window,false)

        super.onCreate(savedInstanceState)

        // Set up view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Set up navigation controller
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        // Check if the user is logged in, redirect to login fragment if not
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null)
            startLoginFragment(navController)

        // Hide the system UI to use the entire screen
        hideSystemUI()

        // Request location permissions
        requestPermissions()

        // Start location services
        startLocationAware()

        // Set up geofencing
        geofencefile = GeoFenceFile()
        setupGeofencing()

        // Set up bottom navigation
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.map_action -> {
                    navController.navigate(R.id.show_googleMapsFragment)
                    true
                }
                R.id.scooter_list -> {
                    navController.navigate(R.id.show_mainFragment)
                    true
                }
                R.id.qr_scanner -> {
                    navController.navigate(R.id.show_mlkitBarcodeScannerFragment)
                    true
                }
                else -> {
                    Log.d(TAG,"Ended in bottombar nav else")
                    false
                }
            }
        }
        bottomNav.selectedItemId = R.id.scooter_list
    }



    /**
     * Called when the activity is resumed. Registers the broadcastReceiver to receive updates from the GeofenceBroadcastReceiver.
     */
    override fun onResume() {
        super.onResume()
        // Create an intent filter to match the action used in the GeofenceBroadcastReceiver.
        val filter = IntentFilter(GeofenceBroadcastReceiver.ACTION_GEOFENCE_UPDATE)
        // Register the broadcast receiver to receive updates from the GeofenceBroadcastReceiver.
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter)
    }

    /**
     * Called when the activity is paused. Unregisters the broadcastReceiver to stop receiving updates from the GeofenceBroadcastReceiver.
     */
    override fun onPause() {
        super.onPause()
        // Unregister the broadcast receiver to stop receiving updates from the GeofenceBroadcastReceiver.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    /**
     * Unregisters the broadcast receiver for geofence updates and removes all geofences when the activity is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        removeAllGeoFence()
        stopService(Intent(this,GeoLocationService::class.java))
    }

    /**
     * Login
     */


    /**
     * Navigates the user to the Login fragment by setting the NavController to navigate to the appropriate fragment.
     * @param navController The NavController used for navigating between fragments.
     */
    private fun startLoginFragment(navController: NavController) {
        // Log the navigation event.
        Log.d(TAG, "Navigate user to Login")
        // Set the NavController to navigate to the Login fragment.
        navController.navigate(R.id.show_updateRideFragment)
    }


    /**
     * Logs out the user and loads the login fragment.
     */
    private fun logout(){
        Toast.makeText(this,"Logout Selected",Toast.LENGTH_SHORT).show()
        MainFragment.auth.signOut()
        navController.navigate(R.id.show_loginFragment)

    }

    /**
     * BroadcastReceiver
     */

    /**
     * A BroadcastReceiver that receives the updates from GeofenceBroadcastReceiver.
     * Overrides onReceive to extract the data.
     */
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // If the received intent's action matches the GeofenceBroadcastReceiver's action,
            // extract the data from the intent and do something with it.
            if (intent?.action == GeofenceBroadcastReceiver.ACTION_GEOFENCE_UPDATE) {
                // Extract the boolean value for whether the device is currently inside a geofence.
                inGeofence = intent.getBooleanExtra("CurrentlyInGeofence", false)
                // Display a Toast message to the user showing whether they are inside the geofence or not.
                Toast.makeText(context,"In geofence = $inGeofence", Toast.LENGTH_LONG).show()


            }
        }
    }


    /**
     * UI setup and other functions
     */

    /**
     * Sets up geofencing by initializing the GeofencingClient and adding geofences if permissions are granted.
     * If permissions are not granted, the function returns early. Logs success and failure messages as appropriate.
     */
    private fun setupGeofencing() {
        geofencingClient = LocationServices.getGeofencingClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG,"Missing permission for geofencing ")
            Toast.makeText(this,"Missing location permission making geofencing unavailable.",Toast.LENGTH_LONG).show()
            return
        }
        geofencingClient.addGeofences(getGeofencingRequest(), geofencePendingIntent).run {
            addOnSuccessListener {
                Log.d(TAG,"Geofencing Worked")
            }
            addOnFailureListener {
                Log.d(TAG,"Geofencing failed")
            }
        }

        if (!arePermissionsGranted()){
            Log.d(TAG,"Permissions Approved and validated")
        }else{
            Log.d(TAG,"Permissions NOT Approved ")
            Toast.makeText(this,"Permissions Location or background denied",Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Hides the system UI.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window,
            window.decorView.findViewById(android.R.id.content)).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())

            // When the screen is swiped up at the bottom
            // of the application, the navigationBar shall
            // appear for some time
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }


    /**
     * Starts the location aware feature.
     */

    private fun startLocationAware() {

        if(arePermissionsGranted())
            requestPermissions()

        // Show a dialog to ask the user to allow the application to access the device's location.


        coordinates = Location("dummyprovider")
        coordinates.latitude = 25.025885
        coordinates.longitude = -78.035889

        if (arePermissionsGranted())
            return

        startService(Intent(this,GeoLocationService::class.java))

        geoServices = GeoLocationService()

    }


    /**
     * Creates and returns a GeofencingRequest object that contains geofences obtained from a GeoFenceFile object,
     * and an initial trigger that triggers the geofence on entering or exiting.
     */
    private fun getGeofencingRequest(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT)
            addGeofences(geofencefile.getGeoFenceList())
        }.build()
    }

    /**
     * Removes all geofences currently set up using the GeofencingClient.
     * Logs success and failure messages as appropriate.
     */
    private fun removeAllGeoFence() {
        geofencingClient.removeGeofences(geofencefile.getGeoFenceIdList())
            .addOnSuccessListener {
                Log.d(TAG,"All geofences removed")
                // Geofence removal was successful
            }
            .addOnFailureListener { e ->
                Log.e(TAG,"Removing Geofence failed",e)
                // Geofence removal failed
            }
    }

    /**
     * Gets a PendingIntent object for the GeofenceBroadcastReceiver.
     */
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE)
    }


    /**
     * Initializes the options menu.
     * @param menu The menu object to inflate.
     * @return Returns a boolean indicating if the menu was successfully created.
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_bar_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Requests permissions from the user.
     */
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )
        ActivityCompat.requestPermissions(this, permissions, 1)
    }
    /**
     * Checks if all necessary permissions are granted.
     * @return Returns a boolean indicating whether all necessary permissions are granted.
     */
    private fun arePermissionsGranted()=

        ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
        &&
        ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
        &&
        ActivityCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED







    /**
     * UI event handling
     */


    /**
     * Handles menu item selection.
     * @param item The selected menu item.
     * @return Returns a boolean indicating if the item selection was handled successfully.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.user -> {

                Toast.makeText(this,"User Selected Not Implemented",Toast.LENGTH_SHORT).show()
            }
            R.id.settings -> Toast.makeText(this,"Settings Not Implemented",Toast.LENGTH_SHORT).show()
            R.id.logout -> logout()
            R.id.coordinates -> {
                val text = "Current location : Lat = ${GeoLocationService.coordinates.latitude} : Long = ${GeoLocationService.coordinates.longitude} "
                val text2 = "User are in side a geofence = $inGeofence"
                Toast.makeText(this,text,Toast.LENGTH_SHORT).show()
                Toast.makeText(this,text2,Toast.LENGTH_SHORT).show()
            }
            R.id.startRide -> {
                navController.navigate(R.id.show_startrideFragment)
            }
            R.id.updateRide ->{
                navController.navigate(R.id.show_updateRideFragment)
            }


        }
        return super.onOptionsItemSelected(item)
    }



}
