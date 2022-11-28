package com.group.nine.camerafilter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.group.nine.camerafilter.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var imView : ImageView
    private lateinit var loadImage : Button
    private lateinit var imageURI : Uri
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        imView = view.findViewById(R.id.ImageViewer)
        loadImage = view.findViewById(R.id.load_image)
        val getContent = registerForActivityResult(ActivityResultContracts.GetContent()){ uri:Uri? ->
            uri?.let{imageUri->
                imageURI = imageUri
                imView.setImageURI(imageUri)
                view.findViewById<TextView>(R.id.textview_first).text = "Loaded Image"
                loadImage.text = "Change Image?"
            }
        }
        loadImage.setOnClickListener {
            if(ContextCompat.checkSelfPermission(view.context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                val galleryIntent :Intent= Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                view.findViewById<TextView>(R.id.textview_first).text = "Loading Image"
                getContent?.launch("image/*")
            }
            else{
                view.findViewById<TextView>(R.id.textview_first).text = "No permissions"
                activity?.requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),2000)
            }
        }
        binding.nextActivity.setOnClickListener {
            setFragmentResult("ImageUri", bundleOf("result" to imageURI.toString()))
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}