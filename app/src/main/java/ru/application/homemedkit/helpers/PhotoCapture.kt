package ru.application.homemedkit.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.core.net.toUri
import java.io.File

class PhotoCapture(val context: Context, val onSave: (String) -> Unit) : ImageCapture.OnImageCapturedCallback() {
    override fun onCaptureSuccess(image: ImageProxy) {
        super.onCaptureSuccess(image)

        val matrix = Matrix().apply {
            postRotate(image.imageInfo.rotationDegrees.toFloat())
        }

        val bitmap = Bitmap.createBitmap(
            image.toBitmap(),
            0,
            0,
            image.width,
            image.height,
            matrix,
            true
        )

        val name = "${System.currentTimeMillis()}_product.jpg"
        val file = File(context.filesDir, name)

        context.contentResolver.openOutputStream(file.toUri())?.let {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, it)
        }

        onSave(name)
    }

    override fun onError(exception: ImageCaptureException) {
        exception.printStackTrace()
    }
}