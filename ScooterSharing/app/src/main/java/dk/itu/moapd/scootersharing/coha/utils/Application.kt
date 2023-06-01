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
import android.app.Application
import android.icu.text.SimpleDateFormat
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.*


/**
 * Firebase Realtime Database URL.
 */
const val DATABASE_URL =
 

/**
 * Firebase Storage URL.
 */
const val BUCKET_STORAGE =



fun date_to_String(timestamp: Long?): String {
    val simpleDateFormat = SimpleDateFormat("HH:mm yyyy-MM-dd", Locale.getDefault())
    return simpleDateFormat.format(timestamp)
}

open class Application : Application() {

    override fun onCreate() {
        super.onCreate()
        Firebase.database(DATABASE_URL).setPersistenceEnabled(true)
    }


}
