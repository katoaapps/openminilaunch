package com.katoaapps.minkboard.core

/**
 * Opaque encrypted transport for the launcher-owned `minkBoard` backup field.
 *
 * The codec implementation owns the wire format and encryption. The core workflow only accepts a
 * successfully decoded, validated [LauncherMinkBoardBackup.Present] value. A missing outer field is
 * represented separately by [LauncherMinkBoardBackupField.Missing], so an unreadable present field
 * can never be mistaken for a backup that predates MinkBoard.
 *
 * The caller retains ownership of [secret] and must clear it when the complete host operation ends.
 */
interface LauncherBackupEnvelopeCodec {
    fun encode(
        backup: LauncherMinkBoardBackup.Present,
        secret: CharArray,
    ): LauncherBackupEnvelopeEncodeResult

    fun decode(
        envelope: ByteArray,
        secret: CharArray,
    ): LauncherBackupEnvelopeDecodeResult
}

sealed interface LauncherBackupEnvelopeEncodeResult {
    data class Encrypted(val envelope: ByteArray) : LauncherBackupEnvelopeEncodeResult
    data object Failed : LauncherBackupEnvelopeEncodeResult
}

sealed interface LauncherBackupEnvelopeDecodeResult {
    data class Decoded(
        val backup: LauncherMinkBoardBackup.Present,
    ) : LauncherBackupEnvelopeDecodeResult

    /** The field is well formed enough to identify, but uses an unsupported format or version. */
    data object Unsupported : LauncherBackupEnvelopeDecodeResult

    /** The field is malformed, damaged, unauthenticated, or cannot be decrypted. */
    data object Corrupt : LauncherBackupEnvelopeDecodeResult
}

sealed interface LauncherMinkBoardBackupField {
    /** The outer launcher backup genuinely has no `minkBoard` field. */
    data object Missing : LauncherMinkBoardBackupField

    /** The outer field exists. Even an empty byte array is present input and must fail closed. */
    data class Encrypted(val envelope: ByteArray) : LauncherMinkBoardBackupField
}

sealed interface LauncherEncryptedBackupCreationResult {
    data class Ready(val envelope: ByteArray) : LauncherEncryptedBackupCreationResult
    data object UnboundWorkspace : LauncherEncryptedBackupCreationResult
    data class BrokenBinding(
        val problem: LauncherBindingProblem,
    ) : LauncherEncryptedBackupCreationResult

    data object EncryptionFailed : LauncherEncryptedBackupCreationResult
}

/**
 * A prepared restore has already distinguished missing input, decoded rich data, unsupported
 * encryption, and corrupt data without mutating the destination.
 */
sealed interface LauncherBackupPreparationResult {
    data class Prepared(
        val plan: LauncherBackupRestorePlan,
    ) : LauncherBackupPreparationResult

    data object UnsupportedEnvelope : LauncherBackupPreparationResult
    data object CorruptEnvelope : LauncherBackupPreparationResult
    data object MissingSecret : LauncherBackupPreparationResult
    data object InvalidRichBackup : LauncherBackupPreparationResult
    data object CorruptLegacyData : LauncherBackupPreparationResult
}

sealed interface LauncherBackupApplyResult {
    data object LeftDestinationUntouched : LauncherBackupApplyResult
    data object ExplicitlyEmptyWorkspace : LauncherBackupApplyResult

    data class LegacyNormalized(
        val result: LauncherNormalizationResult,
    ) : LauncherBackupApplyResult

    data class WorkspaceImported(
        val outcome: LauncherWorkspaceImportOutcome.Imported,
    ) : LauncherBackupApplyResult

    /** A legacy normalization plan requires the destination host's actual legacy schema version. */
    data object MissingLegacySchemaVersion : LauncherBackupApplyResult

    /** Defensive result for a rejected plan; no destination mutation occurred. */
    data object RejectedInvalidRichBackup : LauncherBackupApplyResult

    /** Defensive result for a rejected plan; no destination mutation occurred. */
    data object RejectedCorruptLegacyData : LauncherBackupApplyResult
}

/**
 * Product-neutral launcher backup orchestration.
 *
 * Hosts still own their outer backup JSON, passphrase/recovery UI, legacy preferences, and file IO.
 * This workflow guarantees that only a missing outer field falls back to legacy data, and that all
 * rich imports continue through the existing atomic graph/designation transaction.
 */
class MinkBoardLauncherBackupWorkflow(
    private val facade: MinkBoardLauncherFacade,
    private val envelopeCodec: LauncherBackupEnvelopeCodec,
) {
    suspend fun createEncryptedBackup(
        sourceApp: String,
        secret: CharArray,
    ): LauncherEncryptedBackupCreationResult = when (
        val result = facade.createInMemoryBackup(sourceApp)
    ) {
        is LauncherBackupCreationResult.Ready -> when (
            val encoded = envelopeCodec.encode(result.backup, secret)
        ) {
            is LauncherBackupEnvelopeEncodeResult.Encrypted ->
                LauncherEncryptedBackupCreationResult.Ready(encoded.envelope)
            LauncherBackupEnvelopeEncodeResult.Failed ->
                LauncherEncryptedBackupCreationResult.EncryptionFailed
        }
        LauncherBackupCreationResult.UnboundWorkspace ->
            LauncherEncryptedBackupCreationResult.UnboundWorkspace
        is LauncherBackupCreationResult.BrokenBinding ->
            LauncherEncryptedBackupCreationResult.BrokenBinding(result.problem)
    }

    fun prepareRestore(
        minkBoardField: LauncherMinkBoardBackupField,
        secret: CharArray?,
        legacyTodos: LegacyTodoSource,
    ): LauncherBackupPreparationResult {
        val backup = when (minkBoardField) {
            LauncherMinkBoardBackupField.Missing -> LauncherMinkBoardBackup.Absent
            is LauncherMinkBoardBackupField.Encrypted -> {
                val recoverySecret = secret
                    ?: return LauncherBackupPreparationResult.MissingSecret
                when (val decoded = envelopeCodec.decode(minkBoardField.envelope, recoverySecret)) {
                    is LauncherBackupEnvelopeDecodeResult.Decoded -> decoded.backup
                    LauncherBackupEnvelopeDecodeResult.Unsupported ->
                        return LauncherBackupPreparationResult.UnsupportedEnvelope
                    LauncherBackupEnvelopeDecodeResult.Corrupt ->
                        return LauncherBackupPreparationResult.CorruptEnvelope
                }
            }
        }

        return when (val plan = LauncherBackupReconciler.plan(backup, legacyTodos)) {
            LauncherBackupRestorePlan.InvalidRichBackup ->
                LauncherBackupPreparationResult.InvalidRichBackup
            LauncherBackupRestorePlan.CorruptLegacyData ->
                LauncherBackupPreparationResult.CorruptLegacyData
            else -> LauncherBackupPreparationResult.Prepared(plan)
        }
    }

    suspend fun apply(
        prepared: LauncherBackupPreparationResult.Prepared,
        mode: WorkspaceImportMode,
        context: LauncherBackupRestoreContext,
        legacySourceSchemaVersion: Int? = null,
    ): LauncherBackupApplyResult = when (val plan = prepared.plan) {
        LauncherBackupRestorePlan.LeaveDestinationUntouched ->
            LauncherBackupApplyResult.LeftDestinationUntouched
        LauncherBackupRestorePlan.ExplicitlyEmptyWorkspace ->
            LauncherBackupApplyResult.ExplicitlyEmptyWorkspace
        is LauncherBackupRestorePlan.NormalizeLegacy -> {
            val schemaVersion = legacySourceSchemaVersion
                ?.takeIf { it > 0 }
                ?: return LauncherBackupApplyResult.MissingLegacySchemaVersion
            LauncherBackupApplyResult.LegacyNormalized(
                facade.normalizeLegacyTodos(plan.source, schemaVersion),
            )
        }
        is LauncherBackupRestorePlan.ImportWorkspace ->
            LauncherBackupApplyResult.WorkspaceImported(
                facade.importRichBackup(plan, mode, context),
            )
        LauncherBackupRestorePlan.InvalidRichBackup ->
            LauncherBackupApplyResult.RejectedInvalidRichBackup
        LauncherBackupRestorePlan.CorruptLegacyData ->
            LauncherBackupApplyResult.RejectedCorruptLegacyData
    }
}
