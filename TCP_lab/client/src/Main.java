import java.net.Socket;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class Main {
    public static void main(String[] args) {
        String serverAddress = args[0];
        int port1 = Integer.parseInt(args[1]), port2 = Integer.parseInt(args[2]);
        try {
            Socket socket = new Socket(serverAddress, port1);
            System.out.println("Server access successful");
            Sender sThread = new Sender(socket);
            Receiver rThread = new Receiver(socket);
            sThread.start();
            rThread.start();
            sThread.join();
            rThread.join();
            socket.close();
        } catch (Exception e) {
            System.out.println("Server access failed");
        }

    }
}

class Receiver extends Thread {
    Socket socket;

    public Receiver(Socket s) {
        socket = s;
    }

    @Override
    public void run() {
        InputStream in;
        BufferedReader reader;
        try {
            while (true) {
                String s = null;
                in = socket.getInputStream();
                reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                if ((s = reader.readLine()) != null) {
                    System.out.println(s);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Sender extends Thread {
    Socket socket;
    Scanner scanner;

    public Sender(Socket s) {
        scanner = new Scanner(System.in);
        socket = s;
    }

    @Override
    public void run() {
        try {
            OutputStream out = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out, "UTF-8")), true);
            while (true) {
                String s = scanner.nextLine();
                writer.println(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}