package makswinner.fkts;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class Compressor {

  public enum Method {
    LZMA,//maybe add some unique algorithm
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

  private byte [] decompressDeflate(byte [] input) throws IOException, DataFormatException {
    Inflater inflater = new Inflater(true);
    inflater.setInput(input);
    byte[] decompressed = new byte[2048];
    int decompressedDataLength = inflater.inflate(decompressed);
    byte [] result = new byte[decompressedDataLength];
    System.arraycopy(decompressed, 0, result, 0, decompressedDataLength);
    return result;
  }

  public byte [] compress(byte [] input, Method method) throws IOException {
    System.out.print("Compression start: method [" + method + "], input array of [" + input.length + "] bytes ->");
    byte [] result;
    switch(method) {
      case DEFLATE: result = compressDeflate(input); break;
      default: result = compressDeflate(input);
    }
    System.out.println(" compressed size [" + result.length + "] bytes");
    return result;
  }

  public byte [] compress(byte [] input) throws IOException {
    return compress(input, Method.DEFLATE);
  }

  public byte [] decompress(byte [] input, Method method) throws IOException, DataFormatException {
    System.out.print("Decompression start: method [" + method + "], input array of [" + input.length + "] bytes ->");
    byte [] result;
    switch(method) {
      case DEFLATE: result = decompressDeflate(input); break;
      default: result = decompressDeflate(input);
    }
    System.out.println(" decompressed size [" + result.length + "] bytes");
    return result;
  }

  public byte [] decompress(byte [] input) throws IOException, DataFormatException {
    return decompress(input, Method.DEFLATE);
  }

}
