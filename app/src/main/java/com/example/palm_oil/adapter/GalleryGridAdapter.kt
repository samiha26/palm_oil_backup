package com.example.palm_oil.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import com.example.palm_oil.data.model.GalleryImage
import java.io.File

class GalleryGridAdapter(
    private val context: Context,
    private var images: List<GalleryImage>
) : BaseAdapter() {

    private var onImageClickListener: ((Int) -> Unit)? = null

    fun setOnImageClickListener(listener: (Int) -> Unit) {
        onImageClickListener = listener
    }

    fun updateImages(newImages: List<GalleryImage>) {
        images = newImages
        notifyDataSetChanged()
    }

    override fun getCount(): Int = images.size

    override fun getItem(position: Int): GalleryImage = images[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView: ImageView = if (convertView is ImageView) {
            convertView
        } else {
            ImageView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    300 // Fixed height for grid items
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(4, 4, 4, 4)
            }
        }

        val galleryImage = images[position]
        loadImageFromPath(galleryImage.imagePath, imageView)

        imageView.setOnClickListener {
            onImageClickListener?.invoke(position)
        }

        return imageView
    }

    private fun loadImageFromPath(imagePath: String, imageView: ImageView) {
        try {
            val file = File(imagePath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                } else {
                    // Set a placeholder or error image
                    imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } else {
                // Image file doesn't exist, show placeholder
                imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Set a placeholder or error image
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }
}
