package com.group.nine.camerafilter

import android.R.attr.data
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
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
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private lateinit var srcView : ImageView
    private lateinit var outView : ImageView
    private lateinit var imageURI : Uri
    private lateinit var processImageButton : Button
    private lateinit var saveImageButton : Button
    private lateinit var bitImage : Bitmap
    private lateinit var slider : Slider
    private lateinit var epsilonSlider : Slider
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    val filter = ImageFilter(an_clusters = 3, apoly_epsilon = 10.0)

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
        epsilonSlider = view.findViewById(R.id.epsilonSlider)
        slider.addOnChangeListener { slider, value, fromUser ->
            Log.d("Cluster Slider:","Setting Cluster to :"+slider.value.toString())
            filter.setClusterSize(value.toInt())
        }
        epsilonSlider.addOnChangeListener { epsilonSlider, value, fromUser ->
            Log.d("Epsilon Slider:","Setting Epsilon to :"+ epsilonSlider.value.toString())
            filter.setPolyEpsilon(value.toDouble())
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

        saveImageButton = view.findViewById(R.id.save_image)
        saveImageButton.setOnClickListener {
            saveToGallery(view.context, bitImage, "PML598")
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

    private fun findThresholdBB(faces: List<Face> ) : Int {
        var threshold = 0
        var t = 0
        faces.forEach { face ->
            t = face.boundingBox.width()*face.boundingBox.height()
            threshold = Math.max(threshold, t)
        }
        return threshold/5;
    }

    private fun renderBB(faces: List<Face> ){
        var threshold = findThresholdBB(faces)
        faces.forEach { face ->
            Log.d("FACES:","Values:"+face.boundingBox.toString())
            if ((face.boundingBox.width()*face.boundingBox.height()) <= threshold) {
                var outImage = Bitmap.createBitmap(
                    bitImage,
                    face.boundingBox.left,
                    face.boundingBox.top,
                    face.boundingBox.width(),
                    face.boundingBox.height()
                )
                val rectangle = Rect(
                    face.boundingBox.left,
                    face.boundingBox.top,
                    face.boundingBox.left + face.boundingBox.width(),
                    face.boundingBox.top + face.boundingBox.height()
                )
                outImage = filter.processImage(outImage)
                bitImage = overlayer(bitImage, outImage, rectangle)
            }
        }
        activity?.runOnUiThread {
            outView.setImageBitmap(bitImage)
        }
        Log.d("render","Finished Face render")
    }

    fun saveToGallery(context: Context, bitmap: Bitmap, albumName: String) {
        val filename = "${System.currentTimeMillis()}.png"
        val write: (OutputStream) -> Boolean = {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/$albumName")
            }

            context.contentResolver.let {
                it.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                    it.openOutputStream(uri)?.let(write)
                }
            }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + File.separator + albumName
            val file = File(imagesDir)
            if (!file.exists()) {
                file.mkdir()
            }
            val image = File(imagesDir, filename)
            write(FileOutputStream(image))
        }
        Toast.makeText(getActivity(), "Image Saved" , Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}