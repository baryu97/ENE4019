import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        String serverAddress = args[0];
        int port1 = Integer.parseInt(args[1]), port2 = Integer.parseInt(args[2]);
        try {
            Socket chatSocket = new Socket(serverAddress, port1);
            Socket fileSocket = new Socket(serverAddress, port2);
            System.out.println("Server access successful");
            Sender sThread = new Sender(chatSocket,fileSocket);
            Receiver rThread = new Receiver(chatSocket,fileSocket);
            sThread.start();
            rThread.start();
            sThread.join();
            rThread.join();
            chatSocket.close();
            fileSocket.close();
        } catch (Exception e) {
            System.out.println("Server access failed");
        }

    }
}
class Receiver extends Thread {
    Socket chatSocket;
    Socket fileSocket;

    public Receiver(Socket chatSocket,Socket fileSocket) {
        this.fileSocket = fileSocket;
        this.chatSocket = chatSocket;
    }

    @Override
    public void run() {
        InputStream chatin;
        BufferedReader reader;
        try {
            while (true) {
                String s = null;
                chatin = chatSocket.getInputStream();
                reader = new BufferedReader(new InputStreamReader(chatin, "UTF-8"));
                if ((s = reader.readLine()) != null) {
                    if (s.startsWith("#GET ")){
                        String filename = s.split(" ")[1];
                        InputStream in = fileSocket.getInputStream();
                        DataInputStream din = new DataInputStream(in);
                        int data = din.readInt();
                        File file = new File(filename);
                        FileOutputStream out = new FileOutputStream(file);
                        byte[] buffer = new byte[1024];
                        int len;
                        for(int i = 1;i <= data;++i){
                            if (i % 64 == 0){
                                System.out.print("#");
                            }
                            len = in.read(buffer);
                            out.write(buffer,0,len);
                        }
                        System.out.println("");
                        out.flush();
                        out.close();
                    } else {
                        System.out.println(s);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Sender extends Thread {
    Socket chatSocket;
    Socket fileSocket;
    Scanner scanner;

    public Sender(Socket chatSocket,Socket fileSocket) {
        scanner = new Scanner(System.in);
        this.fileSocket = fileSocket;
        this.chatSocket = chatSocket;
    }

    @Override
    public void run() {
        try {
            OutputStream out = chatSocket.getOutputStream();
            PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out, "UTF-8")), true);
            while (true) {
                String s = scanner.nextLine();
                if (s.startsWith("#PUT ")){
                    OutputStream fout = fileSocket.getOutputStream();
                    DataOutputStream dout = new DataOutputStream(fileSocket.getOutputStream());
                    String filename = s.split(" ")[1];
                    FileInputStream fileIn = new FileInputStream(new File(filename));
                    byte[] buffer = new byte[1024];
                    int len;
                    int data=0;

                    while((len = fileIn.read(buffer))>0){
                        data++;
                    }

                    writer.println(s);
                    fileIn.close();
                    fileIn = new FileInputStream(new File(filename));
                    dout.writeInt(data);

                    for(int i = 1;i <= data;++i){
                        if (i % 64 == 0){
                            System.out.print("#");
                        }
                        len = fileIn.read(buffer);
                        fout.write(buffer,0,len);
                    }
                    System.out.println("");
                    continue;
                }
                writer.println(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}