package makswinner.fkts;

import gnu.io.NRSerialPort;
import makswinner.fkts.Compressor.Method;
import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;

public class SerialWorker {

    public static void main(String[] args) throws Exception {
        String message = "Ще не вмерла України і слава, і воля, Ще нам, браття молодії, усміхнеться доля.";
        //new SerialWorker().sendMessage(message);
        new Compressor().compress(message.getBytes(), Method.LZMA);
        new Compressor().compress(message.getBytes(), Method.DEFLATE);
    }

    public void sendMessage(String message) throws IOException, InterruptedException {
        String port = "COM4";
        int baudRate = 9600;
        int timesToSend = 3;
        long timeoutMs = 1000;

        NRSerialPort serial = new NRSerialPort(port, baudRate);
        serial.connect();

        byte [] compressedBytes = new Compressor().compress(message.getBytes(), Method.DEFLATE);

        BufferedOutputStream out = IOUtils.buffer(serial.getOutputStream());
        for (int i = 0; i < timesToSend; i++) {
            long start = System.currentTimeMillis();
            out.write(compressedBytes);
            out.flush();
            System.out.println("Sent [" + compressedBytes.length + "] bytes in [" + (System.currentTimeMillis() - start) + "] ms");
            Thread.sleep(timeoutMs);
        }
        serial.disconnect();
    }

}
