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
package dk.itu.moapd.scootersharing.coha.utils

import android.graphics.Color
import com.google.android.gms.location.Geofence
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng

class GeoFenceFile {

    // This is the geofence radius in meters
    private var geofenceRadius = 30.0


    // Creates a list of CircleOptions so the geofences can be drawn on google maps
    fun getGoogleCirkelList():List<CircleOptions>{
        val list = ArrayList<CircleOptions>()

        for (i in locationList) {
            val cord = LatLng(i.first,i.second)
            val fillcolor = Color.argb(125,255, 72, 0)
            val bordercolor = Color.argb(125,0, 255, 21)
            list.add(CircleOptions().center(cord).radius(geofenceRadius).strokeColor(
                bordercolor).fillColor(fillcolor))
        }

        return list
    }

    /*
        Creates a list of geofences such that they can be created
     */
    fun getGeoFenceList():List<Geofence>{
        val list = ArrayList<Geofence>()

        for (i in locationList){
            list.add(Geofence.Builder()
                .setRequestId(i.third)
                .setCircularRegion(i.first,i.second, geofenceRadius.toFloat())
                /*
                    Sets the Expiration of a geofence 300000 millis = 5 min
                    Can be change to Never expire (-1) = 584942417 Years 4 Months 1 Weeks 0 Days 22 Hours 6 Minutes 40 Seconds
                 */
                //.setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setExpirationDuration(300000)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
            )
        }

        return list

    }

    /* returns a list of all geofence names,
        used to destroy them on app closure
     */
    fun getGeoFenceIdList():List<String>{
        val list = ArrayList<String>()

        for (i in locationList){
            list.add(i.third)
        }

        return list
    }

    // List<Triple<Latitude,Longitude,LocationName>>
    private var locationList : List<Triple<Double,Double,String>> = listOf(
        Triple(55.659359, 12.591005,"ITU"),
        Triple(55.673392, 12.563941,"Hovedbanegår"),
        Triple(55.680173, 12.585731,"Kongens_Nytorv"),
        Triple(55.655893, 12.589257,"DR_Byen"),
        Triple(55.671536, 12.522909,"Zoologisk_Have"),
        Triple(55.695958, 12.608795,"Hottub Copenhagen"),
        Triple(55.703271, 12.614472,"Trekroner Fort"),
        Triple(55.682961, 12.571274,"Nørreport"),
        // change the parameters beneath to suite your location VVV
        //       Latitude            Longitude     LocationName
        Triple(55.657532, 12.597611,"CustomFence")
    )


}