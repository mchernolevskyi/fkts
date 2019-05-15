package makswinner.fkts;

import gnu.io.NRSerialPort;
import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;

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
        byte [] bytes = "<1234567890123456>".getBytes();

        //LocalDate start = LocalDate.now();
        for (int i = 0; i < 10; i++) {
            long start = System.currentTimeMillis();
            out.write(bytes);
            out.flush();
            System.out.println("Sent [" + bytes.length + "] bytes in [" + (System.currentTimeMillis() - start) + "] ms");
            Thread.sleep(20000);
        }

        serial.disconnect();
    }

}
