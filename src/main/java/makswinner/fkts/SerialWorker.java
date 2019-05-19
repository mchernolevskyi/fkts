package makswinner.fkts;

import gnu.io.NRSerialPort;
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

    private final Compressor compressor = new Compressor();

    public static void main(String[] args) throws Exception {
        String topic = "/Україна/Київ/балачки";
        String user = "Все буде Україна!";
        String text = "Ще не вмерла України і слава, і воля, Ще нам, браття молодії, усміхнеться доля. " +
                "Згинуть наші вороженьки, як роса на сонці, Запануєм і ми, браття, у своїй сторонці.";
        Message message = Message.builder().topic(topic).user(user).text(text).build();
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
            int secondsSince20190101 = toSecondsSince20190101();
            byte[] bytesCurrentDateTime = ByteBuffer.allocate(4).putInt(secondsSince20190101).array();
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
                log.info("Sent [{}] compressed bytes in [{}] ms",
                        compressedBytes.length, (System.currentTimeMillis() - start));
                Thread.sleep(timeoutMs);
            }
            serial.disconnect();
        }
    }

    private String getTextToSend(Message message) {
        return message.getTopic() + ":" + message.getUser() + ":" + message.getText();
    }

    private int toSecondsSince20190101() {
        return 46276481;//TODO
    }

    private LocalDateTime fromSecondsSince20190101(int seconds) {
        return LocalDateTime.now();//TODO
    }

}
