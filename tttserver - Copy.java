import java.io.*;
import java.net.*;
import java.util.*;

public class tttserver {

    public static int PORT = 3116;
    public static Boolean LISTEN = true;
    public static HashMap <Integer, String> sessionsAndClients = new HashMap<>();
    public static HashMap <String, Socket> clientSockets = new HashMap<>();
    public static int sessionID = 1;
    public static final int version = 1;
    public static HashMap <Integer, String[]> games = new HashMap<>();
    public static int gameID = 1;

// Open TCP Socket and wait for connection
// Open UDP socket and wait for connection

    public static void main(String[] args) throws Exception {
        ServerSocket servSockT = new ServerSocket(PORT);
        DatagramSocket servSockU = new DatagramSocket(PORT);

        Socket socket = null;

        while((socket = servSockT.accept()) != null) {

        Thread t1 = new Thread(new MyRunnableTCP(socket));
        //t1.setDaemon(true);
        //Thread t2 = new Thread(new MyRunnableUDP(servSockU));
        //t2.setDaemon(true);

        t1.start();
        //t2.start();

        //t1.join();
        //t2.join();

        }


        servSockU.close();
        servSockT.close();
    }

    public static class MyRunnableTCP implements Runnable {
        private Socket sock;
        
        public MyRunnableTCP(Socket s) {
            this.sock = s;
        }
    
        public void run() {
            try {

                System.out.println("Listening for TCP connection on port " + 3116);
                System.out.println("Connection Successful!");

                while (true) {

                    BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

                    StringBuilder inputData = new StringBuilder();

                    String line;

                    while (in.ready()) {
                        inputData.append((char) in.read());
                    }

                    // Save or process the received data as needed
                    String savedData = inputData.toString().trim();

                    if (!savedData.trim().isEmpty()) {

                    System.out.println("Received data: " + savedData);

                    // Creates output object with Socket
                    PrintWriter out = new PrintWriter(sock.getOutputStream(), true);

                    // Split savedData and grab first word
                    String[] message = savedData.split(" ");

                    if (message[0].equals("HELO")) {
                        if (Integer.parseInt(message[1]) != 1){
                            System.out.println(message[1]);
                            out.println("Error: Invalid version");
                        }
                        System.out.println("Invoking handleClient");
                        startSession(message, sock, out);
                    } else if (message[0].equals("CREA")){
                        System.out.println("Invoking createGame");
                        createGame(message, sock, out);
                    } else if (message[0].equals("LIST")){
                        System.out.println("Invokign listGames");
                        listGames(message, sock, out);
                    } else if (message[0].equals("STAT")){
                        System.out.println("Invoking gameStatus");
                        gameStatus(message, sock, out);
                    } else if (message[0].equals("MOVE")){
                        System.out.println("Invoking makeMove");
                        makeMove(message, sock, out);
                    }
                    
                    // Terminate the loop if "exit" is received
                    if (savedData.equals("exit")) {
                        LISTEN = false;
                        break;
                    }
                }
            }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        // Handles response to client
        public static void startSession(String[] message, Socket sock, PrintWriter out) {
            String clientID = message[2];
            String acknowledgment = "SESS " + version + " " + sessionID;
            String value = 
            sessionsAndClients.put(sessionID, clientID);
            sessionID++;

            clientSockets.put(clientID, sock);

            try {
                out.println(acknowledgment);
                System.out.println("Sent " + acknowledgment);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        public static void createGame(String[] message, Socket sock, PrintWriter out){
            String clientID = message[1];
            String[] gameElements = {clientID, null, "|*|*|*|*|*|*|*|*|*|"};
            games.put(gameID, gameElements);
            gameID++;
            String response = "JOND " + clientID + " " + gameID;

            try {
                out.println(response);
                System.out.println("Sent " + response);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        public static void listGames(String[] message, Socket sock, PrintWriter out){
            String gameList = "";
            if (message[1].equals("ALL")){
                Set<Integer> keys = games.keySet();
                for(Integer key: keys){
                    gameList = gameList + (key + " ");
                }
            } else if (message[1].equals("CURR")){
                for (Map.Entry<Integer, String[]> entry : games.entrySet()){
                    String[] gameState = entry.getValue();
                    if (gameState[1] == null){
                        gameList = gameList + (entry.getKey() + " ");
                    }
                }
            } else {
                gameList = "Error: Please choose CURR or ALL";
            }

            try {
                out.println(gameList);
                System.out.println("Sent " + gameList);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        public static void gameStatus(String[] message, Socket sock, PrintWriter out){
            String gameID = message[1];
            String[] currGame = games.get(gameID);
            String response = "BORD "+ gameID + message[0];
            if(currGame[1] != null){
                response = response + (message[1] + " " + message[2] + " " + message[3]);
            }

            try {
                out.println(response);
                System.out.println("Sent " + response);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static void makeMove(String[] message, Socket sock, PrintWriter out){
            // Assuming structure: MOVE <game ID> <position> <client ID>
            String response = "";
            Boolean wasSuccess = null;
            String opp = "";
            String playerX = "";
            String playerO = "";
            char playerIcon = 'X';

            if(!games.containsKey(message[1])){
                response = "Error: Game not found.";
            // Checks if the client's ID matches the IDs of the players in the game map
            } else if(!message[3].equals((games.get(message[1]))[0]) || !message[3].equals((games.get(message[1]))[1])){
                response = "Error: You are not a player of this game.";
            } else {
                String board = (games.get(message[1]))[3];

                // Checks if player is X or O based on position in map value
                if (message[3].equals(games.get(message[1])[1])){
                    playerIcon = 'O';
                    playerO = message[3];
                    playerX = games.get(message[1])[0];
                    opp = playerX;
                } else {
                    playerX = message[3];
                    playerO = games.get(message[1])[1];
                    opp = playerO;
                }

                int index = 0;

                // Checks if message was sent using coordinate pair for position
                if(message[2].contains(",")){
                    String[] coordinates = message[2].split(",");

                    int row = Integer.parseInt(coordinates[0]);
                    int column = Integer.parseInt(coordinates[1]);

                    index = (3 - row) * 3 + (column - 1);
                    
                } else {
                    index = Integer.parseInt(message[2]) * 2;
                }
                // checks if space on board is available for move.
                if(games.get(message[1])[3] != "*"){
                    response = "Error: Not a valid move";

                } else {
                    // Updates the game status in the game map's value
                    StringBuilder boardBuilder = new StringBuilder(board);
                    boardBuilder.setCharAt(index, playerIcon);
                    (games.get(message[1]))[3] = boardBuilder.toString();

                    // Constructs response
                    if(playerIcon == 'X'){
                        response = "BORD " + message[1] + " " + playerX + " " + 
                        playerO + " " + playerO; 
                    } else {
                        response = "BORD " + message[1] + " " + playerX + " " + 
                        playerO + " " + playerX; 
                    }
                }
            }
            try {
                out.println(response);
                System.out.println("Sent " + response);

            } catch (Exception e) {
                e.printStackTrace();
            }

            if(wasSuccess){
                if(playerIcon == 'X'){
                    response = "YRMV " + " " + playerO;
                } else {
                    response = "YRMV " + " " + playerX;
                }
                try {
                    Socket oppSocket = clientSockets.get(opp);
                    PrintWriter oppOut = new PrintWriter(oppSocket.getOutputStream(), true);
                    out.println(response);
                    oppOut.println(response);

                    System.out.println("Sent " + response);
    
                } catch (Exception e) {
                    e.printStackTrace();
                } 
            }
        }


    }

    // GDBY, JOIN, LIST, MOVE, QUIT, STAT

    // Zach:
        // LIST done
        // MOVE
        // STAT done

    // Vic:
        // GDBY
        // QUIT
        // JOIN

    public static class MyRunnableUDP implements Runnable {
        private DatagramSocket soc;
        public MyRunnableUDP (DatagramSocket s) {
            this.soc = s;
        }

        public void run() {
            try {
                System.out.println("Listening for UDP connection on port " + 3116);
                byte[] b = new byte[256];
                DatagramPacket pack = new DatagramPacket(b, b.length);
                soc.receive(pack);
                InetAddress address = pack.getAddress();
                int port = pack.getPort();
                String message = new String(pack.getData(), 0, pack.getLength());
                byte[] response = message.getBytes(); // message will be the response
                DatagramPacket newP = new DatagramPacket(response, response.length, address, port);
                soc.send(newP);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}

   // Send Acknowledgment with "SESS <version> <sess ID>""
    // sessCount = 1;
    // sessCount++;
    // private static void handleClient(String[] message) {
    //     String clientID = message[2];
    
    //     try {
    //         String acknowledgment = "ACKN " + clientID;
    
    //         //Socket clientSocket = new Socket("localhost", PORT);
    
    //         PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
    
    //         out.println(acknowledgment);
    
    //         out.close();
    //         clientSocket.close();
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    // }

// Client will connect with hello message "HELO <version number> <client ID>"
// ex: "HELO 1 zach"
    // Can probably take in command line arguments for the identifier/username

// Server responds with receipt acknowledging the client "SESS <version number> <session ID>"
    // We could implement some sort of counter so each time a session is created it will use that "count" value and add 1 for the next 1

// Client has two options for playing
    // Create a new game:
        // "CREA <client ID>" to create a new game
        // Server will respond to client with "JOND <client ID> <game ID>"
    // Find a game:
        // "LIST CURR" Server will send a list of all games currently open to join
        // "LIST ALL" Server will send a list of all games running on the server
            // Server responds with "GAMS <List of games>
            // We could store games in like a hashmap, where the key is if it is currently open or not and the value is the game ID
            // Client will then pick a game and send "JOIN <game ID>" and server will respond with "JOND <client ID> <game ID>"

// Server needs to decide who goes first in game (can use math.rand)
// 

