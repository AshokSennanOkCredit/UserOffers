package com.example.useroffers.fragment


import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.camera.AutoFitPreviewBuilder
import com.example.useroffers.R
import com.example.useroffers.TestActivity
import com.example.useroffers.interaction.CameraInteractionFragment
import com.example.useroffers.utils.Picture
import kotlinx.android.synthetic.main.fragment_camera.*
import java.io.File
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/**
 * A simple [Fragment] subclass.
 */
class CameraFragment : Fragment(),CameraInteractionFragment.Interactor {

    companion object{
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        private var cameraX : CameraFragment?  = null
        fun getInstance(): CameraFragment {

            return      CameraFragment()

        }
    }

    private var listener: TestActivity? = null


    private lateinit var textureView: TextureView
    private lateinit var broadcastManager: LocalBroadcastManager
    private lateinit var mainExecutor: Executor

    private var displayId = -1
    private var lenseFacing = CameraX.LensFacing.BACK
    private var preview: Preview?=null
    private var imageCapture: ImageCapture?=null
    private var imageAnalyser: ImageAnalysis?=null

    private lateinit var displayManager: DisplayManager
    private val displayListener = object: DisplayManager.DisplayListener{
        override fun onDisplayAdded(displayId: Int) = Unit

        override fun onDisplayRemoved(displayId: Int) = Unit

        override fun onDisplayChanged(displayId: Int){
            view?.let {view ->
                if(displayId == this@CameraFragment.displayId){
                    Log.d("Roitation Changed","${view.display.rotation}")
                    preview?.setTargetRotation(view.display.rotation)
                    imageCapture?.setTargetRotation(view.display.rotation)
                    imageAnalyser?.setTargetRotation(view?.display.rotation)
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is TestActivity){
            listener = context
        }else
        {
            throw RuntimeException(context.toString()+" must implement onFragmentInteractionListener ")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        mainExecutor = ContextCompat.getMainExecutor(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textureView = view.findViewById(R.id.textuireView)
        broadcastManager = LocalBroadcastManager.getInstance(view.context)

        //iniitailize display manager
        displayManager = textureView.context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        //add display listener in display manager
        displayManager.registerDisplayListener(displayListener,null)

        textureView.post {
            displayId = textureView.display.displayId
            bindCameraUseCases();
        }
        camera_interaction_layout.addInteractor(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //remove the listener from this service
        displayManager.unregisterDisplayListener(displayListener)
    }


    private fun bindCameraUseCases() {
        val matrics = DisplayMetrics().also {
            textureView.display.getMetrics(it)
        }
        Log.d("==>","Screen Matrix: ${matrics.widthPixels} X ${matrics.heightPixels}")
        val screenAspectRatio = aspectRatio(matrics.widthPixels,matrics.heightPixels)
        val viewFinderConfig = PreviewConfig.Builder().apply {
            setLensFacing(lenseFacing)
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(textureView.display.rotation)
        }.build()

        preview = AutoFitPreviewBuilder.build(viewFinderConfig,textureView)

        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setLensFacing(lenseFacing)
            setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(textureView.display.rotation)
        }.build()
        imageCapture = ImageCapture(imageCaptureConfig)


        val analyserConfig = ImageAnalysisConfig.Builder().apply {
            setLensFacing(lenseFacing)
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
            setTargetRotation(textureView.display.rotation)
        }.build()

        imageAnalyser = ImageAnalysis(analyserConfig).apply {
            setAnalyzer(mainExecutor,LuminosyteAnalyser())
        }

        CameraX.bindToLifecycle(context as LifecycleOwner,preview,imageCapture,imageAnalyser)
    }

    private fun aspectRatio(widthPixels: Int, heightPixels: Int): AspectRatio {
        val previewRatio = Math.max(widthPixels, heightPixels).toDouble()/ Math.min(
            widthPixels,
            heightPixels
        )
        if (kotlin.math.abs(previewRatio - RATIO_4_3_VALUE) <= kotlin.math.abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }





    //************** interaction callbacks **********************//
    override fun onClick() {
        imageCapture?.let { imageCapture ->
            val photoFile = File(context?.getExternalFilesDir(null),"${System.currentTimeMillis()}.jpeg")

            // Setup image capture metadata
            val metadata = ImageCapture.Metadata().apply {
                // Mirror image when using the front camera
                isReversedHorizontal = lenseFacing == CameraX.LensFacing.BACK
            }

            imageCapture.takePicture(photoFile,metadata,mainExecutor,object: ImageCapture.OnImageSavedListener{
                override fun onImageSaved(file: File) {
                    Toast.makeText(requireActivity(),"${file.path} picture saved successfully", Toast.LENGTH_SHORT).show()
                    listener?.cameraCapturedImage(Picture(photoFile.absolutePath, true))
                }

                override fun onError(
                    imageCaptureError: ImageCapture.ImageCaptureError,
                    message: String,
                    cause: Throwable?
                ) {
                    Toast.makeText(requireActivity(),"$message", Toast.LENGTH_SHORT).show()
                }

            })
        }

    }

    override fun onFlashClicked() {
        if (preview!!.isTorchOn) {
            camera_interaction_layout.off_Torch()
            preview!!.enableTorch(false)
        } else {
            camera_interaction_layout.on_Torch()
            preview!!.enableTorch(true)
        }
    }

    override fun onBackClicked() {
        preview?.let {
            if (preview!!.isTorchOn) {
                camera_interaction_layout.off_Torch()
                preview!!.enableTorch(false)
            }
        }
    }

}

private class LuminosyteAnalyser(): ImageAnalysis.Analyzer{

    private var lastAnalysedTime:Long = 0L

    override fun analyze(image: ImageProxy, rotationDegrees: Int) {
        val currentTimeStamp = System.currentTimeMillis()
        if(currentTimeStamp - lastAnalysedTime >= TimeUnit.SECONDS.toMillis(1)){
            lastAnalysedTime = currentTimeStamp

            // Since format in ImageAnalysis is YUV, image.planes[0] contains the luminance
            //  plane
            val buffer = image.planes[0].buffer

            // Extract image data from callback object
            val data = buffer.toByteArray()

            // Convert the data into an array of pixel values ranging 0-255
            val pixels = data.map { it.toInt() and 0xFF }

            // Compute average luminance for the image
            val luma = pixels.average()

        }

    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

}