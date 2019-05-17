package makswinner.fkts;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Deflater;
import org.apache.commons.compress.compressors.lzma.LZMACompressorOutputStream;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.LZMAOutputStream;

public class Compressor {

  public enum Method {
    LZMA,
    DEFLATE
  }

  public byte [] compressDeflate(byte [] input) {
    Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
    deflater.setInput(input);
    deflater.finish();
    byte[] compressed = new byte[1024];
    int compressedDataLength = deflater.deflate(compressed);
    byte [] result = new byte[compressedDataLength];
    System.arraycopy(compressed, 0, result, 0, compressedDataLength);
    return result;
  }

  private byte [] compressLzma(byte [] input) throws IOException {
    InputStream in = new ByteArrayInputStream(input);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    LZMAOutputStream lzmaOut = new LZMAOutputStream(out, new LZMA2Options(LZMA2Options.PRESET_MAX), -1);
    final byte[] buffer = new byte[1024];
    int n = 0;
    while (-1 != (n = in.read(buffer))) {
      lzmaOut.write(buffer, 0, n);
    }
    lzmaOut.close();
    in.close();
    return out.toByteArray();
  }

  public byte [] compress(byte [] input, Method method) throws IOException {
    System.out.print("Compression start: method [" + method + "], input array of [" + input.length + "] bytes ->");
    byte [] result;
    switch(method) {
      case LZMA: result = compressLzma(input); break;
      case DEFLATE: result = compressDeflate(input); break;
      default: result = compressDeflate(input);
    }
    System.out.println(" compressed size [" + result.length + "] bytes");
    return result;
  }

}
