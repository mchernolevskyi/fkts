package makswinner.fkts;

import static makswinner.fkts.Util.*;

import gnu.io.NRSerialPort;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.DataFormatException;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j
public class SerialWorker implements Runnable {

  private static final String SERIAL_PORT = "COM5";
  private static final int BAUD_RATE = 9600;
  private static final int TIMES_TO_SEND_ONE_MESSAGE = 1;
  private static final long TIMEOUT_BETWEEN_SENDING_ONE_MESSAGE = 2000;
  private static final long TIMEOUT_BETWEEN_SENDING = 5000;
  private static final long TIMEOUT_BETWEEN_RECEIVING = 200;

  public static final BlockingQueue<Message> OUT_QUEUE = new LinkedBlockingQueue(32);
  public static final Map<String, Set<Message>> MESSAGES = new ConcurrentHashMap<>();
  public static final int MAX_PACKET_SIZE = 256;

  private final Compressor compressor = new Compressor();

  public static void main(String[] args) throws Exception {
    new Thread(new SerialWorker()).start();

    int i = 0;
    while (true) {
      String topic = "/Україна/Київ/балачки";
      String user = "Все буде Україна!";
      String text = "Ще не вмерла України і слава, і воля, Ще нам, браття молодії, усміхнеться доля.\n" +
          "Згинуть наші вороженьки, як роса на сонці, Запануєм і ми, браття, у своїй сторонці: " + i++;
      Message message = Message.builder().topic(topic).user(user).text(text).dateTime(LocalDateTime.now()).build();
      OUT_QUEUE.offer(message);
      Thread.sleep(10000);
    }
  }

  public void run() {
    NRSerialPort serial = new NRSerialPort(SERIAL_PORT, BAUD_RATE);
    new Thread(() -> {
      receiveMessages(serial);
    }).start();
    try {
      Thread.sleep(TIMEOUT_BETWEEN_RECEIVING * 3);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    new Thread(() -> {
      sendMessages(serial);
    }).start();
  }

  public void sendMessages(NRSerialPort serial) {
    //serial.connect();
    BufferedOutputStream serialOutputStream = IOUtils.buffer(serial.getOutputStream());
    while (true) {
      try {
        Message message = OUT_QUEUE.poll();
        if (message != null) {
          byte[] bytesCurrentDateTime = toByteArray(toSeconds(message.getDateTime()));
          byte[] stringBytesToSend = message.getTextToSend().getBytes();
          byte[] bytesToSend = ArrayUtils.addAll(bytesCurrentDateTime, stringBytesToSend);
          byte[] compressedBytes = compressor.compress(bytesToSend);
          byte[] compressedBytesWithTrailingBytes = addTrailingBytes(compressedBytes);
          log.info("Sending [{}] compressed bytes with trailing bytes: [{}]",
              compressedBytesWithTrailingBytes.length, compressedBytesWithTrailingBytes);
          for (int i = 0; i < TIMES_TO_SEND_ONE_MESSAGE; i++) {
            long start = System.currentTimeMillis();
            serialOutputStream.write(compressedBytesWithTrailingBytes);
            serialOutputStream.flush();
            if (message.isSent()) {
              Thread.sleep(TIMEOUT_BETWEEN_SENDING_ONE_MESSAGE);
            } else {
              message.setSent(true);
              Set<Message> topicMessages = MESSAGES.get(message.getTopic());
              topicMessages.add(message);
              MESSAGES.put(message.getTopic(), topicMessages);//TODO move to controller/service
            }
            log.info("Sent [{}] bytes in [{}] ms",
                compressedBytesWithTrailingBytes.length, (System.currentTimeMillis() - start));
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
    //serialOutputStream.close();
  }

  @Getter
  @Builder
  private static class ExtractedMessage {
    private byte[] receivedBytes;
    private boolean noTrailingBytes;
    private int messageEnd;
  }

  public void receiveMessages(NRSerialPort serial) {
    serial.connect();
    BufferedInputStream serialInputStream = IOUtils.buffer(serial.getInputStream());
    Integer longByteArrayOffset = 0;
    byte[] longByteArray = new byte[10000];
    while (true) {
      try {
        if (serialInputStream.available() >= 10) {
          byte[] bytesInRaw = new byte[MAX_PACKET_SIZE];
          int incomingLength = serialInputStream.read(bytesInRaw, 0, MAX_PACKET_SIZE);
          log.info("Received new [{}] bytes", incomingLength);
          System.arraycopy(bytesInRaw, 0, longByteArray, longByteArrayOffset, incomingLength);
          longByteArrayOffset += incomingLength;
          ExtractedMessage extractedMessage = extractReceivedBytes(longByteArray, longByteArrayOffset);
          if (extractedMessage != null) {
            updateRollingArray(longByteArrayOffset, longByteArray, extractedMessage);
            longByteArrayOffset = 0;
            byte[] receivedBytes = extractedMessage.getReceivedBytes();
            log.info("Extracted new message of [{}] bytes", receivedBytes.length);
            byte[] decompressedBytes = compressor.decompress(receivedBytes);
            log.info("Decompressed size [{}] bytes", decompressedBytes.length);
            int seconds = fromByteArray(decompressedBytes, 0, 4);
            String receivedText = new String(decompressedBytes, 4, decompressedBytes.length - 4);
            log.info("Received text: [{}]", receivedText);
            Message message = Message.of(receivedText, seconds, extractedMessage.isNoTrailingBytes());
            Set<Message> topicMessages = MESSAGES.get(message.getTopic());
            topicMessages.add(message);
            log.info("Message: [{}]", message);
          } else {
            log.info("Partly received message, current size is [{}] bytes, could not extract message", longByteArrayOffset);
          }
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

  private void updateRollingArray(Integer longByteArrayOffset, byte[] longByteArray, ExtractedMessage extractedMessage) {
    Arrays.copyOfRange(longByteArray, extractedMessage.getMessageEnd(), longByteArrayOffset);
    Arrays.fill(longByteArray, longByteArrayOffset - extractedMessage.getMessageEnd(), longByteArray.length - 1, (byte) 0);
  }

  private ExtractedMessage extractReceivedBytes(byte[] longByteArray, Integer longByteArrayOffset) {
    int messageStart = findMessageStart(longByteArray, 0, longByteArrayOffset);
    int messageEnd = -1;
    if (messageStart >= 0) {
      messageEnd = findMessageEnd(longByteArray, messageStart + 2, longByteArrayOffset);
    }
    if (messageStart >= 0 && messageEnd > messageStart + 2) {
      return getExtractedMessage(longByteArray, messageStart, messageEnd);
    }
    return null;
  }

  private ExtractedMessage getExtractedMessage(
      byte[] longByteArray, int messageStart, int messageEnd) {
    byte[] receivedBytes = null;
    boolean noTrailingBytes = false;
    receivedBytes = new byte[messageEnd - messageStart];
    System.arraycopy(longByteArray, messageStart, receivedBytes, 0, messageEnd - messageStart);
    if (!isMessageEnd(receivedBytes, receivedBytes.length - 2)) {
      noTrailingBytes = true;
    } else {
      receivedBytes = Arrays.copyOf(receivedBytes, receivedBytes.length - 2);
    }
    return ExtractedMessage.builder().receivedBytes(receivedBytes).noTrailingBytes(noTrailingBytes).messageEnd(messageEnd).build();
  }

  private int findMessageStart(byte[] bytes, int start, Integer end) {
    for (int i = start; i < end - 2; i++) {
      if (isMessageStart(bytes, i)) {
        return i;
      }
    }
    return -1;
  }

  private int findMessageEnd(byte[] bytes, int start, Integer end) {
    int result = -1;
    for (int i = start; i < end - 2; i++) {
      if (isMessageEnd(bytes, i)) {
        return i;
      }
    }
    if (result == -1) {
      //try to find at least new message start
      result = findMessageStart(bytes, start, end);
    }
    return result;
  }

  private boolean isMessageStart(byte[] bytes, int offset) {
    return bytes[offset] == (byte) 85 && bytes[offset + 1] == (byte) -113;
  }

  private boolean isMessageEnd(byte[] bytes, int offset) {
    return bytes[offset] == (byte) -254 && bytes[offset + 1] == (byte) -253;
  }

  private byte[] addTrailingBytes(byte[] compressedBytes) {
    byte[] bytesToSendWithTrailingBytes = new byte[compressedBytes.length + 2];
    System.arraycopy(compressedBytes, 0, bytesToSendWithTrailingBytes, 0, compressedBytes.length);
    bytesToSendWithTrailingBytes[compressedBytes.length] = (byte) -254;
    bytesToSendWithTrailingBytes[compressedBytes.length + 1] = (byte) -253;
    return bytesToSendWithTrailingBytes;
  }

}
