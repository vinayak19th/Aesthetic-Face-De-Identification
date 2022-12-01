package com.group.nine.camerafilter

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.google.android.material.slider.Slider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.group.nine.camerafilter.databinding.FragmentSecondBinding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private lateinit var srcView : ImageView
    private lateinit var outView : ImageView
    private lateinit var imageURI : Uri
    private lateinit var processImageButton : Button
    private lateinit var bitImage : Bitmap
    private lateinit var slider : Slider
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    val filter = ImageFilter(an_clusters = 3)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //  FACE DETECTION CODE
        val faceDetectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        val faceDetector = FaceDetection.getClient(faceDetectorOptions)
        //  FACE DETECTION CODE
        srcView = view.findViewById(R.id.srcImage)
        outView = view.findViewById(R.id.outImage)
        slider = view.findViewById(R.id.clusterSlider)
        slider.addOnChangeListener { slider, value, fromUser ->
            Log.d("Slider:","Setting Cluser to :"+slider.value.toString())
            filter.setClusterSize(value.toInt())
        }
        processImageButton = view.findViewById(R.id.process_image)
        setFragmentResultListener("ImageUri"){ requestKey, bundle ->
            imageURI = Uri.parse(bundle.getString("result"))
            val imSource = ImageDecoder.createSource(requireActivity().contentResolver,imageURI)
            bitImage = ImageDecoder.decodeBitmap(imSource){ decoder, _, _ ->
                decoder.isMutableRequired = true
            }
            srcView.setImageBitmap(bitImage)
        }
        processImageButton.setOnClickListener {
            processImage(faceDetector,view.context)
        }
        binding.previousActivity.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
    }

    private fun processImage(faceDetector:FaceDetector, context:Context) {
        val inputImage : InputImage = InputImage.fromFilePath(context,imageURI)
        val faces = faceDetector.process(inputImage)
            .addOnSuccessListener {
                renderBB(it)
            }
    }

    private fun overlayer(bmp1: Bitmap, bmp2: Bitmap, rectangle: Rect): Bitmap {
        val bmOverlay = Bitmap.createBitmap(bmp1.width, bmp1.height, bmp1.config)
        val canvas = Canvas(bmOverlay)
        canvas.drawBitmap(bmp1, Matrix(), null)
        canvas.drawBitmap(bmp2, null, rectangle, null);
        return bmOverlay
    }

    private fun renderBB(faces: List<Face> ){
        faces.forEach { face ->
            Log.d("FACES:","Values:"+face.boundingBox.toString())
            var outImage = Bitmap.createBitmap(bitImage,face.boundingBox.left,face.boundingBox.top,face.boundingBox.width(),face.boundingBox.height())
            val rectangle = Rect(face.boundingBox.left,face.boundingBox.top,face.boundingBox.left+face.boundingBox.width(),face.boundingBox.top +face.boundingBox.height())
            outImage = filter.processImage(outImage)
            bitImage = overlayer(bitImage, outImage, rectangle)
        }
        activity?.runOnUiThread {
            outView.setImageBitmap(bitImage)
        }
        Log.d("render","Finished Face render")
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}