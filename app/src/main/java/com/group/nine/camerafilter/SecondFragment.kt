package com.group.nine.camerafilter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Im
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.group.nine.camerafilter.databinding.FragmentSecondBinding
import com.group.nine.camerafilter.ImageFilter
/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private lateinit var srcView : ImageView
    private lateinit var outView : ImageView
    private lateinit var tv : TextView
    private lateinit var imageURI : Uri
    private lateinit var processImageButton : Button
    private lateinit var faceDetector : FaceDetector
    private lateinit var bitImage : Bitmap
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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
        tv =  view.findViewById(R.id.bounding_values)
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
            .addOnCompleteListener{
                renderBB(it.result)
            }
    }

    private fun renderBB(faces: List<Face> ){
        val filter = ImageFilter()
        faces.forEach { face ->
            Log.d("FACES:","Values:"+face.boundingBox.toString())
            var outImage = Bitmap.createBitmap(bitImage,face.boundingBox.left,face.boundingBox.top,face.boundingBox.width(),face.boundingBox.height())
            outImage = filter.kmeansProcess(outImage)
            outImage = filter.processFace(outImage)
            activity?.runOnUiThread {
                tv.text = tv.text.toString() + face.boundingBox.toString()
                outView.setImageBitmap(outImage)
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}