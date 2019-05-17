package makswinner.fkts;

import java.util.zip.Deflater;

public class Compressor {

  public byte [] compress(byte [] input) {
    System.out.println("Compressor: input size [" + input.length + "] bytes");
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

}
