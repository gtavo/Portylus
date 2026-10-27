package com.gtavoapps.portylus

import android.content.Context
import java.io.File

object SearchFixture {
    fun create(context: Context, text: Boolean = true): File {
        val content = if (text) "BT /F1 14 Tf 30 300 Td (PDF pdfizador PDF2 PDF_pdf pdf.) Tj " +
            "0 -25 Td (Primera l\\355nea: caf\\351 CAF\\311.) Tj 0 -25 Td (Literal [x].) Tj ET\n" else "0.8 g 30 200 200 70 re f\n"
        val blank = "0.8 g 30 200 200 70 re f\n"
        val objects = listOf(
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R 6 0 R 7 0 R] /Count 3 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 400] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>",
            "<< /Length ${content.length} >>\nstream\n${content}endstream",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 400] /CropBox [10 10 290 390] /Rotate 90 /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 400] /Contents 8 0 R >>",
            "<< /Length ${blank.length} >>\nstream\n${blank}endstream")
        val pdf = StringBuilder("%PDF-1.7\n")
        val offsets = objects.mapIndexed { i, obj -> pdf.length.also { pdf.append("${i+1} 0 obj\n$obj\nendobj\n") } }
        val xref = pdf.length
        pdf.append("xref\n0 9\n0000000000 65535 f \n")
        offsets.forEach { pdf.append(it.toString().padStart(10,'0')).append(" 00000 n \n") }
        pdf.append("trailer\n<< /Size 9 /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return File.createTempFile("search-fixture-", ".pdf", context.filesDir).apply {
            writeBytes(pdf.toString().toByteArray(Charsets.US_ASCII))
        }
    }
}
