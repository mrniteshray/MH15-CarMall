package xcom.niteshray.apps.mh15cars.mh15carsmall

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dev.eren.removebg.RemoveBg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var previewImage: ImageView
    private var carBitmap: Bitmap? = null
    private var processedBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewImage = findViewById(R.id.previewImage)
        val uploadButton = findViewById<Button>(R.id.uploadButton)
        val generateButton = findViewById<Button>(R.id.generateButton)

        uploadButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 1)
        }

        generateButton.setOnClickListener {
            carBitmap?.let { bitmap ->
                removeBackground(bitmap)
            } ?: Toast.makeText(this, "Please upload an image first", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data
            carBitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            previewImage.setImageBitmap(carBitmap)
        }
    }

    private fun removeBackground(bitmap: Bitmap) {
        val remover = RemoveBg(this)

        CoroutineScope(Dispatchers.Main).launch {
            remover.clearBackground(bitmap).collect { output ->
                if (output != null) {
                    processedBitmap = output
                    // Background remove hone ke baad banner generate karo
                    val banner = generateBanner(processedBitmap!!)
                    if (banner != null) {
                        previewImage.setImageBitmap(banner)
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to generate banner", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Failed to remove background", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun generateBanner(carImage: Bitmap): Bitmap? {
        try {
            // Device ke screen size ke hisaab se target dimensions set karo
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val targetWidth = displayMetrics.widthPixels  // Screen width
            val targetHeight = (targetWidth * 0.75).toInt()  // Aspect ratio 4:3

            // Template image load karo with downscaling
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true  // Sirf dimensions check karo
            }
            BitmapFactory.decodeResource(resources, R.drawable.template, options)

            // Calculate inSampleSize for downscaling
            options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
            options.inJustDecodeBounds = false  // Ab actual image load karo
            val template = BitmapFactory.decodeResource(resources, R.drawable.template, options)
                ?: return null  // Agar template load nahi hua toh null return karo

            // Template ko target size pe scale karo
            val scaledTemplate = Bitmap.createScaledBitmap(template, targetWidth, targetHeight, true)

            // Banner bitmap banaye
            val banner = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(banner)

            // Template draw karo
            canvas.drawBitmap(scaledTemplate, 0f, 0f, null)

            // Car image ko scale aur center mein draw karne ke liye
            val scaledCar = Bitmap.createScaledBitmap(carImage, targetWidth / 2, targetHeight / 2, true)
            val carX = (targetWidth - scaledCar.width) / 2f
            val carY = (targetHeight - scaledCar.height) / 2f

            // Paint object for shadow effect
            val paint = Paint().apply {
                isAntiAlias = true
                setShadowLayer(10f, 5f, 5f, Color.BLACK)
            }
            canvas.drawBitmap(scaledCar, carX, carY, paint)


            // Memory free karne ke liye
            template.recycle()
            scaledTemplate.recycle()
            scaledCar.recycle()

            return banner
        } catch (e: OutOfMemoryError) {
            Toast.makeText(this, "Memory error: Try a smaller template", Toast.LENGTH_SHORT).show()
            return null
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.d("Imageerror","Error: ${e.message}")
            return null
        }
    }

    // Helper function to calculate inSampleSize for downscaling
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}