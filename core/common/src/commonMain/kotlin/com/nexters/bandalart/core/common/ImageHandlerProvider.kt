package com.nexters.bandalart.core.common

import androidx.compose.ui.graphics.ImageBitmap
import com.eygraber.uri.Uri

expect class ImageHandlerProvider {
    fun externalShareForBitmap(bitmap: ImageBitmap)
    fun bitmapToFileUri(bitmap: ImageBitmap): Uri?
    fun shareImage(imageUri: Uri)
    fun saveImageToGallery(bitmap: ImageBitmap)
    fun saveUriToGallery(imageUri: Uri)
    fun saveBitmapToDisk(bitmap: ImageBitmap): String
}
