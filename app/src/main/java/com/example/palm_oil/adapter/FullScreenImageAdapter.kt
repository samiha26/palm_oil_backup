package com.example.palm_oil.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.palm_oil.R
import com.example.palm_oil.data.model.GalleryImage
import java.io.File

class FullScreenImageAdapter(
    private val context: Context,
    private var images: List<GalleryImage>
) : RecyclerView.Adapter<FullScreenImageAdapter.ImageViewHolder>() {

    fun updateImages(newImages: List<GalleryImage>) {
        images = newImages
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_fullscreen_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val galleryImage = images[position]
        holder.bind(galleryImage)
    }

    override fun getItemCount(): Int = images.size

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.fullScreenImageView)

        fun bind(galleryImage: GalleryImage) {
            loadImageFromPath(galleryImage.imagePath, imageView)
        }

        private fun loadImageFromPath(imagePath: String, imageView: ImageView) {
            try {
                val file = File(imagePath)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(imagePath)
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                    } else {
                        imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                    }
                } else {
                    imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }
}
