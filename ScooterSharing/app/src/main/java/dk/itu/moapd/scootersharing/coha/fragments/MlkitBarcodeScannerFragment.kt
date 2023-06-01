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
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.database.*
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dk.itu.moapd.scootersharing.coha.R
import dk.itu.moapd.scootersharing.coha.databinding.FragmentMlkitBarcodeScannerBinding
import dk.itu.moapd.scootersharing.coha.models.Scooter
import java.util.concurrent.Executors

private const val CAMERA_PERMISSION_REQUEST_CODE = 1
class MlkitBarcodeScannerFragment : Fragment() {
 companion object{
     private val TAG = MlkitBarcodeScannerFragment::class.qualifiedName


 }
    // if a qurey for data is in progress block others from being created
    private var queryInProgress: Boolean = false

    private var barcodeValue:String = ""
    private var _binding: FragmentMlkitBarcodeScannerBinding? = null

    // Use backing property to avoid null safety checks every time binding is used.
    private val binding get() = _binding!!



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMlkitBarcodeScannerBinding.inflate(inflater,container,false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // check permission and request them if needed.
        if (hasCameraPermission()){
            bindCameraUseCases()
        }
        else {
            requestPermission()
            bindCameraUseCases()
        }

    }

    // checking to see whether user has already granted permission
    private fun hasCameraPermission() =
        ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission(){
        // opening up dialog to ask for camera permission
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }


    private fun bindCameraUseCases() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // setting up the preview use case
            val previewUseCase = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraView.surfaceProvider)
                }

            // configure our MLKit BarcodeScanning client

            // specify what the scanner can scan
            val options = BarcodeScannerOptions.Builder().setBarcodeFormats(
                Barcode.FORMAT_QR_CODE
            ).build()

            // getClient() creates a new instance of the MLKit barcode scanner with the specified options
            val scanner = BarcodeScanning.getClient(options)

            // setting up the analysis use case
            val analysisUseCase = ImageAnalysis.Builder()
                .build()

            // define the actual functionality of our analysis use case
            analysisUseCase.setAnalyzer(
                Executors.newSingleThreadExecutor()
            ) { imageProxy ->
                processImageProxy(scanner, imageProxy)
            }

            // configure to use the back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    previewUseCase,
                    analysisUseCase)
            } catch (illegalStateException: IllegalStateException) {
                // If the use case has already been bound to another lifecycle or method is not called on main thread.
                Log.e(TAG, illegalStateException.message.orEmpty())
            } catch (illegalArgumentException: IllegalArgumentException) {
                // If the provided camera selector is unable to resolve a camera to be used for the given use cases.
                Log.e(TAG, illegalArgumentException.message.orEmpty())
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ) {

        imageProxy.image?.let { image ->
            val inputImage =
                InputImage.fromMediaImage(
                    image,
                    imageProxy.imageInfo.rotationDegrees
                )

            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodeList ->
                    val barcode = barcodeList.getOrNull(0)

                    // `rawValue` is the decoded value of the barcode
                    barcode?.rawValue?.let { value ->
                        barcodeValue = value
                        binding.bottomText.text = "Barcode = $value"
                        /*
                        If a query is in progress then skip.
                        else change the in progress and start a query.
                        The barcode scanner will keep scanning a QR code even after it succeeded once
                        this means that with out this check, it can crate many query's before the first succeed.
                         */
                        if (!queryInProgress){
                            queryInProgress = true
                            retrieveScooter()
                        }

                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, it.message.orEmpty())
                }.addOnCompleteListener {
                    imageProxy.image?.close()
                    imageProxy.close()
                }
        }
    }

    private fun retrieveScooter() {



        // Create the search query.
        MainFragment.auth.currentUser?.let {
            MainFragment.database
                    .child("scooter/${barcodeValue.trim()}")
                    .get().addOnSuccessListener {
                        // extract the scooter values
                        val scooterData = it.value as HashMap<*, *>?
                        val latitude : Double = scooterData?.get("latitude") as Double
                        val longitude: Double  = scooterData.get("longitude") as Double
                        val name :String = scooterData.get("name") as String
                        val location : String = scooterData.get("location") as String
                        val timestamp : Long = scooterData.get("timestamp") as Long

                        Log.d("ScooterData", "Latitude: $latitude")
                        Log.d("ScooterData", "Longitude: $longitude")
                        Log.d("ScooterData", "Name: $name")
                        Log.d("ScooterData", "Location: $location")
                        Log.d("ScooterData", "Timestamp: $timestamp")

                        // assign found scooter as the scooterchosen
                        MainFragment.chosenScooter = Scooter(name,location, timestamp, latitude, longitude)
                        // query done now a new can start
                        queryInProgress = false
                        // navigate to the QR code scooter profile.
                        findNavController().navigate(R.id.show_scooterProfileFragment)
                    }.addOnCanceledListener {
                        Log.d(TAG,"Query failed" )
                        // query failed new one can be done
                        queryInProgress = false
                    }.addOnFailureListener {
                        Log.e(TAG,"Query failed",it )
                        // query failed new one can be done
                        queryInProgress = false
                    }



            }

    }
}
