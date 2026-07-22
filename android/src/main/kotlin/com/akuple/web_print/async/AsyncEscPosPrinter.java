package com.akuple.web_print.async;

import com.dantsu.escposprinter.EscPosPrinterSize;
import com.dantsu.escposprinter.connection.DeviceConnection;

import java.util.ArrayList;
import java.util.List;

public class AsyncEscPosPrinter extends EscPosPrinterSize {
    private DeviceConnection printerConnection;
    private String textToPrint = "";

    /**
     * Sayfanın yatay bantlara bölünmüş hâli. Her eleman tek bir ESC/POS raster
     * komutudur ve yazıcıya ayrı ayrı gönderilir. Doluysa {@link #textToPrint}
     * yerine bu kullanılır.
     */
    private List<byte[]> imageBands = new ArrayList<>();

    public AsyncEscPosPrinter(DeviceConnection printerConnection, int printerDpi, float printerWidthMM, int printerNbrCharactersPerLine) {
        super(printerDpi, printerWidthMM, printerNbrCharactersPerLine);
        this.printerConnection = printerConnection;
    }

    public DeviceConnection getPrinterConnection() {
        return this.printerConnection;
    }

    public AsyncEscPosPrinter setTextToPrint(String textToPrint) {
        this.textToPrint = textToPrint;
        return this;
    }

    public String getTextToPrint() {
        return this.textToPrint;
    }

    public void setImageBands(List<byte[]> imageBands) {
        this.imageBands = imageBands;
    }

    public List<byte[]> getImageBands() {
        return this.imageBands;
    }
}
