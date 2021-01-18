package com.akuple.web_print.async;

import com.dantsu.escposprinter.EscPosCharsetEncoding;
import com.dantsu.escposprinter.EscPosPrinterCommands;
import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;

public class CustomEscPosPrinterCommands extends EscPosPrinterCommands {
    private DeviceConnection printerConnection;

    public CustomEscPosPrinterCommands(DeviceConnection printerConnection) {
        super(printerConnection);
        this.printerConnection = printerConnection;
    }

    public CustomEscPosPrinterCommands(DeviceConnection printerConnection, EscPosCharsetEncoding charsetEncoding) {
        super(printerConnection, charsetEncoding);
    }

    /**
     * Feed the paper
     *
     * @return Fluent interface
     */
    public EscPosPrinterCommands feedPage() throws EscPosConnectionException {
        if (!this.printerConnection.isConnected()) {
            return this;
        }

        this.printerConnection.write(new byte[]{0x1D, 0x0C});


        return this;
    }

    /**
     * Select page mode
     *
     * @return Fluent interface
     */
    public EscPosPrinterCommands selectPageMode() throws EscPosConnectionException {
        if (!this.printerConnection.isConnected()) {
            return this;
        }

        this.printerConnection.write(new byte[]{0x1B, 0x4C});

        return this;
    }

    /**
     * Select page mode
     *
     * @return Fluent interface
     */
    public EscPosPrinterCommands blackMarkAdjustment() throws EscPosConnectionException {
        if (!this.printerConnection.isConnected()) {
            return this;
        }

        this.printerConnection.write(new byte[]{0x1D, 0x28, 0x4D, 0x02, 0x01, 0x31});

        return this;
    }

    /**
     * Select page mode
     *
     * @return Fluent interface
     */
    public EscPosPrinterCommands feedByMM(int mm) throws EscPosConnectionException {
        if (!this.printerConnection.isConnected()) {
            return this;
        }

        byte n = (byte) (mm / 0.125);

        this.printerConnection.write(new byte[]{0x1B, 0x4A, n});

        return this;
    }

    public void send() throws EscPosConnectionException {
        if (this.printerConnection.isConnected()) {
            printerConnection.send(100);
        }
    }

}
