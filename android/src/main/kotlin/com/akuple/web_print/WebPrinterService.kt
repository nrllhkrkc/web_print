package com.akuple.web_print

import android.bluetooth.BluetoothManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
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
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.ImageType
import com.tom_roush.pdfbox.rendering.PDFRenderer
import java.io.FileInputStream
import java.io.IOException
import kotlin.math.roundToInt


@RequiresApi(Build.VERSION_CODES.KITKAT)
class WebPrinterService : PrintService() {
    // Ana thread'de yazılıp hazırlık thread'inde okunduğu için volatile olmalı.
    @Volatile
    private var isCancelledJob = false;

    // PrintJob ve AsyncTask çağrıları ana thread'de yapılmak zorunda.
    private val mainHandler = Handler(Looper.getMainLooper())

    // Her raster bandının piksel yüksekliği. Sayfa yatay bantlara bölünüp ayrı ayrı
    // gönderilir, çünkü tek parça gönderim yazıcının tamponunu taşırıp bağlantıyı
    // düşürüyor.
    //
    // Belirleyici olan ortalama hız değil, anlık patlama (burst): bir bant tek write()
    // ile link hızında (~2 Mbps) boşalırken yazıcı onu 203 dpi / 50 mm/s ile çok daha
    // yavaş basar. Bant, yazıcının tamponundan küçük kalmalı.
    // 576 px genişlikte 32 satır = 2304 bayt; tipik 4 KB tamponun altında.
    private val bandHeightPx = 32

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
            capBuilder.addMediaSize(PrintAttributes.MediaSize("80", "80 mm", 2835, 10000), true)
            capBuilder.addResolution(Resolution("resolutionId", "default resolution", 203, 203), true)
            capBuilder.setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            capBuilder.setColorModes(PrintAttributes.COLOR_MODE_MONOCHROME, PrintAttributes.COLOR_MODE_MONOCHROME)
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
        // onPrintJobQueued ana (UI) thread'de çağrılır. PDF render + kırpma + raster
        // dönüşümü saniyeler sürdüğü için burada senkron yapılırsa ana thread kilitlenip
        // ANR'a yol açıyor. Hazırlık arka planda yapılır; PrintJob ve AsyncTask
        // işlemleri ana thread'de kalmak zorunda olduğu için sonuç oraya geri gönderilir.
        Thread {
            val fis = FileInputStream(fileDescriptor)
            try {
                val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
                val adapter = bluetoothManager.adapter
                val device = adapter.bondedDevices.find { device -> device.address == WebPrintPlugin.printerAddress }
                val printerConnection = if (device != null) BluetoothConnection(device) else null

                val printer = AsyncEscPosPrinter(printerConnection, 203, 80f, 32)

                val bands = ArrayList<ByteArray>()
                val doc = PDDocument.load(fis)
                try {
                    val pdfRenderer = PDFRenderer(doc)
                    val widthPx = printer.mmToPx(72f)

                    for (i in 0 until doc.numberOfPages) {
                        if (isCancelledJob) return@Thread

                        var bitmap = pdfRenderer.renderImageWithDPI(i, 203f, ImageType.RGB)
                        bitmap = trimVertically(bitmap) ?: bitmap
                        bitmap = bitmapToBtm(bitmap, widthPx)
                        bands.addAll(sliceToBands(bitmap, bandHeightPx))
                    }
                } finally {
                    doc.close()
                }

                if (isCancelledJob) return@Thread

                printer.imageBands = bands
                mainHandler.post {
                    if (isCancelledJob) return@post
                    AsyncBluetoothEscPosPrint(printJob)
                        .apply { setTopOffset(topOffset ?: 0) }
                        .execute(printer)
                }
            } catch (e: Exception) {
                Log.d("myprinter", "Print job failed", e)
                mainHandler.post { printJob.fail("Bir hata oluştu: ${e.message}") }
            } finally {
                try {
                    fis.close()
                } catch (e: IOException) {
                    Log.d("myprinter", "Failed to close file stream", e)
                }
            }
        }.start()
    }

    /**
     * Bitmap'i [bandHeight] piksel yüksekliğinde yatay bantlara böler ve her bandı
     * ayrı bir ESC/POS raster komutuna (GS v 0) çevirir.
     *
     * Bantlar arasına satır ilerletme komutu girmediği için çıktıda dikiş izi
     * oluşmaz: yazıcı her raster komutunu bir öncekinin bittiği noktadan bitişik
     * olarak basar.
     */
    private fun sliceToBands(bitmap: Bitmap, bandHeight: Int): List<ByteArray> {
        val bands = ArrayList<ByteArray>()
        var y = 0
        while (y < bitmap.height) {
            val height = minOf(bandHeight, bitmap.height - y)
            val band = Bitmap.createBitmap(bitmap, 0, y, bitmap.width, height)
            bands.add(bitmapToBytes(band, false))
            y += height
        }
        return bands
    }

    private fun bitmapToBtm(bitmap: Bitmap, printerWidthPx: Int): Bitmap {
        var bitmapWidth = bitmap.width
        var bitmapHeight = bitmap.height
        val maxWidth: Int = printerWidthPx

        bitmapHeight = (bitmapHeight.toFloat() * maxWidth.toFloat() / bitmapWidth.toFloat()).roundToInt()
        bitmapWidth = maxWidth

        return Bitmap.createScaledBitmap(bitmap, bitmapWidth, bitmapHeight, false);
    }

    /**
     * Bitmap'in yalnızca üstündeki ve altındaki tamamen beyaz satırları atar; genişliğe
     * dokunmaz. Tamamen beyaz bitmap'te null döner.
     *
     * Yatayda kırpma yapılmamasının nedeni: ölçek katsayısı [bitmapToBtm] içinde
     * genişlikten hesaplanıyor. Her sayfa kendi içerik kutusuna kırpılırsa katsayı
     * sayfadan sayfaya değişir ve içeriği dar olan sayfa (ör. imza blokları) diğerinden
     * daha büyük puntoyla basılır. Genişlik sabit kaldığı sürece tüm sayfalar aynı
     * oranla basılır ve PDF'in kenar boşlukları da olduğu gibi korunur.
     *
     * Piksel başına bir JNI çağrısı yapan getPixel() yerine satır satır getPixels()
     * kullanılır: bir A4 sayfası için milyonlarca yerine yalnızca satır sayısı kadar
     * (~2400) yerel çağrı yapılır.
     */
    private fun trimVertically(bmp: Bitmap): Bitmap? {
        val imgWidth = bmp.width
        val imgHeight = bmp.height
        val row = IntArray(imgWidth)

        var startHeight = -1
        var endHeight = -1

        for (y in 0 until imgHeight) {
            bmp.getPixels(row, 0, imgWidth, 0, y, imgWidth, 1)

            // Satır tamamen beyaz
            if (row.all { it == Color.WHITE }) continue

            if (startHeight == -1) startHeight = y
            endHeight = y
        }

        // Bitmap tamamen beyazsa kırpma yapma
        if (startHeight == -1) return null

        return Bitmap.createBitmap(bmp, 0, startHeight, imgWidth, endHeight - startHeight + 1)
    }

}
