package swarmBots;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import common.Coord;
import common.Group;
import common.MapTile;
import common.ScanMap;
import enums.Science;
import enums.Terrain;


//Making an addition to this file to check whether a remote alternat push will change it

// rearanged the 2nd and 3rd line in the following comment

/**
 * The seed that this program is built on is a chat program example found here:
 * publishing their code examples
 * * http://cs.lmu.edu/~ray/notes/javanetexamples/ Many thanks to the authors for
 */

public class ROVER_09 {

	BufferedReader in;
	PrintWriter out;
	String rovername;
	ScanMap scanMap;
	int sleepTime;
	String SERVER_ADDRESS = "localhost";
	static final int PORT_ADDRESS = 9537;
 	// all the sockets of blue team - output
    List<Socket> outputSockets = new ArrayList<Socket>();

    // objects contains each rover IP, port, and name
    List<Group> blue = new ArrayList<Group>();

    // every science detected will be added in to this set
    Set<Coord> science_discovered = new HashSet<Coord>();

    // this set contains all the science the ROVERED has shared
    // thus whatever thats in science_collection that is not in display_science
    // are "new" and "unshared"
    Set<Coord> displayed_science = new HashSet<Coord>();

    // ROVER current location
    Coord roverLoc;

    // Your ROVER is going to listen for connection with this
    ServerSocket listenSocket;
	int currentDirection = 1;//move east at first
	
    Coord cc = null;
    HashSet<Coord> science_collection = new HashSet<Coord>();
//    HashSet<Coord> displayed_science = new HashSet<Coord>();

	public ROVER_09() {
		// constructor
		System.out.println("ROVER_09 rover object constructed");
		rovername = "ROVER_09";
		SERVER_ADDRESS = "localhost";
		// this should be a safe but slow timer value
		sleepTime = 300; // in milliseconds - smaller is faster, but the server will cut connection if it is too small
	}
	
	public ROVER_09(String serverAddress) {
		// constructor
		System.out.println("ROVER_09 rover object constructed");
		rovername = "ROVER_09";
		SERVER_ADDRESS = serverAddress;
		sleepTime = 200; // in milliseconds - smaller is faster, but the server will cut connection if it is too small
	}
	/**
     * Try to connect each socket on a separate thread. Will try until it works.
     * When socket is created, save it to a LIST
     * 
     * @author  
     *
     */
    class RoverComm implements Runnable {

        String ip;
        int port;
        Socket socket;

        public RoverComm(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        public void run() {
            do {
                try {
                    socket = new Socket(ip, port);
                } catch (UnknownHostException e) {

                } catch (IOException e) {

                }
            } while (socket == null);
            
            outputSockets.add(socket);
            System.out.println(socket.getPort() + " " + socket.getInetAddress());
        }

    }
    /**
     * add all the group's rover into a LIST
     */
    public void initConnection() {
        // dummy value # 1
        blue.add(new Group("Dummy Group #1", "localhost", 53799));

        // blue rooster
        blue.add(new Group("GROUP_01", "localhost", 53701));
        blue.add(new Group("GROUP_02", "localhost", 53702));
        blue.add(new Group("GROUP_03", "localhost", 53703));
        blue.add(new Group("GROUP_04", "localhost", 53704));
        blue.add(new Group("GROUP_05", "localhost", 53705));
        blue.add(new Group("GROUP_06", "localhost", 53706));
        blue.add(new Group("GROUP_07", "localhost", 53707));
        blue.add(new Group("GROUP_08", "localhost", 53708));
    }
    /**
     * Create and start a thread for each ROVER connected to you.
     * 
     * @throws IOException
     * @author  
     */
    private void startServer() throws IOException {

        // create a thread that waits for client to connect to 
        new Thread(() -> {
            while (true) {
                try {
                    // wait for a connection
                    Socket connectionSocket = listenSocket.accept();

                    // once there is a connection, serve them on thread
                    new Thread(new RoverHandler(connectionSocket)).start();
                    

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    /**
     * When any ROVER discovered science, it will write a message to your all ROVERS.
     * That message will be "sent" here. This block of code will read whatever
     * written to you. Your job is to use the data to tell your rover to go pick
     * up that science.
     * 
     * @author  
     *
     */
    class RoverHandler implements Runnable {
        Socket roverSocket;

        public RoverHandler(Socket socket) {
            this.roverSocket = socket;
        }

        @Override
        public void run() {

            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(roverSocket.getInputStream()));

                while (true) {

                    String line = input.readLine();
                    // protocol: ROCK CRYSTAL 25 30
                    System.out.println("NEW MESSAGE: " + line);
        
                    /*
                     * IMPLEMENT YOUR CODE HERE
                     * WHAT DO YOU WANT TO DO WITH THE DATA?
                     */
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }
	/**
	 * Connects to the server then enters the processing loop.
	 */
	private void run() throws IOException, InterruptedException {

		// Make connection and initialize streams
		//TODO - need to close this socket
		Socket socket = new Socket(SERVER_ADDRESS, PORT_ADDRESS); // set port here
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);

		//Gson gson = new GsonBuilder().setPrettyPrinting().create();

 		/*
         * This is the server that you are listening on. 
         * This means when we want to contact you, we use that port number.
         */
        listenSocket = new ServerSocket(53704);
        startServer();
        /*
         * connect to all the ROVERS on a separate thread
         */
        initConnection();
        for (Group group : blue) {
            new Thread(new RoverComm(group.ip, group.port)).start();
        }
        
		// Process all messages from server, wait until server requests Rover ID
		// name
		while (true) {
			String line = in.readLine();
			if (line.startsWith("SUBMITNAME")) {
				out.println(rovername); // This sets the name of this instance
										// of a swarmBot for identifying the
										// thread to the server
				break;
			}
		}

		// ******** Rover logic *********
		// int cnt=0;
		String line = "";

		int counter = 0;
		
		boolean goingSouth = false;
		boolean goingEast = false;
		
		boolean stuck = false; // just means it did not change locations between requests,
								// could be velocity limit or obstruction etc.
		boolean blocked = false;

		String[] cardinals = new String[4];
		cardinals[0] = "N";
		cardinals[1] = "E";
		cardinals[2] = "S";
		cardinals[3] = "W";

		String currentDir = cardinals[0];
		Coord currentLoc = null;
		Coord previousLoc = null;
		

		// start Rover controller process
		while (true) {

			// currently the requirements allow sensor calls to be made with no
			// simulated resource cost
			
			
			// **** location call ****
			out.println("LOC");
			line = in.readLine();
            if (line == null) {
            	System.out.println("ROVER_09 check connection to server");
            	line = "";
            }
			if (line.startsWith("LOC")) {
				// loc = line.substring(4);
				currentLoc = extractLOC(line);
			}
			System.out.println("ROVER_09 currentLoc at start: " + currentLoc);
			
			// after getting location set previous equal current to be able to check for stuckness and blocked later
			previousLoc = currentLoc;
			
			
			
			// **** get equipment listing ****			
			ArrayList<String> equipment = new ArrayList<String>();
			equipment = getEquipment();
			//System.out.println("ROVER_09 equipment list results drive " + equipment.get(0));
			System.out.println("ROVER_09 equipment list results " + equipment + "\n");
			
	

			// ***** do a SCAN *****
			//System.out.println("ROVER_09 sending SCAN request");
			this.doScan();
			scanMap.debugPrintMap();
			
			
			MapTile[][] scanMapTiles = scanMap.getScanMap();
			int centerIndex = (scanMap.getEdgeSize() - 1)/2;
        	MapTile tile = scanMapTiles[centerIndex][centerIndex];
            Science science = tile.getScience();
        	if(science.equals(Science.ORGANIC)){
    			System.out.println("ROVER_09 is requesting GATHER organic!");
    			out.println("GATHER");
        	}
        	Thread.sleep(200);
        	int m,n=0;
        	if((scanMapTiles[centerIndex-1][centerIndex].getScience()==Science.ORGANIC&&scanMapTiles[centerIndex-1][centerIndex].getTerrain()==Terrain.ROCK)
        			||(scanMapTiles[centerIndex-2][centerIndex].getScience()==Science.ORGANIC&&scanMapTiles[centerIndex-2][centerIndex].getTerrain()==Terrain.ROCK&&
        			!isBlocked(scanMapTiles[centerIndex-1][centerIndex]))){
				currentDirection = 3;
				basicMove(currentDirection, scanMapTiles, centerIndex);
        	}
        	if((scanMapTiles[centerIndex+1][centerIndex].getScience()==Science.ORGANIC&&scanMapTiles[centerIndex+1][centerIndex].getTerrain()==Terrain.ROCK)
        			||(scanMapTiles[centerIndex+2][centerIndex].getScience()==Science.ORGANIC&&scanMapTiles[centerIndex+2][centerIndex].getTerrain()==Terrain.ROCK&&
        			!isBlocked(scanMapTiles[centerIndex+1][centerIndex]))){
				currentDirection = 1;
				basicMove(currentDirection, scanMapTiles, centerIndex);
        	}
        	if((scanMapTiles[centerIndex][centerIndex-1].getScience()==Science.ORGANIC&&scanMapTiles[centerIndex][centerIndex-1].getTerrain()==Terrain.ROCK)
        			||(scanMapTiles[centerIndex][centerIndex-2].getScience()==Science.ORGANIC&&scanMapTiles[centerIndex][centerIndex-2].getTerrain()==Terrain.ROCK&&
        			!isBlocked(scanMapTiles[centerIndex][centerIndex-1]))){
				currentDirection = 4;
				basicMove(currentDirection, scanMapTiles, centerIndex);
        	}
        	if((scanMapTiles[centerIndex][centerIndex+1].getScience()==Science.ORGANIC&&scanMapTiles[centerIndex][centerIndex+1].getTerrain()==Terrain.ROCK)
        			||(scanMapTiles[centerIndex][centerIndex+2].getScience()==Science.ORGANIC&&scanMapTiles[centerIndex][centerIndex+2].getTerrain()==Terrain.ROCK&&
        			!isBlocked(scanMapTiles[centerIndex][centerIndex+1]))){
				currentDirection = 2;
				basicMove(currentDirection, scanMapTiles, centerIndex);
        	}
        	
            basicMove(currentDirection, scanMapTiles, centerIndex);
            

    		//System.out.println("It's moving to : " + currentDirection);
            //shareScience();
			
			// MOVING

			// another call for current location
			out.println("LOC");
			line = in.readLine();
			if (line.startsWith("LOC")) {
				currentLoc = extractLOC(line);
			}

			System.out.println("ROVER_09 currentLoc after recheck: " + currentLoc);
			System.out.println("ROVER_09 previousLoc: " + previousLoc);

			// test for stuckness
			stuck = currentLoc.equals(previousLoc);

			System.out.println("ROVER_09 stuck test " + stuck);
			System.out.println("ROVER_09 blocked test " + blocked);

			
			Thread.sleep(sleepTime);
			
			System.out.println("ROVER_09 ------------ bottom process control --------------"); 

		}

	}

	// ################ Support Methods ###########################
	/**
     * iterate through a scan map to find a tile with organic. get the
     * adjusted (absolute) coordinate of the tile and added into a hash set
     * 
     * @param scanMapTiles
     * @author  
     */
//    private void detectOraganic(MapTile[][] scanMapTiles) {
//        for (int x = 0; x < scanMapTiles.length; x++) {
//            for (int y = 0; y < scanMapTiles[x].length; y++) {
//                MapTile mapTile = scanMapTiles[x][y];
//                if (mapTile.getScience() == Science.ORGANIC) {
//                    int tileX = roverLoc.xpos + (x - 3);
//                    int tileY = roverLoc.ypos + (y - 3);
//                    Coord coord = new Coord(mapTile.getTerrain(), mapTile.getScience(), tileX, tileY);
//                    science_discovered.add(coord);
//                }
//            }
//        }
//    }
	
	private void clearReadLineBuffer() throws IOException{
		while(in.ready()){
			//System.out.println("ROVER_09 clearing readLine()");
			String garbage = in.readLine();	
		}
	}
	

	// method to retrieve a list of the rover's equipment from the server
	private ArrayList<String> getEquipment() throws IOException {
		//System.out.println("ROVER_09 method getEquipment()");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		out.println("EQUIPMENT");
		
		String jsonEqListIn = in.readLine(); //grabs the string that was returned first
		if(jsonEqListIn == null){
			jsonEqListIn = "";
		}
		StringBuilder jsonEqList = new StringBuilder();
		//System.out.println("ROVER_09 incomming EQUIPMENT result - first readline: " + jsonEqListIn);
		
		if(jsonEqListIn.startsWith("EQUIPMENT")){
			while (!(jsonEqListIn = in.readLine()).equals("EQUIPMENT_END")) {
				if(jsonEqListIn == null){
					break;
				}
				//System.out.println("ROVER_09 incomming EQUIPMENT result: " + jsonEqListIn);
				jsonEqList.append(jsonEqListIn);
				jsonEqList.append("\n");
				//System.out.println("ROVER_09 doScan() bottom of while");
			}
		} else {
			// in case the server call gives unexpected results
			clearReadLineBuffer();
			return null; // server response did not start with "EQUIPMENT"
		}
		
		String jsonEqListString = jsonEqList.toString();		
		ArrayList<String> returnList;		
		returnList = gson.fromJson(jsonEqListString, new TypeToken<ArrayList<String>>(){}.getType());		
		//System.out.println("ROVER_09 returnList " + returnList);
		
		return returnList;
	}
	

	// sends a SCAN request to the server and puts the result in the scanMap array
	public void doScan() throws IOException {
		//System.out.println("ROVER_09 method doScan()");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		out.println("SCAN");

		String jsonScanMapIn = in.readLine(); //grabs the string that was returned first
		if(jsonScanMapIn == null){
			System.out.println("ROVER_09 check connection to server");
			jsonScanMapIn = "";
		}
		StringBuilder jsonScanMap = new StringBuilder();
		System.out.println("ROVER_09 incomming SCAN result - first readline: " + jsonScanMapIn);
		
		if(jsonScanMapIn.startsWith("SCAN")){	
			while (!(jsonScanMapIn = in.readLine()).equals("SCAN_END")) {
				//System.out.println("ROVER_09 incomming SCAN result: " + jsonScanMapIn);
				jsonScanMap.append(jsonScanMapIn);
				jsonScanMap.append("\n");
				//System.out.println("ROVER_09 doScan() bottom of while");
			}
		} else {
			// in case the server call gives unexpected results
			clearReadLineBuffer();
			return; // server response did not start with "SCAN"
		}
		//System.out.println("ROVER_09 finished scan while");

		String jsonScanMapString = jsonScanMap.toString();
		// debug print json object to a file
		//new MyWriter( jsonScanMapString, 0);  //gives a strange result - prints the \n instead of newline character in the file

		//System.out.println("ROVER_09 convert from json back to ScanMap class");
		// convert from the json string back to a ScanMap object
		scanMap = gson.fromJson(jsonScanMapString, ScanMap.class);		
	}
	

	// this takes the LOC response string, parses out the x and x values and
	// returns a Coord object
	public static Coord extractLOC(String sStr) {
		sStr = sStr.substring(4);
		if (sStr.lastIndexOf(" ") != -1) {
			String xStr = sStr.substring(0, sStr.lastIndexOf(" "));
			//System.out.println("extracted xStr " + xStr);

			String yStr = sStr.substring(sStr.lastIndexOf(" ") + 1);
			//System.out.println("extracted yStr " + yStr);
			return new Coord(Integer.parseInt(xStr), Integer.parseInt(yStr));
		}
		return null;
	}
	
    private void move(int direction) {
    	if(direction ==1){
          out.println("MOVE E");
    	}
    	else if(direction ==2){
            out.println("MOVE S");
      	}
    	else if(direction ==3){
            out.println("MOVE W");
      	}
    	else{
            out.println("MOVE N");
      	}
    }
    /** return a DIFFERENT direction */
    private int changeDirection(int direction) {
    	if(direction==1){
    		return 2;
    	}
    	else if(direction==2){
    		return 3;
    	}
    	else if(direction==3){
    		return 4;
    	}
    	else{
    		return 1;
    	}
    }
    /**
     * recursively call itself until it find a direction that won't lead to a
     * blocked path
     */
    private int findGoodDirection(int direction,
            MapTile[][] scanMapTiles, int centerIndex) {
        if (isNextBlock((direction+1)%4, scanMapTiles, centerIndex)) {
        	if(isNextBlock((direction+3)%4, scanMapTiles, centerIndex)){
            	if(isNextBlock((direction+2)%4, scanMapTiles, centerIndex)){
                	return (direction+4)%4;
            	}
            	else{
            		currentDirection = (direction+2)%4;
                	return (direction+2)%4;
            	}
        	}
        	else{
            	return (direction+3)%4;
        	}
        }
        else{
        	return (direction+1)%4;
        }
        
        
    }
	
    //when starts ,robot goes as this logic
    private void basicMove(int direction, MapTile[][] scanMapTiles,
            int centerIndex) {
//    	if(direction==1&&){
//    		
//    	}

        if (isNextBlock(direction, scanMapTiles, centerIndex)) {
            int goodDirection = findGoodDirection(direction, scanMapTiles,
                    centerIndex);
            if (isNextEdge(direction, scanMapTiles, centerIndex)) {
            	currentDirection = findGoodDirection(direction, scanMapTiles,
                        centerIndex);
                move(currentDirection);
            } else {
            	//currentDirection = goodDirection;
                move(goodDirection);
            }       	

        } else {
            move(direction);
        }
    }
    /** determine if the rover is on ROCK NONE OR SAND */
    private boolean isBlocked(MapTile tile) {
        List<Terrain> blockers = Arrays.asList(Terrain.NONE,
                Terrain.SAND);
        Terrain terrain = tile.getTerrain();
        return tile.getHasRover() || blockers.contains(terrain);
    }
    
    //determine if next block can block robot:
    public boolean isNextBlock(int direction, MapTile[][] scanMapTiles,int centerIndex) {
    	if(direction==1){
          return isBlocked(scanMapTiles[centerIndex + 1][centerIndex]);
    	}
    	else if(direction==2){
          return isBlocked(scanMapTiles[centerIndex][centerIndex + 1]);
    	}
    	else if(direction==3){
          return isBlocked(scanMapTiles[centerIndex - 1][centerIndex]);
    	}
    	else{
          return isBlocked(scanMapTiles[centerIndex][centerIndex - 1]);
    	}
    }
    
    private boolean isNextEdge(int direction, MapTile[][] scanMapTiles,
            int centerIndex) {
    	if(direction==1){
    		return isNone(scanMapTiles[centerIndex + 1][centerIndex]);
    	}
    	else if(direction==2){
    		return isNone(scanMapTiles[centerIndex ][centerIndex+ 1]);
    	}
    	else if(direction==3){
    		return isNone(scanMapTiles[centerIndex - 1][centerIndex]);
    	}
    	else{
    		return isNone(scanMapTiles[centerIndex ][centerIndex-1]);
    	}
    }
    
    private boolean isNone(MapTile tile) {
        return tile.getTerrain() == Terrain.NONE;
    }
    
    private void detectOrganic(MapTile[][] scanMapTiles) {
        for (int x = 0; x < scanMapTiles.length; x++) {
            for (int y = 0; y < scanMapTiles[x].length; y++) {
                MapTile mapTile = scanMapTiles[x][y];
                if (mapTile.getScience() == Science.ORGANIC) {
                    int tileX = cc.xpos + (x - 5);
                    int tileY = cc.ypos + (y - 5);
                    System.out.println("ORGANIC Location: [x:" + tileX
                            + " y: " + tileY);
                    science_collection.add(new Coord(tileX, tileY));
                }
            }
        }
    }
    private void shareScience() {
        for (Coord c : science_discovered) {
            if (!displayed_science.contains(c)) {
                for (Socket s : outputSockets)
                    try {
                        new DataOutputStream(s.getOutputStream()).writeBytes(c.toString() + "\r\n");
                    } catch (Exception e) {

                    }
                displayed_science.add(c);
            }
        }
    }
	

	/**
	 * Runs the client
	 */
	public static void main(String[] args) throws Exception {
		ROVER_09 client = new ROVER_09();
		client.run();
	}
}