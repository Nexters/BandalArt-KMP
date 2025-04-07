/*
 * Copyright 2025 easyhooon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nexters.bandalart.core.common

import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import com.eygraber.uri.Uri
import io.github.aakira.napier.Napier
import android.graphics.Bitmap.CompressFormat.PNG
import kotlinx.datetime.Clock
import java.io.File
import java.io.FileOutputStream

import android.net.Uri as AndroidUri // Android 플랫폼용 Uri

actual class ImageHandlerProvider(
    private val context: Application
) {
    private val contentResolver: ContentResolver get() = context.contentResolver

    @Suppress("TooGenericExceptionCaught")
    actual fun externalShareForBitmap(bitmap: ImageBitmap) {
        try {
            val file = File(saveBitmapToDisk(bitmap))
            val androidUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

            val intent =
                ShareCompat
                    .IntentBuilder(context)
                    .setStream(androidUri)
                    .setType("image/png")
                    .intent

            context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK))
        } catch (e: Exception) {
            Napier.e("[externalShareFoBitmap] message: ${e.message}")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    actual fun bitmapToFileUri(bitmap: ImageBitmap): Uri? =
        try {
            val file = File(saveBitmapToDisk(bitmap))
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file).toKmpUri()
        } catch (e: Exception) {
            Napier.e("Failed to convert bitmap to URI: ${e.message}")
            null
        }

    @Suppress("TooGenericExceptionCaught")
    actual fun shareImage(imageUri: Uri) {
        try {
            ShareCompat
                .IntentBuilder(context)
                .setStream(imageUri.toAndroidUri())
                .setType("image/png")
                .startChooser()
        } catch (e: Exception) {
            Napier.e("Failed to share image: ${e.message}")
        }
    }

    actual fun saveBitmapToDisk(bitmap: ImageBitmap): String {
        val fileName = "shared_image_${Clock.System.now().toEpochMilliseconds()}.png"
        val cachePath = File(context.cacheDir, "images").also { it.mkdirs() }
        val file = File(cachePath, fileName)
        val outputStream = FileOutputStream(file)

        bitmap.asAndroidBitmap().compress(PNG, 100, outputStream)
        outputStream.flush()
        outputStream.close()

        return file.absolutePath
    }

    @Suppress("TooGenericExceptionCaught")
    actual fun saveImageToGallery(bitmap: ImageBitmap) {
        try {
            val fileName = "bandalart_${Clock.System.now().toEpochMilliseconds()}.png"
            val contentValues =
                ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

            val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            imageUri?.let { uri ->
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.asAndroidBitmap().compress(PNG, 100, outputStream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }
            }
        } catch (e: Exception) {
            Napier.e("Failed to save image to gallery: ${e.message}")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    actual fun saveUriToGallery(imageUri: Uri) {
        try {
            val fileName = "bandalart_${Clock.System.now().toEpochMilliseconds()}.png"
            val contentValues = createContentValues(fileName)
            val destinationUri =
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return

            copyUriContent(imageUri.toAndroidUri(), destinationUri)
            updatePendingStatus(destinationUri, contentValues)
        } catch (e: Exception) {
            Napier.e("Failed to save image to gallery: ${e.message}")
        }
    }

    private fun createContentValues(fileName: String) =
        ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

    private fun copyUriContent(
        sourceUri: AndroidUri,
        destinationUri: AndroidUri
    ) {
        contentResolver.openInputStream(sourceUri)?.use { input ->
            contentResolver.openOutputStream(destinationUri)?.use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun updatePendingStatus(
        uri: AndroidUri,
        contentValues: ContentValues
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(uri, contentValues, null, null)
        }
    }

    private fun AndroidUri.toKmpUri(): Uri = Uri.parse(toString())

    private fun Uri.toAndroidUri(): AndroidUri = AndroidUri.parse(toString())
}
