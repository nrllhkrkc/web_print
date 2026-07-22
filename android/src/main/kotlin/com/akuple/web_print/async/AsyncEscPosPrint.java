package com.akuple.web_print.async;

import android.content.Context;
import android.os.AsyncTask;

import com.akuple.web_print.WebPrintPlugin;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;

public abstract class AsyncEscPosPrint extends AsyncTask<AsyncEscPosPrinter, Integer, Integer> {
    protected final static int FINISH_SUCCESS = 1;
    protected final static int FINISH_NO_PRINTER = 2;
    protected final static int FINISH_PRINTER_DISCONNECTED = 3;
    protected final static int FINISH_PARSER_ERROR = 4;
    protected final static int FINISH_ENCODING_ERROR = 5;
    protected final static int FINISH_BARCODE_ERROR = 6;

    protected final static int PROGRESS_CONNECTING = 1;
    protected final static int PROGRESS_CONNECTED = 2;
    protected final static int PROGRESS_PRINTING = 3;
    protected final static int PROGRESS_PRINTED = 4;

    /**
     * Her raster bandı gönderildikten sonra beklenecek süre (ms). Yazıcının iç
     * tamponunun boşalmasına izin verir. Baskı hâlâ yarıda kesiliyorsa artırın;
     * bantlar arasında koyuluk farkı görürseniz azaltın.
     */
    private final static int BAND_DELAY_MS = 50;

    /** Bir bant gönderilemezse kaç kez yeniden bağlanıp denenecek. */
    private final static int MAX_BAND_ATTEMPTS = 3;

    /** Yeniden bağlanmadan önce yazıcının toparlanması için beklenecek süre (ms). */
    private final static int RECONNECT_DELAY_MS = 600;

    private int topOffset = 0;

    public void setTopOffset(int topOffset) {
        this.topOffset = topOffset;
    }

    //    protected ProgressDialog dialog;
//    protected WeakReference<Context> weakContext;


    public AsyncEscPosPrint(Context context) {
//        this.weakContext = new WeakReference<>(context);
    }

    protected Integer doInBackground(AsyncEscPosPrinter... printersData) {
        if (printersData.length == 0) {
            return AsyncEscPosPrint.FINISH_NO_PRINTER;
        }

        this.publishProgress(AsyncEscPosPrint.PROGRESS_CONNECTING);

        AsyncEscPosPrinter printerData = printersData[0];

        try {
            DeviceConnection deviceConnection = printerData.getPrinterConnection();

            if (deviceConnection == null) {
                return AsyncEscPosPrint.FINISH_NO_PRINTER;
            }

            CustomEscPosPrinterCommands escPosPrinterCommands = new CustomEscPosPrinterCommands(deviceConnection);

            EscPosPrinter printer = new EscPosPrinter(
                    escPosPrinterCommands,
                    printerData.getPrinterDpi(),
                    printerData.getPrinterWidthMM(),
                    printerData.getPrinterNbrCharactersPerLine()
            );

            this.publishProgress(AsyncEscPosPrint.PROGRESS_PRINTING);

            escPosPrinterCommands.reset();
            escPosPrinterCommands.feedByMM(topOffset);
            escPosPrinterCommands.printText("");
            escPosPrinterCommands.send();

            java.util.List<byte[]> bands = printerData.getImageBands();
            if (bands != null && !bands.isEmpty()) {
                // Bantları tek tek gönder. printImage() yalnızca raster komutunu yazıp
                // tamponu boşaltır, satır ilerletme eklemez; bu yüzden bantlar çıktıda
                // dikişsiz birleşir. Aradaki bekleme, yazıcının tamponunun boşalmasına
                // izin vererek taşma sonucu oluşan yarıda kesilme/kopmayı önler.
                android.util.Log.d("myprinter", "Sending " + bands.size() + " bands");
                for (int i = 0; i < bands.size(); i++) {
                    if (this.isCancelled()) {
                        android.util.Log.d("myprinter", "Cancelled at band " + i);
                        break;
                    }
                    sendBand(escPosPrinterCommands, deviceConnection, bands.get(i), i, bands.size());
                    try {
                        Thread.sleep(BAND_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                android.util.Log.d("myprinter", "All bands sent");
            } else {
                printer.printFormattedText(printerData.getTextToPrint(), 0);
                escPosPrinterCommands.send();
            }

            escPosPrinterCommands.feedPage();
            escPosPrinterCommands.printText("");
            escPosPrinterCommands.send();

            this.publishProgress(AsyncEscPosPrint.PROGRESS_PRINTED);

        } catch (EscPosConnectionException e) {
            android.util.Log.e("myprinter", "Printer disconnected mid-print", e);
            return AsyncEscPosPrint.FINISH_PRINTER_DISCONNECTED;
        } catch (EscPosParserException e) {
            e.printStackTrace();
            return AsyncEscPosPrint.FINISH_PARSER_ERROR;
        } catch (EscPosEncodingException e) {
            e.printStackTrace();
            return AsyncEscPosPrint.FINISH_ENCODING_ERROR;
        } catch (EscPosBarcodeException e) {
            e.printStackTrace();
            return AsyncEscPosPrint.FINISH_BARCODE_ERROR;
        }

        return AsyncEscPosPrint.FINISH_SUCCESS;
    }

    /**
     * Tek bir raster bandını gönderir; bağlantı koparsa yeniden bağlanıp tekrar dener.
     *
     * Not: printImage() bağlantı kopmuşsa exception atmaz, sessizce hiçbir şey yapmadan
     * döner. Bu yüzden göndermeden önce isConnected() açıkça kontrol edilir; aksi hâlde
     * bant kaybolur ve baskı sessizce eksik çıkar.
     */
    private void sendBand(
            CustomEscPosPrinterCommands commands,
            DeviceConnection connection,
            byte[] band,
            int index,
            int total
    ) throws EscPosConnectionException {
        EscPosConnectionException lastError = null;

        for (int attempt = 1; attempt <= MAX_BAND_ATTEMPTS; attempt++) {
            if (!connection.isConnected()) {
                reconnect(connection);
                if (!connection.isConnected()) {
                    lastError = new EscPosConnectionException("Yazıcı bağlantısı kurulamadı.");
                    continue;
                }
            }

            try {
                commands.printImage(band);
                android.util.Log.d("myprinter", "Sent band " + (index + 1) + "/" + total
                        + " (" + band.length + " bytes)"
                        + (attempt > 1 ? " after " + attempt + " attempts" : ""));
                return;
            } catch (EscPosConnectionException e) {
                lastError = e;
                android.util.Log.w("myprinter", "Band " + (index + 1) + "/" + total
                        + " failed on attempt " + attempt + ", reconnecting", e);
                reconnect(connection);
            }
        }

        throw lastError != null
                ? lastError
                : new EscPosConnectionException("Bant gönderilemedi: " + (index + 1) + "/" + total);
    }

    /** Bağlantıyı kapatıp yeniden açar. Başarısız olursa isConnected() false kalır. */
    private void reconnect(DeviceConnection connection) {
        try {
            connection.disconnect();
        } catch (Exception ignored) {
            // Zaten kopmuş olabilir, önemsiz.
        }

        try {
            Thread.sleep(RECONNECT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        try {
            connection.connect();
            android.util.Log.d("myprinter", "Reconnected to printer");
        } catch (Exception e) {
            android.util.Log.w("myprinter", "Reconnect failed", e);
        }
    }

    protected void onPreExecute() {
//        if (this.dialog == null) {
//            Context context = weakContext.get();
//
//            if (context == null) {
//                return;
//            }
//
//            this.dialog = new ProgressDialog(context);
//            this.dialog.setTitle("Printing in progress...");
//            this.dialog.setMessage("...");
//            this.dialog.setProgressNumberFormat("%1d / %2d");
//            this.dialog.setCancelable(false);
//            this.dialog.setIndeterminate(false);
//            this.dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//            this.dialog.show();
//        }
    }

    protected void onProgressUpdate(Integer... progress) {
//        switch (progress[0]) {
//            case AsyncEscPosPrint.PROGRESS_CONNECTING:
//                this.dialog.setMessage("Connecting printer...");
//                break;
//            case AsyncEscPosPrint.PROGRESS_CONNECTED:
//                this.dialog.setMessage("Printer is connected...");
//                break;
//            case AsyncEscPosPrint.PROGRESS_PRINTING:
//                this.dialog.setMessage("Printer is printing...");
//                break;
//            case AsyncEscPosPrint.PROGRESS_PRINTED:
//                this.dialog.setMessage("Printer has finished...");
//                break;
//        }
//        this.dialog.setProgress(progress[0]);
//        this.dialog.setMax(4);
    }

    protected void onPostExecute(Integer result) {
//        this.dialog.dismiss();
//        this.dialog = null;
//
//        Context context = weakContext.get();
//
//        if (context == null) {
//            return;
//        }
//
//        switch (result) {
//            case AsyncEscPosPrint.FINISH_SUCCESS:
//                new AlertDialog.Builder(context)
//                        .setTitle("Success")
//                        .setMessage("Congratulation ! The text is printed !")
//                        .show();
//                break;
//            case AsyncEscPosPrint.FINISH_NO_PRINTER:
//                new AlertDialog.Builder(context)
//                        .setTitle("No printer")
//                        .setMessage("The application can't find any printer connected.")
//                        .show();
//                break;
//            case AsyncEscPosPrint.FINISH_PRINTER_DISCONNECTED:
//                new AlertDialog.Builder(context)
//                    .setTitle("Broken connection")
//                    .setMessage("Unable to connect the printer.")
//                    .show();
//                break;
//            case AsyncEscPosPrint.FINISH_PARSER_ERROR:
//                new AlertDialog.Builder(context)
//                    .setTitle("Invalid formatted text")
//                    .setMessage("It seems to be an invalid syntax problem.")
//                    .show();
//                break;
//            case AsyncEscPosPrint.FINISH_ENCODING_ERROR:
//                new AlertDialog.Builder(context)
//                    .setTitle("Bad selected encoding")
//                    .setMessage("The selected encoding character returning an error.")
//                    .show();
//                break;
//            case AsyncEscPosPrint.FINISH_BARCODE_ERROR:
//                new AlertDialog.Builder(context)
//                    .setTitle("Invalid barcode")
//                    .setMessage("Data send to be converted to barcode or QR code seems to be invalid.")
//                    .show();
//                break;
//        }
    }
}
