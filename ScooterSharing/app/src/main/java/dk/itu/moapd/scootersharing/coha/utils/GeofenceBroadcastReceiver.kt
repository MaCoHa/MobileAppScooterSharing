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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    companion object{
        private val TAG = GeofenceBroadcastReceiver::class.qualifiedName
        const val ACTION_GEOFENCE_UPDATE = "dk.itu.moapd.scootersharing.coha.ACTION_GEOFENCE_UPDATE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        /*
            An geofence was triggered by the user
            Get the event data
         */
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        // check if any errors were registered
        if (geofencingEvent?.hasError() == true) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, errorMessage)
            return
        }

        // Get the transition type.
        val geofenceTransition = geofencingEvent?.geofenceTransition
            // as default the user is not inside a geofence
            var inGeofence = false

            // See what type of transition it was and act accordingly
            when (geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    Log.d(TAG, "************ User are in a geofence **********************")
                    //Toast.makeText(context,"We are in a geofence", Toast.LENGTH_LONG).show()
                    inGeofence = true
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    Log.d(TAG, "************ User left a geofence **********************")
                    // Toast.makeText(context,"We left a geofence", Toast.LENGTH_LONG).show()
                    inGeofence = false
                }
                Geofence.GEOFENCE_TRANSITION_DWELL -> {
                    Log.d(TAG, "************  geofence DWELL triggered **********************")
                    //Toast.makeText(context,"geofence triggered", Toast.LENGTH_LONG).show()
                }
                else -> {
                    // Log the error.
                    Log.d(TAG, "error happened with geofence")
                }
            }

            // pack the boolean into Intent so it can be broadcast
            val intent = Intent(ACTION_GEOFENCE_UPDATE).apply {
                putExtra("CurrentlyInGeofence", inGeofence)
            }

            // Broadcaster that shares the value such that other fragments can read the value
            val localBroadcastManager = LocalBroadcastManager.getInstance(context)
            localBroadcastManager.sendBroadcast(intent)


    }
}