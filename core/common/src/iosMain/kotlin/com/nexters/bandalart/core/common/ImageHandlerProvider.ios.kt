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

import androidx.compose.ui.graphics.ImageBitmap
import com.eygraber.uri.Uri
import io.github.aakira.napier.Napier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateWithName
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.kCGBitmapByteOrder32Little
import platform.CoreGraphics.kCGColorSpaceSRGB
import platform.Foundation.NSDate
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIImageWriteToSavedPhotosAlbum

actual class ImageHandlerProvider {
    @Suppress("TooGenericExceptionCaught")
    actual fun externalShareForBitmap(bitmap: ImageBitmap) {
        try {
            val image = bitmap.toUiImage()
            shareBitmap(image)
        } catch (e: Exception) {
            Napier.e("[externalShareFoBitmap] message: ${e.message}")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    actual fun bitmapToFileUri(bitmap: ImageBitmap): Uri? =
        try {
            val filePath = saveBitmapToDisk(bitmap)
            NSURL.fileURLWithPath(filePath).absoluteString?.let { Uri.parse(it) }
        } catch (e: Exception) {
            Napier.e("Failed to convert bitmap to URI: ${e.message}")
            null
        }

    @Suppress("TooGenericExceptionCaught")
    actual fun shareImage(imageUri: Uri) {
        try {
            val nsUrl = NSURL.URLWithString(imageUri.toString()) ?: return
            shareUrl(nsUrl)
        } catch (e: Exception) {
            Napier.e("Failed to share image: ${e.message}")
        }
    }

    actual fun saveBitmapToDisk(bitmap: ImageBitmap): String {
        val fileName = "shared_image_${NSDate().timeIntervalSince1970}.png"
        val tempDir = NSTemporaryDirectory()
        val filePath = (tempDir as NSString).stringByAppendingPathComponent(fileName)

        val image = bitmap.toUiImage()
        if (image != null) {
            UIImagePNGRepresentation(image)?.writeToFile(filePath, true)
        }

        return filePath
    }

    @OptIn(ExperimentalForeignApi::class)
    @Suppress("TooGenericExceptionCaught")
    actual fun saveImageToGallery(bitmap: ImageBitmap) {
        try {
            val image = bitmap.toUiImage()
            if (image != null) {
                UIImageWriteToSavedPhotosAlbum(image, null, null, null)
            }
        } catch (e: Exception) {
            Napier.e("Failed to save image to gallery: ${e.message}")
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    @Suppress("TooGenericExceptionCaught")
    actual fun saveUriToGallery(imageUri: Uri) {
        try {
            val nsUrl = NSURL.URLWithString(imageUri.toString()) ?: return
            val image = UIImage.imageWithContentsOfFile(nsUrl.path!!)
            if (image != null) {
                UIImageWriteToSavedPhotosAlbum(image, null, null, null)
            }
        } catch (e: Exception) {
            Napier.e("Failed to save image to gallery: ${e.message}")
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun ImageBitmap.toUiImage(): UIImage? {
        val buffer = IntArray(width * height)
        readPixels(buffer)

        // https://github.com/takahirom/roborazzi/blob/main/roborazzi-compose-ios/src/iosMain/kotlin/io/github/takahirom/roborazzi/RoborazziIos.kt#L88C51-L88C68
        val colorSpace = CGColorSpaceCreateWithName(kCGColorSpaceSRGB)
        val bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedFirst.value or kCGBitmapByteOrder32Little
        val context =
            CGBitmapContextCreate(
                data = buffer.refTo(0),
                width = width.toULong(),
                height = height.toULong(),
                bitsPerComponent = 8u,
                bytesPerRow = (4 * width).toULong(),
                space = colorSpace,
                bitmapInfo = bitmapInfo,
            )

        val cgImage = CGBitmapContextCreateImage(context)
        return cgImage?.let { UIImage.imageWithCGImage(it) }
    }

    private fun shareBitmap(bitmap: UIImage?) {
        bitmap ?: return
        val activityViewController =
            UIActivityViewController(
                activityItems = listOf(bitmap),
                applicationActivities = null,
            )
        UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
            activityViewController,
            animated = true,
            completion = null,
        )
    }

    private fun shareUrl(nsUrl: NSURL) {
        val activityViewController =
            UIActivityViewController(
                activityItems = listOf(nsUrl),
                applicationActivities = null,
            )
        UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
            activityViewController,
            animated = true,
            completion = null,
        )
    }
}
