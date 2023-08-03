package com.group.nine.camerafilter

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
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.drawToBitmap
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
class ImageProcessor : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private lateinit var srcView : ImageView
    private lateinit var outView : ImageView
    private lateinit var imageURI : Uri
    private lateinit var bitImage : Bitmap
    private lateinit var numFacesLabel : TextView
    private lateinit var progressWheel : ProgressBar
    private var threshholdFactor : Int = 5
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var filter : ImageFilter

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
        numFacesLabel = view.findViewById(R.id.number_faces)
        progressWheel = view.findViewById(R.id.progressWheel)
        filter = ImageFilter(view.context,an_clusters = 5, apoly_epsilon = 4.0)

        //Sliders
        view.findViewById<Slider>(R.id.clusterSlider).addOnChangeListener { slider, value, fromUser ->
            Log.d("Cluster Slider:","Setting Cluster to :"+slider.value.toString())
            filter.setClusterSize(value.toInt())
        }
        view.findViewById<Slider>(R.id.epsilonSlider).addOnChangeListener { epsilonSlider, value, fromUser ->
            Log.d("Epsilon Slider:","Setting Epsilon to :"+ epsilonSlider.value.toString())
            filter.setPolyEpsilon(value.toDouble())
        }
        view.findViewById<Slider>(R.id.areaSlider).addOnChangeListener { areaSlider, value, fromUser ->
            Log.d("Area Slider:","Setting min area to :"+ areaSlider.value.toString())
            filter.setMinArea(value.toDouble())
        }
        view.findViewById<Slider>(R.id.threshhold_slider).addOnChangeListener { threshSlider, value, fromUser ->
            Log.d("Epsilon Slider:","Setting Epsilon to :"+ threshSlider.value.toString())
            threshholdFactor = value.toInt()
        }

        setFragmentResultListener("ImageUri"){ requestKey, bundle ->
            imageURI = Uri.parse(bundle.getString("result"))
            val imSource = ImageDecoder.createSource(requireActivity().contentResolver,imageURI)
            bitImage = ImageDecoder.decodeBitmap(imSource){ decoder, _, _ ->
                decoder.isMutableRequired = true
            }
            srcView.setImageBitmap(bitImage)
        }

        //Buttons

        view.findViewById<Button>(R.id.process_image).setOnClickListener {
            progressWheel.visibility = View.VISIBLE
            processImage(faceDetector,view.context)
        }
        binding.saveImage.setOnClickListener {
            saveToGallery(view.context, "PML598")
        }
        binding.previousActivity.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
    }

    private fun processImage(faceDetector:FaceDetector, context:Context) {
        val inputImage : InputImage = InputImage.fromFilePath(context,imageURI)
        faceDetector.process(inputImage)
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
        faces.forEach { face ->
            threshold = Math.max(threshold, face.boundingBox.width()*face.boundingBox.height())
        }
        return threshold/threshholdFactor;
    }

    private fun correctDims(bb:Rect,imWidth:Int,imheight:Int):Pair<Int,Int>{
        var width = bb.width()
        var height = bb.height()
        if(bb.left+bb.width() > imWidth){
            width = imWidth - bb.left
        }
        if(bb.top+height > imheight){
            height =  imheight - bb.top
        }
        return Pair(width,height)
    }

    private fun renderBB(faces: List<Face> ){
        numFacesLabel.text = faces.size.toString()
        var threshold = findThresholdBB(faces)
        var outFinalImage = bitImage.copy(bitImage.config,true)
        faces.forEach { face ->
            Log.d("FACES:","Values:"+face.boundingBox.toString())
            if ((face.boundingBox.width()*face.boundingBox.height()) <= threshold) {
                Log.d("Bitmap Creation","x:"+face.boundingBox.left.toString()+" | width()="+face.boundingBox.width().toString() + " | bitmap.width()="+ bitImage.width.toString())
                val (correctWidth,correctHeight) = correctDims(face.boundingBox,bitImage.width,bitImage.height)
                var outImage = Bitmap.createBitmap(bitImage,face.boundingBox.left,face.boundingBox.top,correctWidth,correctHeight)
                val rectangle = Rect(
                    face.boundingBox.left,
                    face.boundingBox.top,
                    face.boundingBox.left + face.boundingBox.width(),
                    face.boundingBox.top + face.boundingBox.height()
                )
                progressWheel.visibility = View.VISIBLE
                outImage = filter.processImage(outImage)
                outFinalImage = overlayer(outFinalImage, outImage, rectangle)
            }
        }
        activity?.runOnUiThread {
            outView.setImageBitmap(outFinalImage)
            outView.invalidate()
            progressWheel.visibility = View.GONE
        }
        Log.d("render","Finished Face render")
    }

    fun saveToGallery(context: Context, albumName: String) {
        val filename = "${System.currentTimeMillis()}.png"
        val write: (OutputStream) -> Boolean = {
            outView.drawToBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
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