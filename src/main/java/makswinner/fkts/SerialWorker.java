package makswinner.fkts;

import gnu.io.NRSerialPort;
import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;

public class SerialWorker {

    public static void main(String[] args) throws Exception {
        new SerialWorker().start();
    }

    public void start() throws IOException, InterruptedException {
        String port = "COM4";
        int baudRate = 9600;
        NRSerialPort serial = new NRSerialPort(port, baudRate);
        serial.connect();

        //DataInputStream ins = new DataInputStream(serial.getInputStream());
        //DataOutputStream outs = new DataOutputStream(serial.getOutputStream());

        //outs.write(b);
        //byte b = ins.read();

        BufferedOutputStream out = IOUtils.buffer(serial.getOutputStream());
        byte [] bytes = (
                "The java.util.zip.Deflater.deflate(byte[] b) method https://www.tutorialspoint.com/javazip/javazip_deflater_deflate.htm T&(^&(*^&(U*(_"
                ).getBytes();

        System.out.println("Input size [" + bytes.length + "] bytes");
        Deflater deflater = new Deflater();
        deflater.setInput(bytes);
        deflater.finish();
        byte[] compressed = new byte[1024];
        int compressedDataLength = deflater.deflate(compressed);
        System.out.println("Compressed size [" + compressedDataLength + "] bytes");

        //LocalDate start = LocalDate.now();
        for (int i = 0; i < 3; i++) {
            long start = System.currentTimeMillis();
            out.write(compressed, 0, compressedDataLength);
            out.flush();
            System.out.println("Sent [" + compressedDataLength + "] bytes in [" + (System.currentTimeMillis() - start) + "] ms");
            Thread.sleep(1000);
        }

        serial.disconnect();
    }

}
