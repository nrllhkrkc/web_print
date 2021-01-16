package com.akuple.web_print.async;

import android.os.Build;
import android.printservice.PrintJob;

import androidx.annotation.RequiresApi;

import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;

public class AsyncBluetoothEscPosPrint extends AsyncEscPosPrint {
    private PrintJob printJob;

    public AsyncBluetoothEscPosPrint(PrintJob printJob) {
        super(null);
        this.printJob = printJob;
    }

    protected Integer doInBackground(AsyncEscPosPrinter... printersData) {
        if (printersData.length == 0) {
            return AsyncEscPosPrint.FINISH_NO_PRINTER;
        }

        AsyncEscPosPrinter printerData = printersData[0];

        DeviceConnection deviceConnection = printerData.getPrinterConnection();

        if (deviceConnection == null) {
            this.publishProgress(AsyncEscPosPrint.PROGRESS_CONNECTING);

            printersData[0] = new AsyncEscPosPrinter(
                    BluetoothPrintersConnections.selectFirstPaired(),
                    printerData.getPrinterDpi(),
                    printerData.getPrinterWidthMM(),
                    printerData.getPrinterNbrCharactersPerLine()
            );
            printersData[0].setTextToPrint(printerData.getTextToPrint());
        }

        return super.doInBackground(printersData);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);
        switch (result) {
            case FINISH_SUCCESS:
                printJob.complete();
                break;
            case FINISH_NO_PRINTER:
                printJob.fail("Yazıcı bulunamadı.");
                break;
            case FINISH_PRINTER_DISCONNECTED:
                printJob.fail("Yazıcı bağlantısı koptu.");
                break;
            case FINISH_PARSER_ERROR:
                printJob.fail("Parser hatası.");
                break;
            case FINISH_BARCODE_ERROR:
                printJob.fail("Barkod hatası.");
                break;
            case FINISH_ENCODING_ERROR:
                printJob.fail("Encoding hatası.");
                break;
        }
    }
}
