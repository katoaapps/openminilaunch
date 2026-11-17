package com.katoaapps.minkboard.data

import com.katoaapps.minkboard.core.LauncherBackupEnvelopeCodec
import com.katoaapps.minkboard.core.LauncherBackupEnvelopeDecodeResult
import com.katoaapps.minkboard.core.LauncherBackupEnvelopeEncodeResult
import com.katoaapps.minkboard.core.LauncherMinkBoardBackup
import java.security.SecureRandom

/** Passphrase-encrypted transport for a launcher's outer `minkBoard` backup field. */
class PassphraseLauncherBackupEnvelopeCodec internal constructor(
    random: SecureRandom,
    writeIterations: Int,
) : LauncherBackupEnvelopeCodec {
    private val payloadJson = LauncherBackupPayloadJsonCodec()
    private val envelopeCrypto = LauncherBackupEnvelopeCrypto(random, writeIterations)

    constructor() : this(
        random = SecureRandom(),
        writeIterations = LauncherBackupEnvelopeCrypto.DEFAULT_KDF_ITERATIONS,
    )

    override fun encode(
        backup: LauncherMinkBoardBackup.Present,
        secret: CharArray,
    ): LauncherBackupEnvelopeEncodeResult = try {
        LauncherBackupEnvelopeEncodeResult.Encrypted(
            envelopeCrypto.encode(payloadJson.encode(backup), secret),
        )
    } catch (_: Exception) {
        LauncherBackupEnvelopeEncodeResult.Failed
    }

    override fun decode(
        envelope: ByteArray,
        secret: CharArray,
    ): LauncherBackupEnvelopeDecodeResult = try {
        LauncherBackupEnvelopeDecodeResult.Decoded(
            payloadJson.decode(envelopeCrypto.decode(envelope, secret)),
        )
    } catch (_: UnsupportedLauncherBackupEnvelopeException) {
        LauncherBackupEnvelopeDecodeResult.Unsupported
    } catch (_: UnsupportedLauncherBackupPayloadException) {
        LauncherBackupEnvelopeDecodeResult.Unsupported
    } catch (_: Exception) {
        LauncherBackupEnvelopeDecodeResult.Corrupt
    }
}
