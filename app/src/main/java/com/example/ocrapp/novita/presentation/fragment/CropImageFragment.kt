package com.example.ocrapp.novita.presentation.fragment

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.ocrapp.novita.R
import com.example.ocrapp.novita.databinding.FragmentCropImageBinding
import com.example.ocrapp.novita.presentation.component.progress_indicator.ProgressIndicator
import com.example.ocrapp.novita.util.TextAnalyser
import com.snatik.storage.Storage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CropImageFragment : Fragment(R.layout.fragment_crop_image) {

    private var _binding: FragmentCropImageBinding? = null
    private val binding get() = _binding!!
    private lateinit var progressIndicator: ProgressIndicator
    private val cropImageFragmentArgs: CropImageFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCropImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        progressIndicator = ProgressIndicator(requireContext(), false)
        binding.cropImageView.setImageUriAsync(Uri.parse(cropImageFragmentArgs.uri))
        binding.btnNext.setOnClickListener {
            progressIndicator.show()
            analyzeImage()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun analyzeImage() {
        val img = binding.cropImageView.croppedImage
        val file = createFile(
            getOutputDirectory(requireContext()),
            "yyyy-MM-dd-HH-mm-ss-SSS",
            ".png"
        )

        FileOutputStream(file).use { out ->
            img.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        GlobalScope.launch(Dispatchers.IO) {
            TextAnalyser({ scanResult ->
                if (scanResult.isEmpty()) {
                    progressIndicator.dismiss()
                    Toast.makeText(
                        requireContext(),
                        "No Text Detected",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    progressIndicator.dismiss()
                    findNavController().navigate(
                        R.id.action_cameraFragment_to_displayResultFragment,
                        Bundle().apply {
                            putString("text", scanResult)
                        }
                    )
                }
            }, requireContext(), Uri.fromFile(file)).analyseImage()
        }
    }

    @Suppress("SameParameterValue")
    private fun createFile(baseFolder: File, format: String, extension: String) =
        File(
            baseFolder,
            SimpleDateFormat(format, Locale.US).format(System.currentTimeMillis()) + extension
        )

    private fun getOutputDirectory(context: Context): File {
        val storage = Storage(context)
        val mediaDir = storage.internalCacheDirectory?.let {
            File(it, "OCR").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
    }
}