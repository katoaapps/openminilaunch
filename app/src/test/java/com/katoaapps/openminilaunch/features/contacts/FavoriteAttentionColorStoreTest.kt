package com.katoaapps.openminilaunch.features.contacts

import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteAttentionColorStoreTest {
    @Test
    fun codecRoundTripsOpaqueColorsWithoutExposingFieldsOutsideEncryption() {
        val colors = mapOf(
            "android-contact:amy" to 0xFF005BBB.toInt(),
            "integrated-contact:gary" to 0xFFCC5500.toInt(),
        )

        assertEquals(
            colors,
            FavoriteAttentionColorCodec.decode(FavoriteAttentionColorCodec.encode(colors)),
        )
    }
}
