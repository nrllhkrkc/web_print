package com.akuple.web_print

import android.bluetooth.BluetoothAdapter
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
import com.tom_roush.pdfbox.pdmodel.PDDocument
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

            removePrinters(priorityList)

            val printers: MutableList<PrinterInfo> = ArrayList()
            val printerId = generatePrinterId("servis_cepte_yazdırma_servisi")
            val builder: PrinterInfo.Builder = PrinterInfo.Builder(printerId, "Servis Cepte Yazdırma Servisi", PrinterInfo.STATUS_IDLE)
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
        val fis = FileInputStream(document.data!!.fileDescriptor)
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            val device = adapter.bondedDevices.find { device -> device.address == WebPrintPlugin.printerAddress }
            val printerConnection = if (device != null) BluetoothConnection(device) else null

            val printer = AsyncEscPosPrinter(printerConnection, 203, 80f, 32)

            val doc = PDDocument.load(fis)
            val pdfRenderer = PDFRenderer(doc)
            var bitmap = pdfRenderer.renderImageWithDPI(0, 203f, Bitmap.Config.RGB_565)
            bitmap = trimBitmap(bitmap)
            val widthPx = printer.mmToPx(72f)
            bitmap = bitmapToBytes(bitmap, widthPx)

            doc.close()

            if (isCancelledJob) {
                return
            }

            printer.textToPrint = "<img>" + PrinterTextParserImg.bytesToHexadecimalString(bitmapToBytes(bitmap)) + "</img>";
            val asyncBluetoothEscPosPrint = AsyncBluetoothEscPosPrint(printJob).apply {
                setTopOffset(topOffset ?: 0)
            }
            val execute = asyncBluetoothEscPosPrint.execute(printer)
            if (isCancelledJob) execute.cancel(true)

        } catch (e: IOException) {
            Log.d("myprinter", "", e)
            printJob.fail("Bir hata oluştu.")
            return
        } finally {
            try {
                fis.close()
            } catch (e: IOException) {
                printJob.fail("Bir hata oluştu.")
                return
            }
        }
    }

    private fun bitmapToBytes(bitmap: Bitmap, printerWidthPx: Int): Bitmap {
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


        //TRIM WIDTH - LEFT
        var startWidth = 0
        for (x in 0 until imgWidth) {
            if (startWidth == 0) {
                for (y in 0 until imgHeight) {
                    if (bmp.getPixel(x, y) != Color.WHITE) {
                        startWidth = x
                        break
                    }
                }
            } else break
        }


        //TRIM WIDTH - RIGHT
        var endWidth = 0
        for (x in imgWidth - 1 downTo 0) {
            if (endWidth == 0) {
                for (y in 0 until imgHeight) {
                    if (bmp.getPixel(x, y) != Color.WHITE) {
                        endWidth = x
                        break
                    }
                }
            } else break
        }


        //TRIM HEIGHT - TOP
        var startHeight = 0
        for (y in 0 until imgHeight) {
            if (startHeight == 0) {
                for (x in 0 until imgWidth) {
                    if (bmp.getPixel(x, y) != Color.WHITE) {
                        startHeight = y
                        break
                    }
                }
            } else break
        }


        //TRIM HEIGHT - BOTTOM
        var endHeight = 0
        for (y in imgHeight - 1 downTo 0) {
            if (endHeight == 0) {
                for (x in 0 until imgWidth) {
                    if (bmp.getPixel(x, y) != Color.WHITE) {
                        endHeight = y
                        break
                    }
                }
            } else break
        }
        return Bitmap.createBitmap(
                bmp,
                startWidth,
                startHeight,
                endWidth - startWidth,
                endHeight - startHeight
        )
    }

}