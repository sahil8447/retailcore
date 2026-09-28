package com.sahil.retailcore.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.sahil.retailcore.data.local.entity.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object InvoiceGenerator {

    fun generateSaleInvoice(
        context: Context,
        shopName: String,
        shopAddress: String,
        shopGst: String,
        sale: SaleEntity,
        customer: CustomerEntity?,
        items: List<Pair<SaleItemEntity, ProductEntity>>
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
        }
        val headerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
        }
        val textPaint = Paint().apply {
            typeface = Typeface.DEFAULT
            textSize = 10f
        }

        var y = 40f
        
        // Shop Details
        canvas.drawText(shopName, 40f, y, titlePaint)
        y += 20f
        canvas.drawText(shopAddress, 40f, y, textPaint)
        y += 15f
        canvas.drawText("GSTIN: $shopGst", 40f, y, textPaint)
        y += 30f

        canvas.drawText("TAX INVOICE", 250f, y, titlePaint)
        y += 30f

        // Invoice Info
        canvas.drawText("Invoice No: ${sale.invoiceNumber}", 40f, y, headerPaint)
        canvas.drawText("Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(sale.createdAt))}", 400f, y, headerPaint)
        y += 20f

        // Customer Details
        canvas.drawText("Bill To:", 40f, y, headerPaint)
        y += 15f
        canvas.drawText("Name: ${customer?.name ?: "Cash Customer"}", 40f, y, textPaint)
        y += 15f
        canvas.drawText("Phone: ${customer?.phone ?: ""}", 40f, y, textPaint)
        y += 15f
        canvas.drawText("Address: ${customer?.address ?: ""}", 40f, y, textPaint)
        y += 30f

        // Table Header
        val xCols = floatArrayOf(40f, 80f, 300f, 350f, 420f, 480f)
        canvas.drawText("S.No", xCols[0], y, headerPaint)
        canvas.drawText("Product Description", xCols[1], y, headerPaint)
        canvas.drawText("Qty", xCols[2], y, headerPaint)
        canvas.drawText("Rate", xCols[3], y, headerPaint)
        canvas.drawText("GST%", xCols[4], y, headerPaint)
        canvas.drawText("Total", xCols[5], y, headerPaint)
        y += 10f
        canvas.drawLine(40f, y, 550f, y, paint)
        y += 20f

        // Items
        items.forEachIndexed { index, pair ->
            val item = pair.first
            val product = pair.second
            canvas.drawText("${index + 1}", xCols[0], y, textPaint)
            canvas.drawText("${product.name} ${product.modelNumber}", xCols[1], y, textPaint)
            canvas.drawText("${item.quantity}", xCols[2], y, textPaint)
            canvas.drawText("${item.unitPrice}", xCols[3], y, textPaint)
            canvas.drawText("${item.gstPercent}%", xCols[4], y, textPaint) 
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", item.subtotal), xCols[5], y, textPaint)
            y += 20f
        }

        y += 10f
        canvas.drawLine(40f, y, 550f, y, paint)
        y += 30f

        // Totals
        canvas.drawText("Sub Total:", 400f, y, headerPaint)
        canvas.drawText("₹${String.format(Locale.getDefault(), "%.2f", sale.totalAmount - sale.tax + sale.discount)}", 480f, y, textPaint)
        y += 20f
        canvas.drawText("Tax Amount:", 400f, y, headerPaint)
        canvas.drawText("₹${String.format(Locale.getDefault(), "%.2f", sale.tax)}", 480f, y, textPaint)
        y += 20f
        canvas.drawText("Discount:", 400f, y, headerPaint)
        canvas.drawText("₹${String.format(Locale.getDefault(), "%.2f", sale.discount)}", 480f, y, textPaint)
        y += 20f
        canvas.drawText("Grand Total:", 400f, y, titlePaint)
        canvas.drawText("₹${String.format(Locale.getDefault(), "%.2f", sale.totalAmount)}", 480f, y, titlePaint)
        y += 40f

        canvas.drawText("Payment Method: ${sale.paymentMethod}", 40f, y, textPaint)
        y += 15f
        canvas.drawText("Status: ${sale.status}", 40f, y, textPaint)

        pdfDocument.finishPage(page)

        val file = File(context.getExternalFilesDir(null), "Invoices/Sale_${sale.invoiceNumber}.pdf")
        file.parentFile?.mkdirs()
        
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }

    fun generatePurchaseInvoice(
        context: Context,
        shopName: String,
        shopAddress: String,
        shopGst: String,
        purchase: PurchaseEntity,
        supplier: SupplierEntity?,
        items: List<Pair<PurchaseItemEntity, ProductEntity>>
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
        }
        val headerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
        }
        val textPaint = Paint().apply {
            typeface = Typeface.DEFAULT
            textSize = 10f
        }

        var y = 40f
        
        // Shop Details
        canvas.drawText(shopName, 40f, y, titlePaint)
        y += 20f
        canvas.drawText(shopAddress, 40f, y, textPaint)
        y += 15f
        canvas.drawText("GSTIN: $shopGst", 40f, y, textPaint)
        y += 30f

        canvas.drawText("PURCHASE VOUCHER", 220f, y, titlePaint)
        y += 30f

        // Invoice Info
        canvas.drawText("Invoice No: ${purchase.invoiceNumber}", 40f, y, headerPaint)
        canvas.drawText("Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(purchase.invoiceDate))}", 400f, y, headerPaint)
        y += 20f

        // Supplier Details
        canvas.drawText("Supplier Details:", 40f, y, headerPaint)
        y += 15f
        canvas.drawText("Name: ${supplier?.name ?: "Unknown"}", 40f, y, textPaint)
        y += 15f
        canvas.drawText("Phone: ${supplier?.phone ?: ""}", 40f, y, textPaint)
        y += 15f
        canvas.drawText("Address: ${supplier?.address ?: ""}", 40f, y, textPaint)
        y += 15f
        canvas.drawText("GST: ${supplier?.gstNumber ?: ""}", 40f, y, textPaint)
        y += 30f

        // Table Header
        val xCols = floatArrayOf(40f, 80f, 300f, 350f, 420f, 480f)
        canvas.drawText("S.No", xCols[0], y, headerPaint)
        canvas.drawText("Product Description", xCols[1], y, headerPaint)
        canvas.drawText("Qty", xCols[2], y, headerPaint)
        canvas.drawText("Rate", xCols[3], y, headerPaint)
        canvas.drawText("GST%", xCols[4], y, headerPaint)
        canvas.drawText("Total", xCols[5], y, headerPaint)
        y += 10f
        canvas.drawLine(40f, y, 550f, y, paint)
        y += 20f

        // Items
        items.forEachIndexed { index, pair ->
            val item = pair.first
            val product = pair.second
            canvas.drawText("${index + 1}", xCols[0], y, textPaint)
            canvas.drawText("${product.name} ${product.modelNumber}", xCols[1], y, textPaint)
            canvas.drawText("${item.quantity}", xCols[2], y, textPaint)
            canvas.drawText("${item.purchasePriceWithoutGST}", xCols[3], y, textPaint)
            canvas.drawText("${item.gstPercent}%", xCols[4], y, textPaint)
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", item.lineTotal), xCols[5], y, textPaint)
            y += 20f
        }

        y += 10f
        canvas.drawLine(40f, y, 550f, y, paint)
        y += 30f

        // Totals
        canvas.drawText("Sub Total:", 400f, y, headerPaint)
        canvas.drawText("₹${String.format(Locale.getDefault(), "%.2f", purchase.subtotal)}", 480f, y, textPaint)
        y += 20f
        canvas.drawText("Tax Amount:", 400f, y, headerPaint)
        canvas.drawText("₹${String.format(Locale.getDefault(), "%.2f", purchase.totalGST)}", 480f, y, textPaint)
        y += 20f
        canvas.drawText("Grand Total:", 400f, y, titlePaint)
        canvas.drawText("₹${String.format(Locale.getDefault(), "%.2f", purchase.grandTotal)}", 480f, y, titlePaint)
        y += 40f

        canvas.drawText("Payment Status: ${purchase.paymentStatus}", 40f, y, textPaint)

        pdfDocument.finishPage(page)

        val file = File(context.getExternalFilesDir(null), "Invoices/Purchase_${purchase.invoiceNumber}.pdf")
        file.parentFile?.mkdirs()
        
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }
}
