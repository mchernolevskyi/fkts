package makswinner.fkts;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Deflater;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorOutputStream;

public class Compressor {

  public enum Method {
    LZMA,
    DEFLATE
  }

  public byte [] compressDeflate(byte [] input) {
    System.out.println("Input size [" + input.length + "] bytes");
    Deflater deflater = new Deflater();
    deflater.setInput(input);
    deflater.finish();
    byte[] compressed = new byte[1024];
    int compressedDataLength = deflater.deflate(compressed);
    System.out.println("Compressed size [" + compressedDataLength + "] bytes");
    byte [] result = new byte[compressedDataLength];
    System.arraycopy(compressed, 0, result, 0, compressedDataLength);
    return result;
  }

  private byte [] compressLzma(byte [] input) throws IOException {
    System.out.println("Input size [" + input.length + "] bytes");
    InputStream fin = new ByteArrayInputStream(input);
    BufferedInputStream in = new BufferedInputStream(fin);
    LZMACompressorOutputStream lzmaIn = new LZMACompressorOutputStream(in);
    final byte[] buffer = new byte[1024];
    int n = 0;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
//    while (-1 != (n = lzmaIn.read(buffer))) {
//      out.write(buffer, 0, n);
//    }
    out.close();
    lzmaIn.close();
    byte [] result = out.toByteArray();
    System.out.println("Compressed size [" + result.length + "] bytes");
    return result;
  }

  public byte [] compress(byte [] input, Method method) throws IOException {
    switch(method) {
      case LZMA: return compressLzma(input);
      case DEFLATE: return compressDeflate(input);
      default: return compressDeflate(input);
    }
  }

}
