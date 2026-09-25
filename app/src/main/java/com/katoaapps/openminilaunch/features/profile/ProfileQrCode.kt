package com.katoaapps.openminilaunch.features.profile

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.security.MessageDigest

internal enum class ProfileQrDensity { NORMAL, DENSE, TOO_DENSE }

internal data class ProfileQrCode(
    val payload: String,
    val payloadHash: String,
    val modules: Int,
    val density: ProfileQrDensity,
)

internal object ProfileQrGenerator {
    private const val QUIET_ZONE_MODULES = 4
    private const val MAX_CACHED_BITMAPS = 4
    private val bitmapCache = object : LinkedHashMap<String, Bitmap>(MAX_CACHED_BITMAPS, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>): Boolean {
            val remove = size > MAX_CACHED_BITMAPS
            if (remove) eldest.value.recycle()
            return remove
        }
    }
    private val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to QUIET_ZONE_MODULES,
    )

    fun describe(payload: String): ProfileQrCode {
        val intrinsic = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 1, 1, hints)
        val modules = intrinsic.width - QUIET_ZONE_MODULES * 2
        val density = when {
            modules <= 77 -> ProfileQrDensity.NORMAL
            modules <= 97 -> ProfileQrDensity.DENSE
            else -> ProfileQrDensity.TOO_DENSE
        }
        return ProfileQrCode(payload, payload.sha256(), modules, density)
    }

    fun describe(card: ProfileCard, links: List<ProfileLink>): ProfileQrCode? = runCatching {
        require(card.fullName.isNotBlank())
        require(card.resolveSelectedLinks(links).all(ProfileVCard::isValid))
        describe(ProfileVCard.create(card, links))
    }.getOrNull()

    fun render(code: ProfileQrCode, sizePx: Int): Bitmap {
        require(code.density != ProfileQrDensity.TOO_DENSE) { "Profile QR is too dense" }
        val size = sizePx.coerceAtLeast(1)
        val cacheKey = "${code.payloadHash}:$size"
        synchronized(bitmapCache) {
            bitmapCache[cacheKey]?.takeUnless(Bitmap::isRecycled)?.let {
                return it.copy(Bitmap.Config.ARGB_8888, false)
            }
        }
        val matrix = QRCodeWriter().encode(code.payload, BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(matrix.width * matrix.height)
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                pixels[y * matrix.width + x] = if (matrix[x, y]) BLACK else WHITE
            }
        }
        val rendered = createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
        }
        synchronized(bitmapCache) {
            bitmapCache.put(cacheKey, rendered)?.takeUnless(Bitmap::isRecycled)?.recycle()
        }
        return rendered.copy(Bitmap.Config.ARGB_8888, false)
    }

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private const val BLACK = 0xff000000.toInt()
    private const val WHITE = 0xffffffff.toInt()
}
