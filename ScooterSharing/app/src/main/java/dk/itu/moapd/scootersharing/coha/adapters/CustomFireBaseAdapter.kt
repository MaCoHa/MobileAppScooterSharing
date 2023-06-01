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
package dk.itu.moapd.scootersharing.coha.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import dk.itu.moapd.scootersharing.coha.R
import dk.itu.moapd.scootersharing.coha.databinding.ListRidesBinding
import dk.itu.moapd.scootersharing.coha.models.Scooter
import dk.itu.moapd.scootersharing.coha.utils.BUCKET_STORAGE
import dk.itu.moapd.scootersharing.coha.utils.date_to_String

class CustomFireBaseAdapter(
    options: FirebaseRecyclerOptions<Scooter>
):
    FirebaseRecyclerAdapter<Scooter, CustomFireBaseAdapter.ViewHolder>(options) {

    /**
     * The `CustomFireBaseAdapter` companion object.
     */
    companion object {
        private val TAG = CustomFireBaseAdapter::class.qualifiedName
    }

    /**
     * The `ViewHolder` class for the `CustomFireBaseAdapter`.
     *
     * @param binding The `ListRidesBinding` instance.
     */
    class ViewHolder(private val binding: ListRidesBinding) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Bind the `ViewHolder` with the provided `Scooter` data.
         *
         * @param scooter The `Scooter` data.
         */
        fun bind(scooter: Scooter) {
            binding.scooterName.text = scooter.name
            binding.scooterLocation.text = scooter.location
            binding.scooterTimestamp.text = date_to_String(scooter.timestamp)

            // Create a Firebase storage instance.
            val storage = Firebase.storage(BUCKET_STORAGE)
            val imageRef = storage.reference.child("Images/${scooter.name}.jpg")


            // Download and set an image into the ImageView.
            imageRef.downloadUrl.addOnSuccessListener {
                Picasso.get().load(it).into(binding.imageView)
            }.addOnFailureListener{
                Log.e(TAG,"Retriving from bucket failed ",it)
                binding.imageView.setImageResource(R.drawable.scootertemp)
            }



        }

    }



    /**
     * Create a new `ViewHolder` instance.
     *
     * @param parent The `ViewGroup` parent.
     * @param viewType The view type.
     * @return The new `ViewHolder` instance.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Log system.
        Log.d(TAG, "Creating a new ViewHolder.")

        // Create a new view, which defines the UI of the list item
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListRidesBinding.inflate(inflater, parent, false)
        Log.d(TAG, "Creating a new Suceded ViewHolder.")
        return ViewHolder(binding)
    }

    /**
     * Bind the `ViewHolder` with the provided `Scooter` data.
     *
     * @param holder The `ViewHolder` instance.
     * @param position The item position.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int, scooter: Scooter) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element.
        Log.i(TAG, "Populate an item at position: $position")

        // Bind the view holder with the selected `Scooter` data.
        holder.apply {
            bind(scooter)
        }
    }

}
