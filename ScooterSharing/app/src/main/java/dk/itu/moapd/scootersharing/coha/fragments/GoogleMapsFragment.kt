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

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import dk.itu.moapd.scootersharing.coha.activities.MainActivity
import dk.itu.moapd.scootersharing.coha.databinding.FragmentGoogleMapsBinding
import dk.itu.moapd.scootersharing.coha.utils.GeoFenceFile
import dk.itu.moapd.scootersharing.coha.utils.GeoLocationService


class GoogleMapsFragment : Fragment(), OnMapReadyCallback {

    companion object {
        /**
         * A private constant `TAG` used for logging purposes.
         */
        private val TAG = GoogleMapsFragment::class.qualifiedName

        private lateinit var googleMap: GoogleMap

        private lateinit var mapView: MapView

        private lateinit var binding: FragmentGoogleMapsBinding

        lateinit var coordinates : Location

        /**
         * geofencefile contains the hard coded cordinates and radius of all geofences
         */
        lateinit var geofencefile: GeoFenceFile
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get the users current coordinates
        coordinates = GeoLocationService.coordinates

        geofencefile = MainActivity.geofencefile
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGoogleMapsBinding.inflate(inflater, container, false)

        mapView = binding.mapview
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        return binding.root
    }

    override fun onMapReady(map: GoogleMap) {

        // Check if the user allows the application to access the location-aware resources.
        if (checkPermission())
            return

        googleMap = map

        Log.d(TAG,"NO google map instance")

        // Show the current device's location as a blue dot.
        googleMap.isMyLocationEnabled = true

        // Set the default map type.
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL

        // Setup the UI settings state.
        googleMap.uiSettings.apply {
            isCompassEnabled = true
            isIndoorLevelPickerEnabled = true
            isMyLocationButtonEnabled = true
            isRotateGesturesEnabled = true
            isScrollGesturesEnabled = true
            isTiltGesturesEnabled = true
            isZoomControlsEnabled = true
            isZoomGesturesEnabled = true
        }

        // Move the Google Maps UI buttons under the OS top bar.
        googleMap.setPadding(0, 100, 0, 0)

        // Zoom in on the user based on their current coordinates
       val user = LatLng(coordinates.latitude, coordinates.longitude)  // NE bounds
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(user, 20F))

        // Draw all geofences on the map
        for (i in geofencefile.getGoogleCirkelList()){
            googleMap.addCircle(i)
        }

        // Place all scooters as markers on the map
        placeScooter()
    }

    private fun placeScooter() {

        val markerArray = ArrayList<MarkerOptions>()

         MainFragment.auth.currentUser.let {
             // Make a query to get all scooters, extract their coordinates and name
             MainFragment.database.
             child("scooter")
                 .get()
                 .addOnSuccessListener{
                     it.children.forEach { childSnapshot ->

                         val name = childSnapshot.child("name").getValue(String::class.java)
                         val latitude: Double = childSnapshot.child("latitude").getValue(Double::class.java)!!
                         val longitude: Double  = childSnapshot.child("longitude").getValue(Double::class.java)!!
                         // use the values to make the scooter into a marker
                         markerArray.add(MarkerOptions().position(LatLng(latitude,longitude)).title(name))

                     }
                     // Add all markers to the map
                     for (i in markerArray){
                         googleMap.addMarker(i)
                     }
                 }.addOnFailureListener{
                    Log.d(TAG,"MarkerQuery failed")
                 }





         }




    }

    private fun checkPermission() =
        ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}