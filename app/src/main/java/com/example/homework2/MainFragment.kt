package com.example.homework2

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.homework2.databinding.FragmentMainBinding
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val imagePaths: MutableList<String> = mutableListOf()
    private val disposableBag = CompositeDisposable()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMainBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.startLoading.setOnClickListener(::startLoading)
        binding.cancelLoading.setOnClickListener(::cancelLoading)

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                MY_PERMISSIONS_REQUEST_READ_IMAGES
            )
        } else {
            readImages()
        }
    }

    private fun startLoading(view: View) {
        val disposable = Flowable.fromIterable(imagePaths)
            .subscribeOn(Schedulers.io())
            .doOnNext {
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(::load, Throwable::printStackTrace)

        disposableBag.add(disposable)
    }

    private fun cancelLoading(view: View) {
        disposableBag.clear()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_IMAGES -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    readImages()
                }
                return
            }
        }
    }

    private fun readImages() {

        val imageCursor = requireActivity().contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media.DATA),
            null,
            null,
            null
        ) ?: return

        repeat(imageCursor.count) {
            imageCursor.moveToNext()
            imagePaths.add(imageCursor.getString(0))
        }

        imageCursor.close()
    }

    private fun load(imagePath: String) {
        try {
            binding.imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        disposableBag.clear()
    }


    companion object {
        fun newInstance() = MainFragment()
        private const val MY_PERMISSIONS_REQUEST_READ_IMAGES = 0
    }
}