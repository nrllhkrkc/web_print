package com.akuple.web_print

import android.bluetooth.BluetoothManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintAttributes.Resolution
import android.print.PrinterCapabilitiesInfo
import android.print.PrinterId
import android.print.PrinterInfo
import android.printservice.PrintJob
import android.printservice.PrintService
import android.printservice.PrinterDiscoverySession
import android.util.Log
import androidx.annotation.RequiresApi
import com.akuple.web_print.async.AsyncBluetoothEscPosPrint
import com.akuple.web_print.async.AsyncEscPosPrinter
import com.dantsu.escposprinter.EscPosPrinterCommands.bitmapToBytes
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.ImageType
import com.tom_roush.pdfbox.rendering.PDFRenderer
import java.io.FileInputStream
import java.io.IOException
import kotlin.math.roundToInt


@RequiresApi(Build.VERSION_CODES.KITKAT)
class WebPrinterService : PrintService() {
    private var isCancelledJob = false;

    override fun onCreatePrinterDiscoverySession(): PrinterDiscoverySession = object : PrinterDiscoverySession() {
        override fun onStartPrinterDiscovery(priorityList: MutableList<PrinterId>) {
//            if (priorityList.isNotEmpty()) {
//                return
//            }

            PDFBoxResourceLoader.init(applicationContext)

            removePrinters(priorityList)

            val printers: MutableList<PrinterInfo> = ArrayList()
            val printerId = generatePrinterId("servis_cepte_yazdırma_servisi")
            val builder: PrinterInfo.Builder =
                PrinterInfo.Builder(printerId, "Servis Cepte Yazdırma Servisi", PrinterInfo.STATUS_IDLE)
            val capBuilder = PrinterCapabilitiesInfo.Builder(printerId)
            capBuilder.addMediaSize(PrintAttributes.MediaSize("80", "80 mm", 4150, 10000), true)
            capBuilder.addResolution(Resolution("resolutionId", "default resolution", 203, 203), true)
            capBuilder.setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            capBuilder.setColorModes(PrintAttributes.COLOR_MODE_MONOCHROME, PrintAttributes.COLOR_MODE_COLOR)
            builder.setCapabilities(capBuilder.build())
            printers.add(builder.build())

            addPrinters(printers)
        }

        override fun onStopPrinterDiscovery() {
        }

        override fun onValidatePrinters(printerIds: MutableList<PrinterId>) {
        }

        override fun onStartPrinterStateTracking(printerId: PrinterId) {
        }

        override fun onStopPrinterStateTracking(printerId: PrinterId) {
        }

        override fun onDestroy() {
        }
    }

    override fun onRequestCancelPrintJob(printJob: PrintJob?) {
        Log.d("myprinter", "canceled: " + printJob?.getId()?.toString());
        isCancelledJob = true
        printJob?.cancel()
    }

    override fun onPrintJobQueued(printJob: PrintJob?) {
        if (printJob == null) return

        if (WebPrintPlugin.printerAddress == null) {
            printJob.fail("Yazıcı seçilmemiş.Uygulamanın ayarlar bölümünden yazıcı seçebilirsiniz.")
            return
        }

        val topOffset = WebPrintPlugin.topOffset;

        printJob.start()
        isCancelledJob = false

        val document = printJob.document
        val fileDescriptor = document.data?.fileDescriptor
        if (fileDescriptor == null) {
            printJob.fail("Yazdırılacak belge verisi alınamadı.")
            return
        }
        val fis = FileInputStream(fileDescriptor)
        try {
            val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter
            val device = adapter.bondedDevices.find { device -> device.address == WebPrintPlugin.printerAddress }
            val printerConnection = if (device != null) BluetoothConnection(device) else null

            val printer = AsyncEscPosPrinter(printerConnection, 203, 80f, 32)

            val doc = PDDocument.load(fis)
            val pdfRenderer = PDFRenderer(doc)
            val pageCount = doc.numberOfPages
            val widthPx = printer.mmToPx(72f)

            val imgParts = StringBuilder()
            for (i in 0 until pageCount) {
                if (isCancelledJob) {
                    doc.close()
                    return
                }
                var bitmap = pdfRenderer.renderImageWithDPI(i, 203f, ImageType.RGB)
                bitmap = trimBitmap(bitmap) ?: bitmap
                bitmap = bitmapToBtm(bitmap, widthPx)
                imgParts.append("<img>")
                imgParts.append(PrinterTextParserImg.bytesToHexadecimalString(bitmapToBytes(bitmap, false)))
                imgParts.append("</img>\n")
            }

            doc.close()

            if (isCancelledJob) {
                return
            }

            printer.textToPrint = imgParts.toString()
            val asyncBluetoothEscPosPrint = AsyncBluetoothEscPosPrint(printJob).apply {
                setTopOffset(topOffset ?: 0)
            }
            val execute = asyncBluetoothEscPosPrint.execute(printer)
            if (isCancelledJob) execute.cancel(true)

        } catch (e: Exception) {
            Log.d("myprinter", "Print job failed", e)
            printJob.fail("Bir hata oluştu: ${e.message}")
            return
        } finally {
            try {
                fis.close()
            } catch (e: IOException) {
                Log.d("myprinter", "Failed to close file stream", e)
            }
        }
    }

    private fun bitmapToBtm(bitmap: Bitmap, printerWidthPx: Int): Bitmap {
        var bitmapWidth = bitmap.width
        var bitmapHeight = bitmap.height
        val maxWidth: Int = printerWidthPx

        bitmapHeight = (bitmapHeight.toFloat() * maxWidth.toFloat() / bitmapWidth.toFloat()).roundToInt()
        bitmapWidth = maxWidth

        return Bitmap.createScaledBitmap(bitmap, bitmapWidth, bitmapHeight, false);
    }

    private fun trimBitmap(bmp: Bitmap): Bitmap? {
        val imgHeight = bmp.height
        val imgWidth = bmp.width

        var startWidth = -1
        var endWidth = -1
        var startHeight = -1
        var endHeight = -1

        //TRIM WIDTH - LEFT
        for (x in 0 until imgWidth) {
            for (y in 0 until imgHeight) {
                if (bmp.getPixel(x, y) != Color.WHITE) {
                    startWidth = x
                    break
                }
            }
            if (startWidth != -1) break
        }

        // Bitmap tamamen beyazsa kırpma yapma
        if (startWidth == -1) return null

        //TRIM WIDTH - RIGHT
        for (x in imgWidth - 1 downTo 0) {
            for (y in 0 until imgHeight) {
                if (bmp.getPixel(x, y) != Color.WHITE) {
                    endWidth = x
                    break
                }
            }
            if (endWidth != -1) break
        }

        //TRIM HEIGHT - TOP
        for (y in 0 until imgHeight) {
            for (x in 0 until imgWidth) {
                if (bmp.getPixel(x, y) != Color.WHITE) {
                    startHeight = y
                    break
                }
            }
            if (startHeight != -1) break
        }

        //TRIM HEIGHT - BOTTOM
        for (y in imgHeight - 1 downTo 0) {
            for (x in 0 until imgWidth) {
                if (bmp.getPixel(x, y) != Color.WHITE) {
                    endHeight = y
                    break
                }
            }
            if (endHeight != -1) break
        }

        val width = endWidth - startWidth + 1
        val height = endHeight - startHeight + 1

        if (width <= 0 || height <= 0) return null

        return Bitmap.createBitmap(
            bmp,
            startWidth,
            startHeight,
            width,
            height
        )
    }

}
