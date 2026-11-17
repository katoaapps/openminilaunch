package com.katoaapps.minkboard.data

internal interface FieldCipher {
    fun encrypt(value: String, subject: String, field: String): ByteArray
    fun decrypt(value: ByteArray, subject: String, field: String): String
}
