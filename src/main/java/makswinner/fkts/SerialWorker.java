package makswinner.fkts;

import gnu.io.NRSerialPort;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SerialWorker {

    public void start() throws IOException {
        String port = "COM3";
        int baudRate = 115200;
        NRSerialPort serial = new NRSerialPort(port, baudRate);
        serial.connect();

        DataInputStream ins = new DataInputStream(serial.getInputStream());
        DataOutputStream outs = new DataOutputStream(serial.getOutputStream());

        byte b = ins.read();
        outs.write(b);

        serial.disconnect();
    }

}
