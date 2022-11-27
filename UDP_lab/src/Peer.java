import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.security.MessageDigest;

public class Peer{
    public static byte[] sha256(String str) throws NoSuchAlgorithmException {
        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        hash.update(str.getBytes());
        return hash.digest();
    }
    public static void main(String[] args) throws Exception{
        int port = Integer.parseInt(args[0]);
        Scanner scanner = new Scanner(System.in);
//        int port = scanner.nextInt();
        String str;
        String[] strArr;
        do {
            str = scanner.nextLine();
            strArr = str.split(" ");
        }while (!strArr[0].equals("#JOIN") || strArr.length < 3);

        byte[] hashVal = sha256(strArr[1]);
        byte[] address = new byte[4];

        address[0] = (byte) 225;
        address[1] = hashVal[29];
        address[2] = hashVal[30];
        address[3] = hashVal[31];

        InetAddress IPAddress = InetAddress.getByAddress(address);
        MulticastSocket ms = new MulticastSocket(port);

        ms.joinGroup(IPAddress);

        Send send = new Send(ms,IPAddress,strArr[2],port);
        Receive receive = new Receive(ms);

        send.start();
        receive.start();
    }
}

class Receive extends Thread{
    MulticastSocket ms;
    public Receive(MulticastSocket ms){
        this.ms = ms;
    }
    public void run(){
        while (true){
            try {
                byte[] receiveData = new byte[512];

                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                ms.receive(receivePacket);

                System.out.println(new String(receivePacket.getData()).trim());
            } catch(SocketException s) {
              break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class Send extends Thread{
    MulticastSocket ms;
    InetAddress IPAddress;
    String userName;
    int portNum;
    public Send(MulticastSocket ms, InetAddress IPAddress ,String userName, int portNum){
        this.ms = ms;
        this.userName = userName;
        this.IPAddress = IPAddress;
        this.portNum = portNum;
    }
    public void run(){
        while (true) {
            try {
                Scanner sc = new Scanner(System.in);
                String str = sc.nextLine();
                if (str.charAt(0) == '#') {
                    String[] strArr = str.split(" ");
                    if (strArr[0].equals("#EXIT")){
                        ms.leaveGroup(IPAddress);
                        ms.close();
                        break;
                    }
                    continue;
                }

                str = userName + ": " + str;

                while (str.length() > 512){
                    String tmpStr = str.substring(0,512);
                    DatagramPacket dp = new DatagramPacket(tmpStr.getBytes(), tmpStr.getBytes().length, IPAddress, portNum);
                    ms.send(dp);
                    str = userName + ": " + str.substring(512);
                }

                DatagramPacket dp = new DatagramPacket(str.getBytes(), str.getBytes().length, IPAddress, portNum);
                ms.send(dp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
