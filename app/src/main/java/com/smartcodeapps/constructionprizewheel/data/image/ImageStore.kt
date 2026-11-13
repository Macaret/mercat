package com.smartcodeapps.constructionprizewheel.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.smartcodeapps.constructionprizewheel.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    private val prizeDir = File(context.filesDir, "prize_images")
    private val logoDir = File(context.filesDir, "logo")

    suspend fun savePrizeImage(source: Uri): String = withContext(io) {
        val bitmap = decode(source, PRIZE_MAX_EDGE)
        val square = ImageGeometry.centerSquare(bitmap.width, bitmap.height)
        val cropped = Bitmap.createBitmap(bitmap, square.x, square.y, square.size, square.size)
        write(scaleToFit(cropped, PRIZE_MAX_EDGE), prizeDir, Bitmap.CompressFormat.JPEG, "jpg")
    }

    suspend fun saveLogo(source: Uri): String = withContext(io) {
        val bitmap = decode(source, LOGO_MAX_EDGE)
        write(scaleToFit(bitmap, LOGO_MAX_EDGE), logoDir, Bitmap.CompressFormat.PNG, "png")
    }

    suspend fun deleteLogosExcept(keep: String?) = withContext(io) {
        val keepPath = keep?.let { File(it).absolutePath }
        logoDir.listFiles()?.filter { it.absolutePath != keepPath }?.forEach { it.delete() }
    }

    private fun scaleToFit(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val (w, h) = ImageGeometry.fitWithin(bitmap.width, bitmap.height, maxEdge)
        return if (w == bitmap.width && h == bitmap.height) bitmap
        else Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    suspend fun deletePrizeImagesExcept(keep: Collection<String>) = withContext(io) {
        val keepFiles = keep.map { File(it).absolutePath }.toSet()
        prizeDir.listFiles()?.filter { it.absolutePath !in keepFiles }?.forEach { it.delete() }
    }

    suspend fun deleteAllPrizeImages() = withContext(io) {
        prizeDir.listFiles()?.forEach { it.delete() }
    }

    suspend fun deleteLogo() = withContext(io) {
        logoDir.listFiles()?.forEach { it.delete() }
    }

    private fun decode(source: Uri, maxEdge: Int): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Cannot read image")

        val options = BitmapFactory.Options().apply {
            inSampleSize = ImageGeometry.sampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
        }
        val decoded = resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Cannot decode image")

        val orientation = resolver.openInputStream(source)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        return if (degrees == 0f) decoded else Bitmap.createBitmap(
            decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(degrees) }, true
        )
    }

    private fun write(bitmap: Bitmap, dir: File, format: Bitmap.CompressFormat, extension: String): String {
        dir.mkdirs()
        val file = File(dir, "${UUID.randomUUID()}.$extension")
        file.outputStream().use { bitmap.compress(format, 90, it) }
        return file.absolutePath
    }

    private companion object {
        const val PRIZE_MAX_EDGE = 800
        const val LOGO_MAX_EDGE = 1200
    }
}
