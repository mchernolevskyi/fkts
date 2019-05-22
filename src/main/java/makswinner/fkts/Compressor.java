package makswinner.fkts;

import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Compressor {

  public enum Method {
    LZMA,//tested, not good enough, maybe switch to some new algorithm in future
    DEFLATE
  }

  private byte [] compressDeflate(byte [] input) {
    Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
    deflater.setInput(input);
    deflater.finish();
    byte[] compressed = new byte[1024];
    int compressedDataLength = deflater.deflate(compressed);
    byte [] result = new byte[compressedDataLength];
    System.arraycopy(compressed, 0, result, 0, compressedDataLength);
    return result;
  }

  private byte [] decompressDeflate(byte [] input) throws DataFormatException {
    Inflater inflater = new Inflater(true);
    inflater.setInput(input);
    byte[] decompressed = new byte[2048];
    int decompressedDataLength = inflater.inflate(decompressed);
    byte [] result = new byte[decompressedDataLength];
    System.arraycopy(decompressed, 0, result, 0, decompressedDataLength);
    return result;
  }

  public byte [] compress(byte [] input, Method method) {
    log.debug("Compression start: method [{}], input of [{}] bytes", method, input.length);
    byte [] result;
    switch(method) {
      case DEFLATE: result = compressDeflate(input); break;
      default: throw new RuntimeException("Compression method not supported");
    }
    log.debug("Compression end: method [{}], compressed size [{}] bytes", method, result.length);
    return result;
  }

  public byte [] compress(byte [] input) {
    return compress(input, Method.DEFLATE);
  }

  public byte [] decompress(byte [] input, Method method) throws DataFormatException {
    log.debug("Decompression start: method [{}], input of [{}] bytes", method, input.length);
    byte [] result;
    switch(method) {
      case DEFLATE: result = decompressDeflate(input); break;
      default: throw new RuntimeException("Compression method not supported");
    }
    log.debug("Decompression end: method [{}], decompressed size [{}] bytes", method, result.length);
    return result;
  }

  public byte [] decompress(byte [] input) throws DataFormatException {
    return decompress(input, Method.DEFLATE);
  }

}
