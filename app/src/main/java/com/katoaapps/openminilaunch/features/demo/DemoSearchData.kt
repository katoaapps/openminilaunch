package com.katoaapps.openminilaunch.features.demo

import com.katoaapps.openminilaunch.model.ContactResult
import com.katoaapps.openminilaunch.model.FileSearchResult

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/** Search-only sample data used for screenshots and demonstrations. */
internal object DemoSearchData {
    private val seedNamesByLetter = listOf(
        listOf("Aaliyah", "Aaron", "Amara", "Avery"),
        listOf("Bianca", "Blake", "Bruno"),
        listOf("Camila", "Caleb", "Carmen", "Cyrus"),
        listOf("Dahlia", "Damon", "Diana"),
        listOf("Elena", "Elias", "Emery", "Example"),
        listOf("Fatima", "Felix", "Freya"),
        listOf("Gabriela", "George", "Gideon"),
        listOf("Hana", "Harper", "Hector"),
        listOf("Imani", "Isaac"),
        listOf("Jada", "Jasper", "June", "Julian"),
        listOf("Kai", "Kara", "Keira"),
        listOf("Laila", "Leo", "Lucia"),
        listOf("Maya", "Mateo", "Mina", "Marcus"),
        listOf("Nadia", "Nico", "Noelle"),
        listOf("Omar", "Opal"),
        listOf("Paola", "Parker", "Priya"),
        listOf("Quentin", "Quiana"),
        listOf("Rafael", "Raina", "Rowan", "Ruby"),
        listOf("Sabrina", "Samir", "Sofia", "Silas"),
        listOf("Talia", "Theo", "Tristan", "Tessa"),
        listOf("Uma", "Uri"),
        listOf("Valeria", "Victor"),
        listOf("Wade", "Willa", "Wyatt"),
        listOf("Xander", "Xiomara"),
        listOf("Yara", "Yusuf"),
        listOf("Zahra", "Zoe"),
    )
    private val lastNames = listOf("Adams", "Bennett", "Chen", "Diaz", "Ellis", "Flores")
    private val phoneLabels = listOf("Mobile", "Work", "Home")

    private val invoices = listOf(
        DemoInvoice("Northstar_invoice_2026-08-01.pdf", "Northstar Office Supply", "INV-1048", "\$284.60"),
        DemoInvoice("Cedar_Grove_invoice_2026-07-18.pdf", "Cedar Grove Studio", "INV-1031", "\$1,240.00"),
        DemoInvoice("Harborline_invoice_2026-06-30.pdf", "Harborline Internet", "INV-998", "\$89.00"),
    )

    fun searchContacts(query: String): List<ContactResult> {
        val clean = query.trim()
        if (clean.isEmpty()) return emptyList()
        val letterIndex = clean.first().lowercaseChar() - 'a'
        val namesForLetter = seedNamesByLetter.getOrNull(letterIndex).orEmpty()
        val contactIdOffset = seedNamesByLetter.take(letterIndex.coerceAtLeast(0)).sumOf(List<String>::size)
        return namesForLetter.filter { it.startsWith(clean, ignoreCase = true) }.mapIndexed { index, firstName ->
            val id = contactIdOffset + namesForLetter.indexOf(firstName)
            ContactResult(
                contactUri = "content://com.katoaapps.openminilaunch.demo/contacts/$id",
                name = if (firstName == "Example") {
                    "Example Contact"
                } else {
                    "$firstName ${lastNames[id % lastNames.size]}"
                },
                phone = "+1 202-555-${(100 + id).toString().padStart(4, '0')}",
                phoneLabel = phoneLabels[index % phoneLabels.size],
            )
        }
    }

    fun searchFiles(context: Context, query: String): List<FileSearchResult> {
        val clean = query.trim()
        if (clean.length < 2) return emptyList()
        return invoices.asSequence()
            .filter { it.fileName.contains(clean, ignoreCase = true) }
            .mapIndexedNotNull { index, invoice ->
                runCatching {
                    val file = ensureInvoicePdf(context, invoice)
                    FileSearchResult(
                        name = invoice.fileName,
                        uri = FileProvider.getUriForFile(context, "${context.packageName}.demo-files", file),
                        mimeType = "application/pdf",
                        modifiedAt = DEMO_MODIFIED_AT - index,
                    )
                }.getOrNull()
            }
            .toList()
    }

    fun clearFiles(context: Context) {
        runCatching { File(context.cacheDir, DEMO_DIRECTORY).deleteRecursively() }
    }

    @Synchronized
    private fun ensureInvoicePdf(context: Context, invoice: DemoInvoice): File {
        val directory = File(context.cacheDir, DEMO_DIRECTORY).apply { mkdirs() }
        val output = File(directory, invoice.fileName)
        if (output.length() > 0L) return output

        val document = PdfDocument()
        try {
            val page = document.startPage(PdfDocument.PageInfo.Builder(612, 792, 1).create())
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 30f; isFakeBoldText = true }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 16f }
            page.canvas.apply {
                drawText("INVOICE", 54f, 80f, titlePaint)
                drawText(invoice.company, 54f, 124f, bodyPaint)
                drawText("Invoice number: ${invoice.number}", 54f, 180f, bodyPaint)
                drawText("Demo services and materials", 54f, 236f, bodyPaint)
                drawText("Total: ${invoice.total}", 54f, 292f, titlePaint)
                drawText("Sample document generated by MinkLauncher OpenSource", 54f, 728f, bodyPaint)
            }
            document.finishPage(page)
            FileOutputStream(output).use(document::writeTo)
        } finally {
            document.close()
        }
        return output
    }

    private data class DemoInvoice(
        val fileName: String,
        val company: String,
        val number: String,
        val total: String,
    )

    private const val DEMO_DIRECTORY = "demo-search-data"
    private const val DEMO_MODIFIED_AT = 1_787_000_000_000L
}
