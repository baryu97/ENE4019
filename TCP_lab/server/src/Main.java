import javax.naming.ldap.SortKey;
import javax.swing.*;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class Main extends Thread {
    static HashMap<String, TreeMap<String,Socket>> users = new HashMap<>();
    static HashMap<Socket,Socket> temp_bucket = new HashMap<>();
    Socket socket;
    String roomName="";
    String nick="";
    public Main(Socket socket) {
        this.socket = socket;
        temp_bucket.put(socket,socket);
    }
    void remove_socket(){
        if (roomName.equals("") && nick.equals("")){
            temp_bucket.remove(socket);
        }
        else {
            System.out.println("ERROR");
        }
//        else {
//            TreeMap<String, Socket> treeMap = users.get(roomName);
//            treeMap.remove(nick);
//            users.put(roomName,treeMap);
//        }
    }
    void send_message(Socket socket, String string){
        try {
            OutputStream out = socket.getOutputStream(); // 쓰기
            PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out, "UTF-8")), true);
            writer.println(string);
            writer.flush();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    boolean join_room(){
        TreeMap<String, Socket> treeMap = users.get(roomName);
        if (treeMap.containsKey(nick)){
            nick = "";
            roomName = "";
            return false;
        }
        treeMap.put(nick,socket);
        users.put(roomName,treeMap);
        String str = nick + " join into the chatting room.";
        for (Map.Entry<String,Socket> entry : users.get(roomName).entrySet()) {
            send_message(entry.getValue(),str);
        }
        temp_bucket.remove(socket);
        return true;
    }
    boolean exit_room(){
        TreeMap<String, Socket> treeMap = users.get(roomName);
        treeMap.remove(nick);
        nick = "";
        roomName = "";
        temp_bucket.put(socket,socket);
        return true;
    }

    @Override
    public void run() {
        try {
            enum State { WAIT, CHAT }
            State state = State.WAIT;
            while (true) {
                String str;
                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input,"UTF-8")); // 읽기
                if ((str = reader.readLine()) != null) {
                    if (state.equals(State.WAIT)) {
                        if (str.equals("#QUIT")) {
                            send_message(socket, str);
                            remove_socket();
                            break;
                        }
                        if (str.startsWith("#JOIN ")) {
                            roomName = str.split(" ")[1];
                            nick = str.split(" ")[2];
                            if (nick.equals("") || roomName.equals(""))
                                continue;
                            if (users.containsKey(roomName)) {
                                join_room();
                                state = State.CHAT;
                            } else {
                                send_message(socket,"Fail");
                            }
                        }
                        if (str.startsWith("#CREATE ")) {
                            roomName = str.split(" ")[1];
                            nick = str.split(" ")[2];
                            if (nick.equals("") || roomName.equals(""))
                                continue;
                            if (users.containsKey(roomName)) {
                                send_message(socket,"Fail");
                            } else {
                                join_room();
                                state = State.CHAT;
                            }
                        }
                    } else if (state.equals(State.CHAT)) {
                        if (str.startsWith("#")){
                            if (str.equals("#EXIT")) {
                                exit_room();
                                state = State.WAIT;
                            }
                            continue;
                        }
                        for (Map.Entry<String,Socket> entry : users.get(roomName).entrySet()) {
                            send_message(entry.getValue(),str);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        int port1 = Integer.parseInt(args[0]), port2 = Integer.parseInt(args[1]);

        try {
            ServerSocket ss = new ServerSocket(port1);
            System.out.println("Server Open");
            while (true) {
                Socket user = ss.accept();
                System.out.println("Client join " + user.getLocalAddress() + " : " + user.getLocalPort());
                Thread serverThread = new Main(user);
                serverThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}