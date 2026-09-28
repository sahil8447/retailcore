package com.sahil.retailcore.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object InvoiceUtils {

    fun shareInvoice(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Invoice"))
    }

    fun printInvoice(context: Context, file: File) {
        // For printing, we can use Android PrintManager
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
        val jobName = "${context.getString(com.sahil.retailcore.R.string.app_name)} Invoice"
        
        val printAdapter = object : android.print.PrintDocumentAdapter() {
            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: android.os.ParcelFileDescriptor?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    val input = java.io.FileInputStream(file)
                    val output = java.io.FileOutputStream(destination?.fileDescriptor)
                    input.copyTo(output)
                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }

            override fun onLayout(
                oldAttributes: android.print.PrintAttributes?,
                newAttributes: android.print.PrintAttributes?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: android.os.Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val pdi = android.print.PrintDocumentInfo.Builder(file.name)
                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback?.onLayoutFinished(pdi, true)
            }
        }
        
        printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
    }
}
