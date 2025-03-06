package com.nexters.bandalart.core.common.extension

// suspend fun Task<AppUpdateInfo>.await(): AppUpdateInfo {
//     return suspendCoroutine { continuation ->
//         addOnCompleteListener { result ->
//             if (result.isSuccessful) {
//                 continuation.resume(result.result)
//             } else {
//                 result.exception?.let { continuation.resumeWithException(it) }
//             }
//         }
//     }
// }
