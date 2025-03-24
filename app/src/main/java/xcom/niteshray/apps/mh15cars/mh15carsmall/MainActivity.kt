package xcom.niteshray.apps.mh15cars.mh15carsmall


import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dev.eren.removebg.RemoveBg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var previewImage: ImageView
    private lateinit var showroomName: EditText
    private lateinit var showroomAddress: EditText
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
                    previewImage.setImageBitmap(banner)
                } else {
                    Toast.makeText(this@MainActivity, "Failed to remove background", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun generateBanner(carImage: Bitmap): Bitmap {
        // Template image load karo (res/drawable mein banner_template.png hona chahiye)
        val template = BitmapFactory.decodeResource(resources, R.drawable.template)
        val banner = Bitmap.createBitmap(template.width, template.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(banner)

        // Template draw karo
        canvas.drawBitmap(template, 0f, 0f, null)

        // Car image ko scale aur center mein draw karo
        val scaledCar = Bitmap.createScaledBitmap(carImage, template.width / 2, template.height / 2, true)
        val carX = (template.width - scaledCar.width) / 2f
        val carY = (template.height - scaledCar.height) / 2f

        // Paint object for shadow effect
        val paint = Paint().apply {
            isAntiAlias = true
            setShadowLayer(10f, 5f, 5f, Color.BLACK)  // Shadow effect for natural look
        }
        canvas.drawBitmap(scaledCar, carX, carY, paint)

        // Showroom name aur address draw karo
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 50f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val name = " "
        val address = " "

        // Showroom name upar
        canvas.drawText(name, template.width / 2f, 100f, textPaint)

        // Address neeche
        textPaint.textSize = 40f
        canvas.drawText(address, template.width / 2f, template.height - 50f, textPaint)

        return banner
    }
}