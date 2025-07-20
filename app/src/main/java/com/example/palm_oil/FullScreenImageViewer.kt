package com.example.palm_oil

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.palm_oil.adapter.FullScreenImageAdapter
import com.example.palm_oil.data.model.GalleryImage

class FullScreenImageViewer : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var imageInfoText: TextView
    private lateinit var adapter: FullScreenImageAdapter
    
    private var images: ArrayList<GalleryImage> = arrayListOf()
    private var currentPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image_viewer)

        // Hide action bar for fullscreen experience
        supportActionBar?.hide()

        initializeViews()
        setupViewPager()
        getIntentData()
    }

    private fun initializeViews() {
        viewPager = findViewById(R.id.viewPager)
        imageInfoText = findViewById(R.id.imageInfoText)
        
        findViewById<View>(R.id.closeButton).setOnClickListener {
            finish()
        }
    }

    private fun setupViewPager() {
        adapter = FullScreenImageAdapter(this, images)
        viewPager.adapter = adapter
        
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPosition = position
                updateImageInfo(position)
            }
        })
    }

    private fun getIntentData() {
        currentPosition = intent.getIntExtra("CURRENT_POSITION", 0)
        
        // Get image paths and create GalleryImage objects
        val imagePaths = intent.getStringArrayListExtra("IMAGE_PATHS") ?: arrayListOf()
        val treeIds = intent.getStringArrayListExtra("TREE_IDS") ?: arrayListOf()
        val plotIds = intent.getStringArrayListExtra("PLOT_IDS") ?: arrayListOf()
        val createdAts = intent.getLongArrayExtra("CREATED_ATS") ?: longArrayOf()
        val formIds = intent.getLongArrayExtra("FORM_IDS") ?: longArrayOf()
        val imageIndexes = intent.getIntArrayExtra("IMAGE_INDEXES") ?: intArrayOf()

        // Create GalleryImage objects
        for (i in imagePaths.indices) {
            val galleryImage = GalleryImage(
                imagePath = imagePaths[i],
                formId = if (i < formIds.size) formIds[i] else 0L,
                treeId = if (i < treeIds.size) treeIds[i] else "",
                plotId = if (i < plotIds.size) plotIds[i] else "",
                createdAt = if (i < createdAts.size) createdAts[i] else 0L,
                imageIndex = if (i < imageIndexes.size) imageIndexes[i] else 1
            )
            images.add(galleryImage)
        }

        adapter.updateImages(images)
        viewPager.setCurrentItem(currentPosition, false)
        updateImageInfo(currentPosition)
    }

    private fun updateImageInfo(position: Int) {
        if (position < images.size) {
            val image = images[position]
            val info = "Tree: ${image.treeId} | Plot: ${image.plotId} | Image ${image.imageIndex}\n" +
                      "${position + 1} of ${images.size}"
            imageInfoText.text = info
        }
    }
}
