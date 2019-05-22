package makswinner.fkts;

import gnu.io.NRSerialPort;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.DataFormatException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j
public class SerialWorker {
    private static final String SERIAL_PORT = "/dev/pts/1";
    private static final int BAUD_RATE = 9600;
    private static final int TIMES_TO_SEND_ONE_MESSAGE = 2;
    private static final long TIMEOUT_BETWEEN_SENDING_ONE_MESSAGE = 5000;
    private static final long TIMEOUT_BETWEEN_SENDING = 1000;
    private static final long TIMEOUT_BETWEEN_RECEIVING = 100;

    public static final BlockingQueue<Message> OUT_QUEUE = new LinkedBlockingQueue(32);
    public static final Map<String, Message> MESSAGES = new ConcurrentHashMap<>();

    private final Compressor compressor = new Compressor();

    public static void main(String[] args) throws Exception {
        String topic = "/Україна/Київ/балачки";
        String user = "Все буде Україна!";
        String text = "Ще не вмерла України і слава, і воля, Ще нам, браття молодії, усміхнеться доля. " +
                "Згинуть наші вороженьки, як роса на сонці, Запануєм і ми, браття, у своїй сторонці.";
        Message message = Message.builder().topic(topic).user(user).text(text).dateTime(LocalDateTime.now()).build();
        OUT_QUEUE.offer(message);
        new SerialWorker().run();

//        Compressor compressor = new Compressor();
//        byte [] compressed = compressor.compress(text.getBytes());
//        byte [] decompressed = compressor.decompress(compressed);
//        log.info("res = {}", new String(decompressed));
    }

    public void run() {
        NRSerialPort serial = new NRSerialPort(SERIAL_PORT, BAUD_RATE);
        serial.connect();
        BufferedOutputStream serialOutputStream = IOUtils.buffer(serial.getOutputStream());
        BufferedInputStream serialInputStream = IOUtils.buffer(serial.getInputStream());
        new Thread(() -> {
            receiveMessages(serialInputStream);
        }).start();
        new Thread(() -> {
            sendMessages(serialOutputStream);
        }).start();
        serial.disconnect();
    }

    public void sendMessages(BufferedOutputStream serialOutputStream) {
        while (true) {
            try {
                Message message = OUT_QUEUE.poll();
                if (message != null) {
                    int seconds = toSeconds(message.getDateTime());
                    byte[] bytesCurrentDateTime = toByteArray(seconds);
                    String textToSend = getTextToSend(message);
                    byte[] stringBytesToSend = textToSend.getBytes();
                    byte[] bytesToSend = ArrayUtils.addAll(bytesCurrentDateTime, stringBytesToSend);
                    byte[] compressedBytes = compressor.compress(bytesToSend);
                    log.info("Sending [{}] compressed bytes:", compressedBytes.length);
                    for (int i = 0; i < TIMES_TO_SEND_ONE_MESSAGE; i++) {
                        long start = System.currentTimeMillis();
                        serialOutputStream.write(compressedBytes);
                        serialOutputStream.flush();
                        if (!message.isSent()) {
                            message.setSent(true);
                            MESSAGES.put(message.getTopic(), message);//TODO move to controller/service
                        }
                        log.info("Sent [{}] compressed bytes in [{}] ms",
                            compressedBytes.length, (System.currentTimeMillis() - start));
                        Thread.sleep(TIMEOUT_BETWEEN_SENDING_ONE_MESSAGE);
                    }
                }
                Thread.sleep(TIMEOUT_BETWEEN_SENDING);
            } catch (IOException e) {
                log.error("IOException while sending", e);
            } catch (InterruptedException e) {
                log.error("InterruptedException while sending", e);
            } catch (Exception e) {
                log.error("Undefined exception while sending", e);
            }
        }
    }

    public void receiveMessages(BufferedInputStream serialInputStream) {
        while (true) {
            try {
                if (serialInputStream.available() > 0) {
                    byte [] bytesInRaw = new byte[256];
                    int incomingLength = serialInputStream.read(bytesInRaw, 0 , 256);
                    log.info("Received new message, size [{}] bytes", incomingLength);
                    byte [] bytesReceived = new byte[incomingLength];
                    System.arraycopy(bytesInRaw, 0 , bytesReceived, 0 , incomingLength);
                    byte [] decompressedBytes = compressor.decompress(bytesReceived);
                    int seconds = fromByteArray(decompressedBytes, 0, 4);
                    log.info("Decompressed size [{}] bytes", decompressedBytes.length);
                    String receivedText = new String(decompressedBytes, 4, decompressedBytes.length - 4);
                    Message message = reconstructMessage(receivedText, seconds);
                    MESSAGES.put(message.getTopic(), message);
                }
                Thread.sleep(TIMEOUT_BETWEEN_RECEIVING);
            } catch (IOException e) {
                log.error("IOException while receiving", e);
            } catch (DataFormatException e) {
                log.error("DataFormatException while receiving", e);
            } catch (InterruptedException e) {
                log.error("InterruptedException while receiving", e);
            } catch (Exception e) {
                log.error("Undefined exception while receiving", e);
            }
        }
    }

    private Message reconstructMessage(String receivedText, int seconds) {
        String [] split1 = receivedText.split(":", 2);
        String topic = split1[0];
        String [] split2 = split1[1].split(":", 2);
        String user = split2[0];
        String text = split2[1];
        LocalDateTime dateTime = fromSeconds(seconds);
        return Message.builder().received(true).dateTime(dateTime)
            .topic(topic).user(user).text(text).build();
    }

    private String getTextToSend(Message message) {
        return message.getTopic() + ":" + message.getUser() + ":" + message.getText();
    }

    private int toSeconds(LocalDateTime dateTime) {
      long ms = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
      return (int) (ms / 1000);
    }

    private LocalDateTime fromSeconds(int seconds) {
      long ms = ((long) seconds) * 1000L;
      return LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), TimeZone.getDefault().toZoneId());
    }

    private byte[] toByteArray(int value) {
        return  ByteBuffer.allocate(4).putInt(value).array();
    }

    private int fromByteArray(byte[] bytes, int offset, int length) {
        return ByteBuffer.wrap(bytes, offset, length).getInt();
    }

}
