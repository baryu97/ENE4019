import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.io.InputStream;

public class ServerThread extends Thread {
    static HashMap<String, TreeMap<String,Socket>> users = new HashMap<>();
    static HashMap<Socket,Socket> temp_bucket = new HashMap<>();
    static HashMap<String, File> files = new HashMap<>();
    Socket chatSocket;
    Socket fileSocket;
    String roomName="";
    String nick="";
    public ServerThread(Socket chatSocket, Socket fileSocket) {
        this.chatSocket = chatSocket;
        this.fileSocket = fileSocket;
        temp_bucket.put(chatSocket,chatSocket);
    }
    void remove_socket(){
        if (roomName.equals("") && nick.equals("")){
            temp_bucket.remove(chatSocket);
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
            OutputStream out = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out, "UTF-8")), true);
            writer.println(string);
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    void send_user_message(Socket socket, String string){
        send_message(socket, nick + ": " + string);
    }
    boolean create_room(){
        TreeMap<String, Socket> treeMap = new TreeMap<>();
        treeMap.put(nick,chatSocket);
        users.put(roomName,treeMap);
        String str = nick + " join into the chatting room.";
        send_message(chatSocket,str);
        temp_bucket.remove(chatSocket);
        return true;
    }
    boolean join_room(){
        TreeMap<String, Socket> treeMap = users.get(roomName);
        if (treeMap.containsKey(nick)){
            nick = "";
            roomName = "";
            return false;
        }
        treeMap.put(nick,chatSocket);
        users.put(roomName,treeMap);
        String str = nick + " join into the chatting room.";
        for (Map.Entry<String,Socket> entry : users.get(roomName).entrySet()) {
            send_message(entry.getValue(),str);
        }
        temp_bucket.remove(chatSocket);
        return true;
    }
    boolean exit_room(){
        TreeMap<String, Socket> treeMap = users.get(roomName);
        treeMap.remove(nick);
        nick = "";
        roomName = "";
        temp_bucket.put(chatSocket,chatSocket);
        return true;
    }

    @Override
    public void run() {
        try {
            enum State { WAIT, CHAT }
            State state = State.WAIT;
            while (true) {
                String str;
                InputStream input = chatSocket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input,"UTF-8")); // 읽기
                if ((str = reader.readLine()) != null) {
                    if (str.startsWith("#GET ")){
                        OutputStream out = fileSocket.getOutputStream();
                        DataOutputStream dout = new DataOutputStream(fileSocket.getOutputStream());
                        String filename = str.split(" ")[1];
                        File file = files.get(filename);
                        FileInputStream fileIn = new FileInputStream(file);
                        byte[] buffer = new byte[1024];
                        int len;
                        int data=0;

                        while((len = fileIn.read(buffer))>0){
                            data++;
                        }

                        send_message(chatSocket,str);
                        fileIn.close();
                        fileIn = new FileInputStream(file);
                        dout.writeInt(data);

                        for(int i = 1;i <= data;++i){
//                            if (i % 64 == 0){
//                                send_message(chatSocket,"#");
//                            }
                            len = fileIn.read(buffer);
                            out.write(buffer,0,len);
                        }
                    } else if (str.startsWith("#PUT ")) {
                        String filename = str.split(" ")[1];
                        InputStream in = fileSocket.getInputStream();
                        DataInputStream din = new DataInputStream(in);
                        int data = din.readInt();
                        File file = new File(filename);
                        FileOutputStream out = new FileOutputStream(file);
                        byte[] buffer = new byte[1024];
                        int len;
                        for (int i = 1;i <= data;++i) {
//                            if (i % 64 == 0){
//                                send_message(chatSocket,"#");
//                            }
                            len = in.read(buffer);
                            out.write(buffer,0,len);
                        }
                        out.flush();
                        out.close();
                        files.put(filename,file);
                    }
                    if (state.equals(State.WAIT)) {
//                        if (str.equals("#QUIT")) {
////                            send_message(chatSocket, str);
//                            remove_socket();
//                            chatSocket.close();
//                            break;
//                        }
                        if (str.startsWith("#JOIN ")) {
                            if (str.split(" ").length < 3)
                                continue;
                            roomName = str.split(" ")[1];
                            nick = str.split(" ")[2];
                            if (nick.equals("") || roomName.equals("")) {
                                nick = "";
                                roomName = "";
                                continue;
                            }
                            if (users.containsKey(roomName) && join_room()) {
                                state = State.CHAT;
                            } else {
                                send_message(chatSocket,"Fail");
                            }
                        }
                        if (str.startsWith("#CREATE ")) {
                            if (str.split(" ").length < 3)
                                continue;
                            roomName = str.split(" ")[1];
                            nick = str.split(" ")[2];
                            if (nick.equals("") || roomName.equals("")) {
                                nick = "";
                                roomName = "";
                                continue;
                            }
                            if (users.containsKey(roomName)) {
                                send_message(chatSocket,"Fail");
                            } else {
                                create_room();
                                state = State.CHAT;
                            }
                        }
                    }
                    else if (state.equals(State.CHAT)) {
                        if (str.startsWith("#")){
                            if (str.equals("#EXIT")) {
                                exit_room();
                                send_message(chatSocket,"Exit successful.");
                                state = State.WAIT;
                            }
                            if (str.equals("#STATUS")){
                                StringBuilder msg = new StringBuilder("Room name: " + roomName + ", Users: ");
                                for(String s : users.get(roomName).keySet()){
                                    msg.append(s).append(", ");
                                }
                                send_message(chatSocket, msg.substring(0,msg.length()-2));
                            }
                            continue;
                        }
                        for (Map.Entry<String,Socket> entry : users.get(roomName).entrySet()) {
                            if (entry.getValue() != chatSocket) {
                                send_user_message(entry.getValue(), str);
                            }
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
            ServerSocket chatSocket = new ServerSocket(port1);
            ServerSocket fileSocket = new ServerSocket(port2);
            System.out.println("Server Open");
            while (true) {
                Socket userChat = chatSocket.accept();
                Socket userFile = fileSocket.accept();
//                System.out.println("Client join " + userChat.getLocalAddress() + " : " + userChat.getLocalPort());
                Thread serverThread = new ServerThread(userChat,userFile);
                serverThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}