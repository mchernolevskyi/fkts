package makswinner.fkts;

import gnu.io.NRSerialPort;
import makswinner.fkts.Compressor.Method;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class SerialWorker {
    private static final String port = "COM4";
    private static final int baudRate = 9600;
    private static final int timesToSend = 2;
    private static final long timeoutMs = 5000;

    private final Compressor compressor = new Compressor();

    public static void main(String[] args) throws Exception {
        String user = "Все буде Україна!";
        String message = "Ще не вмерла України і слава, і воля, Ще нам, браття молодії, усміхнеться доля. " +
                "Згинуть наші вороженьки, як роса на сонці, Запануєм і ми, браття, у своїй сторонці.";
        SerialWorker serialWorker = new SerialWorker();
        serialWorker.sendMessage(user, message);
//        new Compressor().compress(message.getBytes(), Method.LZMA);
//        new Compressor().compress(message.getBytes(), Method.DEFLATE);
    }

    public void sendMessage(String user, String message) throws IOException, InterruptedException {
        String textToSend = user + ":" + message;
        byte [] stringBytesToSend = textToSend.getBytes();
        int secondsSince20190101 = countSecondsSince20190101();
        byte[] bytesCurrentDateTime = ByteBuffer.allocate(4).putInt(secondsSince20190101).array();
        byte[] bytesToSend = ArrayUtils.addAll(bytesCurrentDateTime, stringBytesToSend);
        byte [] compressedBytes = compressor.compress(bytesToSend, Method.DEFLATE);
        System.out.println("Sending [" + compressedBytes.length + "] compressed bytes:");
        NRSerialPort serial = new NRSerialPort(port, baudRate);
        serial.connect();
        BufferedOutputStream out = IOUtils.buffer(serial.getOutputStream());
        for (int i = 0; i < timesToSend; i++) {
            long start = System.currentTimeMillis();
            out.write(compressedBytes);
            out.flush();
            System.out.println("Sent [" + compressedBytes.length + "] compressed bytes in [" + (System.currentTimeMillis() - start) + "] ms");
            Thread.sleep(timeoutMs);
        }
        serial.disconnect();
    }

    private int countSecondsSince20190101() {
        return 46276481;//TODO
    }

}
