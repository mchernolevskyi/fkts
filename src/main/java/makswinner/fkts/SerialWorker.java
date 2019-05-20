package makswinner.fkts;

import gnu.io.NRSerialPort;
import java.io.BufferedInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.DataFormatException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class SerialWorker {
    private static final String port = "COM4";
    private static final int baudRate = 9600;
    private static final int timesToSend = 2;
    private static final long timeoutMs = 5000;

    public static final BlockingQueue<Message> queue = new LinkedBlockingQueue(32);
    public static final Map<String, Message> messages = new ConcurrentHashMap<>();

    private final Compressor compressor = new Compressor();

    public static void main(String[] args) throws Exception {
        String topic = "/Україна/Київ/балачки";
        String user = "Все буде Україна!";
        String text = "Ще не вмерла України і слава, і воля, Ще нам, браття молодії, усміхнеться доля. " +
                "Згинуть наші вороженьки, як роса на сонці, Запануєм і ми, браття, у своїй сторонці.";
        Message message = Message.builder().topic(topic).user(user).text(text).dateTime(LocalDateTime.now()).build();
        queue.offer(message);
        SerialWorker serialWorker = new SerialWorker();
        serialWorker.sendMessage();

//        Compressor compressor = new Compressor();
//        byte [] compressed = compressor.compress(text.getBytes());
//        byte [] decompressed = compressor.decompress(compressed);
//        log.info("res = {}", new String(decompressed));

    }

    public void sendMessage() throws IOException, InterruptedException {
        Message message = queue.poll();
        if (message != null) {
            int seconds = toSeconds(message.getDateTime());
            byte[] bytesCurrentDateTime = toByteArray(seconds);
            String textToSend = getTextToSend(message);
            byte [] stringBytesToSend = textToSend.getBytes();
            byte[] bytesToSend = ArrayUtils.addAll(bytesCurrentDateTime, stringBytesToSend);
            byte [] compressedBytes = compressor.compress(bytesToSend);
            log.info("Sending [{}] compressed bytes:", compressedBytes.length);
            NRSerialPort serial = new NRSerialPort(port, baudRate);
            serial.connect();
            BufferedOutputStream out = IOUtils.buffer(serial.getOutputStream());
            for (int i = 0; i < timesToSend; i++) {
                long start = System.currentTimeMillis();
                out.write(compressedBytes);
                out.flush();
                if (!message.isSent()) {
                    message.setSent(true);
                }
                log.info("Sent [{}] compressed bytes in [{}] ms",
                        compressedBytes.length, (System.currentTimeMillis() - start));
                Thread.sleep(timeoutMs);
            }
            serial.disconnect();
        }
    }

    private byte[] toByteArray(int value) {
        return  ByteBuffer.allocate(4).putInt(value).array();
    }

    private int fromByteArray(byte[] bytes, int offset, int length) {
        return ByteBuffer.wrap(bytes, offset, length).getInt();
    }

    public void receiveMessage() throws IOException, InterruptedException, DataFormatException {
        NRSerialPort serial = new NRSerialPort(port, baudRate);
        serial.connect();
        BufferedInputStream in = IOUtils.buffer(serial.getInputStream());
        byte [] bytesInRaw = new byte[256];
        int incomingLength = in.read(bytesInRaw, 0 , 256);
        byte [] bytesReceived = new byte[incomingLength];
        System.arraycopy(bytesInRaw, 0 , bytesReceived, 0 , incomingLength);
        byte [] decompressedBytes = compressor.decompress(bytesReceived);
        int seconds = fromByteArray(decompressedBytes, 0, 4);
        LocalDateTime dateTime = fromSeconds(seconds);
        log.info("Received [{}] bytes:", decompressedBytes.length - 4);
        Message message = Message.builder().received(true).dateTime(dateTime).build();

        //TODO parse


        serial.disconnect();
    }

    private String getTextToSend(Message message) {
        return message.getTopic() + ":" + message.getUser() + ":" + message.getText();
    }

    private int toSeconds(LocalDateTime dateTime) {
        //Since 20190101
        return 46276481;//TODO
    }

    private LocalDateTime fromSeconds(int seconds) {
        //Since 20190101
        return LocalDateTime.now();//TODO
    }

}
