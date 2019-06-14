package makswinner.fkts.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gnu.io.NRSerialPort;
import java.io.*;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import makswinner.fkts.Compressor;
import makswinner.fkts.Message;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.DataFormatException;

import static java.util.Comparator.comparing;
import static makswinner.fkts.Util.*;

@Slf4j
@Service
public class SerialService {

  public static final int MAX_MESSAGE_SIZE = 222;

  private static final BlockingQueue<Message> OUT_QUEUE = new LinkedBlockingQueue(32);
  private static final Map<String, Set<Message>> MESSAGES = new ConcurrentHashMap<>();

  private static Map<Long, Exception> SEND_EXCEPTIONS;
  private static Map<Long, Exception> RECEIVE_EXCEPTIONS;

  private static final int BAUD_RATE = 9600;
  private static final int TIMES_TO_SEND_ONE_MESSAGE = 2;
  private static final long TIMEOUT_BETWEEN_SENDING_ONE_MESSAGE = 2000;
  private static final long TIMEOUT_BETWEEN_SENDING = 5000;
  private static final long TIMEOUT_BETWEEN_RECEIVING = 200;

  private static final int RECEIVE_BUFFER_SIZE = 1024;

  private final Compressor compressor = new Compressor();

  @Value("${serial.port}")
  private String serialPort;

  @PostConstruct
  public void init() {
    NRSerialPort serial = new NRSerialPort(serialPort, BAUD_RATE);
    log.info("Started serial service, serial port is [{}]", serialPort);
    new Thread(() -> {
      receiveMessages(serial);
    }).start();

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    new Thread(() -> {
      sendMessages(serial);
    }).start();

    new Thread(() -> {
      try {
        sendSomeMessages();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }).start();
  }

  public Set<String> getTopics() {
    return MESSAGES.keySet();
  }

  public Set<Message> getTopicMessages(String topic) {
    return MESSAGES.get(topic);
  }

  public void createNewTopic(String topic) {
    if (MESSAGES.get(topic) != null) {
      //silently swallow
    } else {
      MESSAGES.put(topic, new HashSet<>());
    }
  }

  public void sendMessage(Message message) {
    if (getBytesToSend(message).length <= MAX_MESSAGE_SIZE) {
      offerMessageToQueue(message);
      putMessageToTopic(message);
    } else {
      throw new RuntimeException("Cannot send message because it is too long");
    }
  }

  public Map<String, Object> getStatistics() {
    return new HashMap<String, Object>() {{
      put("SEND_EXCEPTIONS", SEND_EXCEPTIONS);
      put("RECEIVE_EXCEPTIONS", RECEIVE_EXCEPTIONS);
    }};
    //TODO
  }

  private void saveMessages() {
    try {
      new ObjectMapper().writeValue(new File("messages.json"), MESSAGES);
      log.info("Saved messages to a file");
    } catch (IOException e) {
      log.error("Could not save messages to a file", e);
    }
  }

  private void sendSomeMessages() throws Exception {
    int i = 0;
    while (true) {
      String topic = "/Україна/Київ/балачки" + (i % 2 + 1);
      String user = "Все буде Україна!";
      String text = "" + ++i + " Ще не вмерла України і слава, і воля, Ще нам, браття молодії, усміхнеться доля.\n" +
          "Згинуть наші вороженьки, як роса на сонці, Запануєм і ми, браття, у своїй сторонці.";
      Message message = Message.builder()
          .topic(topic).user(user).text(text).createdDateTime(LocalDateTime.now())
          .received(i % 5 == 0)
          .build();
      offerMessageToQueue(message);
      putMessageToTopic(message);
      log.info("Put message [{}] to queue, thread [{}]", message, Thread.currentThread().getName());
      Thread.sleep(10000);

      if (i % 5 == 0) {
        saveMessages();
      }
    }
  }

  private void offerMessageToQueue(Message message) {
    if (OUT_QUEUE.stream().anyMatch(m -> m.contentEquals(message))) {
      log.info("!!! Got duplicated message [{}], not adding to queue", message);
      return;
    } else {
      OUT_QUEUE.offer(message);
    }
  }

  private void putMessageToTopic(Message message) {
    Set<Message> topicMessages = MESSAGES.get(message.getTopic());
    if (topicMessages == null) {
      topicMessages = new HashSet<>();
    } else {
      if (topicMessages.stream().anyMatch(m -> m.contentEquals(message))) {
        log.info("!!! Got duplicated message [{}], not adding to topic", message);
        return;
      }
    }
    topicMessages.add(message);
    MESSAGES.put(message.getTopic(), topicMessages);
    try {
      log.info("Topic [{}] ([{}]) has [{}] received messages, last message text is [{}]",
          message.getTopic(),
          Base64.getEncoder().encodeToString(message.getTopic().getBytes(StandardCharsets.UTF_8.name())),
          topicMessages.stream().filter(m -> m.isReceived()).count(),
          topicMessages.stream().sorted(comparing(Message::getCreatedDateTime).reversed()).findFirst().get().getText()
      );
    } catch (UnsupportedEncodingException e) {
      log.error("Could not log info", e);
    }
  }

  private void sendMessages(NRSerialPort serial) {
    //serial.connect();
    BufferedOutputStream serialOutputStream = IOUtils.buffer(serial.getOutputStream());
    while (true) {
      try {
        Message message = OUT_QUEUE.poll();
        if (message != null) {
          log.info("Sending message [{}]", message.getTextToSend());
          byte[] compressedBytesWithLeadingAndTrailingBytes = getBytesToSend(message);
          log.info("Sending [{}] compressed bytes with leading and trailing bytes, bytes are [{}]",
              compressedBytesWithLeadingAndTrailingBytes.length, compressedBytesWithLeadingAndTrailingBytes);
          for (int i = 0; i < TIMES_TO_SEND_ONE_MESSAGE; i++) {
            long start = System.currentTimeMillis();
            serialOutputStream.write(compressedBytesWithLeadingAndTrailingBytes);
            serialOutputStream.flush();
            log.info("Sent [{}] bytes in [{}] ms",
                    compressedBytesWithLeadingAndTrailingBytes.length, (System.currentTimeMillis() - start));
            if (message.isSent()) {
              Thread.sleep(TIMEOUT_BETWEEN_SENDING_ONE_MESSAGE);
            } else {
              message.setSent(true);
              message.setSentDateTime(LocalDateTime.now());
            }
          }
        }
        Thread.sleep(TIMEOUT_BETWEEN_SENDING);
      } catch (Exception e) {
        log.error("Exception while sending", e);
        SEND_EXCEPTIONS.put(System.currentTimeMillis(), e);
      }
    }
    //serialOutputStream.close();
  }

  private byte[] getBytesToSend(Message message) {
    try {
      byte[] bytesCurrentDateTime = toByteArray(toSeconds(message.getCreatedDateTime()));
      byte[] stringBytesToSend = message.getTextToSend().getBytes(StandardCharsets.UTF_8.name());
      byte[] bytesToSend = ArrayUtils.addAll(bytesCurrentDateTime, stringBytesToSend);
      byte[] compressedBytes = compressor.compress(bytesToSend);
      log.debug("Compressed message bytes [{}] are [{}]", compressedBytes.length, compressedBytes);
      return addLeadingAndTrailingBytes(compressedBytes);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Could not get bytes to send", e);
    }
  }

  @Getter
  @Builder
  private static class ExtractedMessage {
    private byte[] receivedBytes;
    private boolean noTrailingBytes;
    private int messageEnd;
    private boolean checksumOk;
  }

  private void receiveMessages(NRSerialPort serial) {
    serial.connect();
    BufferedInputStream serialInputStream = IOUtils.buffer(serial.getInputStream());
    Integer longByteArrayOffset = 0;
    byte[] longByteArray = initLongByteArray();
    while (true) {
      try {
        if (longByteArrayOffset > RECEIVE_BUFFER_SIZE) {
          //something went so wrong
          log.error("Buffer size is [{}], should never happen, discarding data", longByteArrayOffset);
          longByteArrayOffset = 0;
          longByteArray = initLongByteArray();
        }
        int available = serialInputStream.available();
        //log.info("!!! Available [{}] bytes", available);
        if (available > 0) {
          byte[] bytesInRaw = new byte[MAX_MESSAGE_SIZE];
          int incomingLength = serialInputStream.read(bytesInRaw, 0, MAX_MESSAGE_SIZE);
          log.info("Received [{}] bytes, bytes are [{}]", incomingLength, bytesInRaw);
          System.arraycopy(bytesInRaw, 0, longByteArray, longByteArrayOffset, incomingLength);
          longByteArrayOffset += incomingLength;
          log.info("Current array has size [{}] bytes, bytes are [{}]", longByteArrayOffset, longByteArray);
          ExtractedMessage extractedMessage = extractReceivedBytes(longByteArray, longByteArrayOffset);
          if (extractedMessage != null) {
            updateRollingArray(longByteArrayOffset, longByteArray, extractedMessage.getMessageEnd());
            longByteArrayOffset -= extractedMessage.getMessageEnd();
              log.info("Received message, truncated array now has size [{}] bytes, bytes are [{}]",
                      longByteArrayOffset, longByteArray);
            Message message = getMessageFromExtractedMessage(extractedMessage);
            log.info("Received message is [{}]", message);
            putMessageToTopic(message);
          } else {
            log.info(
                "Partly received message, current array has size [{}] bytes, could not extract message, bytes are [{}]",
                longByteArrayOffset, longByteArray);
          }
        }
        Thread.sleep(TIMEOUT_BETWEEN_RECEIVING);
      } catch (Exception e) {
        log.error("Exception while receiving", e);
        RECEIVE_EXCEPTIONS.put(System.currentTimeMillis(), e);
      }
    }
  }

  private byte[] initLongByteArray() {
    return new byte[RECEIVE_BUFFER_SIZE + MAX_MESSAGE_SIZE * 2];
  }

  private Message getMessageFromExtractedMessage(ExtractedMessage extractedMessage) throws DataFormatException, UnsupportedEncodingException {
    byte[] receivedBytes = extractedMessage.getReceivedBytes();
    log.info("Extracted message without trailing bytes has [{}] bytes, checksum ok [{}], bytes are [{}]",
            receivedBytes.length, extractedMessage.isChecksumOk(), extractedMessage.getReceivedBytes());
    byte[] decompressedBytes = compressor.decompress(receivedBytes);
    log.info("Decompressed received message size is [{}] bytes", decompressedBytes.length);
    int seconds = fromByteArray(decompressedBytes, 0, 4);
    String receivedText = new String(decompressedBytes, 4, decompressedBytes.length - 4, StandardCharsets.UTF_8.name());
    log.info("Received text is [{}]", receivedText);
    return Message.received(receivedText, seconds, extractedMessage.isNoTrailingBytes(), extractedMessage.isChecksumOk());
  }

  private void updateRollingArray(int longByteArrayOffset, byte[] longByteArray, int messageEnd) {
    log.info("updateRollingArray, longByteArrayOffset is [{}], messageEnd is [{}], bytes are [{}]",
            longByteArrayOffset, messageEnd, longByteArray);
    Arrays.copyOfRange(longByteArray, messageEnd, longByteArrayOffset);
      log.info("updateRollingArray after truncate bytes are [{}]", longByteArray);
      Arrays.fill(longByteArray, longByteArrayOffset - messageEnd, longByteArray.length - 1, (byte) 0);
      log.info("updateRollingArray after fill bytes are [{}]", longByteArray);
  }

  private ExtractedMessage extractReceivedBytes(byte[] longByteArray, int longByteArrayOffset) {
    int messageStart = findMessageStart(longByteArray, 0, longByteArrayOffset);
    int messageEnd = -1;
    if (messageStart >= 0) {
      messageEnd = findMessageEnd(longByteArray, messageStart + 2, longByteArrayOffset);
    }
    if (messageStart >= 0 && messageEnd > messageStart + 5) {
      return getExtractedMessage(longByteArray, messageStart, messageEnd);
    }
    return null;
  }

  private ExtractedMessage getExtractedMessage(
      byte[] longByteArray, int messageStart, int messageEnd) {
    byte[] receivedBytes;
    boolean noTrailingBytes = false;
    boolean checksumOk = false;
    receivedBytes = new byte[messageEnd - messageStart - 2];
    System.arraycopy(longByteArray, messageStart + 2, receivedBytes, 0, messageEnd - messageStart - 2);
    if (!isMessageEnd(receivedBytes, receivedBytes.length - 2)) {
      noTrailingBytes = true;
    } else {
      byte checksumRemote = receivedBytes[receivedBytes.length - 3];
      receivedBytes = Arrays.copyOf(receivedBytes, receivedBytes.length - 3);
      checksumOk = checksumRemote == checksum(receivedBytes);
    }
    return ExtractedMessage.builder()
        .receivedBytes(receivedBytes).noTrailingBytes(noTrailingBytes).messageEnd(messageEnd).checksumOk(checksumOk)
        .build();
  }

  private int findMessageStart(byte[] bytes, int start, int end) {
    for (int i = start; i < end - 1; i++) {
      if (isMessageStart(bytes, i)) {
        return i;
      }
    }
    return -1;
  }

  private int findMessageEnd(byte[] bytes, int start, int end) {
    int result = -1;
    for (int i = start; i < end - 1; i++) {
      if (isMessageEnd(bytes, i)) {
        return i + 2;
      }
    }
    if (result == -1 && end > MAX_MESSAGE_SIZE) {
      //try to find at least new message start
      result = findMessageStart(bytes, start, end);
    }
    return result;
  }

  private boolean isMessageStart(byte[] bytes, int offset) {
    return bytes[offset] == (byte) -127 && bytes[offset + 1] == (byte) -128;
  }

  private boolean isMessageEnd(byte[] bytes, int offset) {
    return bytes[offset] == (byte) -128 && bytes[offset + 1] == (byte) -127;
  }

  private byte[] addLeadingAndTrailingBytes(byte[] compressedBytes) {
    byte[] bytesToSendWithTrailingBytes = new byte[compressedBytes.length + 5];
    System.arraycopy(compressedBytes, 0, bytesToSendWithTrailingBytes, 2, compressedBytes.length);
    bytesToSendWithTrailingBytes[0] = (byte) -127;
    bytesToSendWithTrailingBytes[1] = (byte) -128;
    bytesToSendWithTrailingBytes[compressedBytes.length + 2] = checksum(compressedBytes);
    bytesToSendWithTrailingBytes[compressedBytes.length + 3] = (byte) -128;
    bytesToSendWithTrailingBytes[compressedBytes.length + 4] = (byte) -127;
    return bytesToSendWithTrailingBytes;
  }

}
