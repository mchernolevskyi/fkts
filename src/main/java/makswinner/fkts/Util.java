package makswinner.fkts;

import java.nio.ByteBuffer;
import java.time.*;
import java.util.TimeZone;

public class Util {

  public static int toSeconds(LocalDateTime dateTime) {
    long ms = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    return (int) (ms / 1000);
  }

  public static LocalDateTime fromSeconds(int seconds) {
    long ms = ((long) seconds) * 1000L;
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), TimeZone.getDefault().toZoneId());
  }

  public static byte[] toByteArray(int value) {
    return ByteBuffer.allocate(4).putInt(value).array();
  }

  public static int fromByteArray(byte[] bytes, int offset, int length) {
    return ByteBuffer.wrap(bytes, offset, length).getInt();
  }

  public static short computeBsdChecksumShort(byte[] bytes) {
    int result = 0;
    for (byte current : bytes) {
      result = (result >>> 1) + ((result & 1) << 15);
      result += current;
      result &= 0xffff;
    }
    return (short) result;
  }
}
