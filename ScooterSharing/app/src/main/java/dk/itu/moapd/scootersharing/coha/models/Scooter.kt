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
package dk.itu.moapd.scootersharing.coha.models

import android.icu.text.SimpleDateFormat
import com.google.firebase.database.IgnoreExtraProperties
import java.util.*




/**
 * A data class representing a Scooter object.
 *
 * @property name The name of the scooter.
 * @property location The current location of the scooter.
 * @property timestamp The timestamp when the scooter was placed at its current location.
 */
@IgnoreExtraProperties
data class Scooter(var name: String? = null, var location : String? = null,var timestamp: Long? = null,var latitude: Double? = null,var longitude: Double? = null){

    /**
     * Returns a string representation of the Scooter object.
     *
     * @return A string containing the name, location, and timestamp of the scooter.
     */
    override fun toString(): String {
        return "[Scooter] $name is placed at $location at time ${time_to_String()}"
    }

    /**
     * Returns a string representation of the timestamp in time format.
     *
     * @return A string containing the time in HH:mm format.
     */
    fun time_to_String(): String {
        val simpleDateFormat = SimpleDateFormat.getTimeInstance()
        return simpleDateFormat.format(timestamp)
    }

    /**
     * Returns a string representation of the timestamp in date format.
     *
     * @return A string containing the date and time in HH:mm yyyy-MM-dd format.
     */
    fun date_to_String(): String {
        val simpleDateFormat = SimpleDateFormat("HH:mm yyyy-MM-dd", Locale.getDefault())
        return simpleDateFormat.format(timestamp)
    }

}

