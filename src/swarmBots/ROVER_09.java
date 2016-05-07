package swarmBots;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import common.Coord;
import common.MapTile;
import common.ScanMap;
import enums.Terrain;

public class ROVER_09 {
	BufferedReader in;
	PrintWriter out;
	String rovername;
	ScanMap scanMap;
	int sleepTime;
	String SERVER_ADDRESS = "localhost";
	static final int PORT_ADDRESS = 9537;
	Coord currentLoc, previousLoc, rovergroupStartPosition = null;

	public ROVER_09() {
		// constructor
		System.out.println("ROVER_09 rover object constructed");
		rovername = "ROVER_09";
		SERVER_ADDRESS = "localhost";
		// this should be a safe but slow timer value
		sleepTime = 300; // in milliseconds - smaller is faster, but the server
							// will cut connection if it is too small
	}

	/**
	 * Connects to the server then enters the processing loop.
	 */
	//customBot.Timer objectTimer = new customBot.Timer();

	private void run() throws IOException, InterruptedException {

		// Make connection and initialize streams
		// TODO - need to close this socket
		Socket socket = new Socket(SERVER_ADDRESS, PORT_ADDRESS); // set port
																	// here
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);

		// Gson gson = new GsonBuilder().setPrettyPrinting().create();

		// Process all messages from server, wait until server requests Rover ID
		// name
		//objectTimer.start();
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

		boolean stuck = false; // just means it did not change locations between
								// requests,
								// could be velocity limit or obstruction etc.
		boolean blocked = false;

		boolean[] cardinals = new boolean[4];
		cardinals[0] = false; // N
		cardinals[1] = true;// East ,going east is true by default
		cardinals[2] = false;// South
		cardinals[3] = false; // West

		boolean currentDir = cardinals[0];
		Coord currentLoc = null;
		Coord previousLoc = null;
		//long ltime = objectTimer.stop();
		//System.out.println("Stop time :    " + ltime);
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

			// after getting location set previous equal current to be able to
			// check for stuckness and blocked later
			previousLoc = currentLoc;

			// **** get equipment listing ****
			ArrayList<String> equipment = new ArrayList<String>();
			equipment = getEquipment();
			// System.out.println("ROVER_09 equipment list results drive " +
			// equipment.get(0));
			System.out.println("ROVER_09 equipment list results " + equipment + "\n");

			// ***** do a SCAN *****
			// System.out.println("ROVER_09 sending SCAN request");
			// gets the scanMap from the server based on the Rover current
			// location
			loadScanMapFromSwarmServer();
			scanMap.debugPrintMap();

			// MOVING

			// ---------------------------------Switch case for all the four
			// direction------------------

			// ***** MOVING *****
			// pull the MapTile array out of the ScanMap object
			MapTile[][] scanMapTiles = scanMap.getScanMap();

			int centerIndex = (scanMap.getEdgeSize() - 1) / 2;
			// tile S = y + 1; N = y - 1; E = x + 1; W = x - 1

			roverMoving(cardinals, scanMapTiles, centerIndex);
			setCurrentLoc(); // another call for current location

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

	private void setCurrentLoc() throws IOException {

		String line;
		// **** Request Rover Location from SwarmServer ****
		out.println("LOC");
		line = in.readLine();
		if (line == null) {
			System.out.println(rovername + " check connection to server");
			line = "";
		}
		if (line.startsWith("LOC")) {
			// loc = line.substring(4);
			currentLoc = extractLocationFromString(line);

		}
	}

	// ################ Support Methods ###########################

	private void clearReadLineBuffer() throws IOException {
		while (in.ready()) {
			// System.out.println("ROVER_09 clearing readLine()");
			String garbage = in.readLine();
		}
	}

	// method to retrieve a list of the rover's equipment from the server
	private ArrayList<String> getEquipment() throws IOException {
		// System.out.println("ROVER_09 method getEquipment()");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		out.println("EQUIPMENT");

		String jsonEqListIn = in.readLine(); // grabs the string that was
												// returned first
		if (jsonEqListIn == null) {
			jsonEqListIn = "";
		}
		StringBuilder jsonEqList = new StringBuilder();
		// System.out.println("ROVER_09 incomming EQUIPMENT result - first
		// readline: " + jsonEqListIn);

		if (jsonEqListIn.startsWith("EQUIPMENT")) {
			while (!(jsonEqListIn = in.readLine()).equals("EQUIPMENT_END")) {
				if (jsonEqListIn == null) {
					break;
				}
				// System.out.println("ROVER_09 incomming EQUIPMENT result: " +
				// jsonEqListIn);
				jsonEqList.append(jsonEqListIn);
				jsonEqList.append("\n");
				// System.out.println("ROVER_09 doScan() bottom of while");
			}
		} else {
			// in case the server call gives unexpected results
			clearReadLineBuffer();
			return null; // server response did not start with "EQUIPMENT"
		}

		String jsonEqListString = jsonEqList.toString();
		ArrayList<String> returnList;
		returnList = gson.fromJson(jsonEqListString, new TypeToken<ArrayList<String>>() {
		}.getType());
		// System.out.println("ROVER_09 returnList " + returnList);

		return returnList;
	}

	// sends a SCAN request to the server and puts the result in the scanMap
	// array
	// sends a SCAN request to the server and puts the result in the scanMap
	// array
	public void loadScanMapFromSwarmServer() throws IOException {
		// System.out.println("ROVER_09 method doScan()");
		Gson gson = new GsonBuilder().setPrettyPrinting().enableComplexMapKeySerialization().create();
		out.println("SCAN");

		String jsonScanMapIn = in.readLine(); // grabs the string that was
												// returned first
		if (jsonScanMapIn == null) {
			System.out.println("ROVER_09 check connection to server");
			jsonScanMapIn = "";
		}
		StringBuilder jsonScanMap = new StringBuilder();
		System.out.println("ROVER_09 incomming SCAN result - first readline: " + jsonScanMapIn);

		if (jsonScanMapIn.startsWith("SCAN")) {
			while (!(jsonScanMapIn = in.readLine()).equals("SCAN_END")) {
				// System.out.println("ROVER_09 incomming SCAN result: " +
				// jsonScanMapIn);
				jsonScanMap.append(jsonScanMapIn);
				jsonScanMap.append("\n");
				// System.out.println("ROVER_09 doScan() bottom of while");
			}
		} else {
			// in case the server call gives unexpected results
			clearReadLineBuffer();
			return; // server response did not start with "SCAN"
		}
		// System.out.println("ROVER_09 finished scan while");

		String jsonScanMapString = jsonScanMap.toString();
		// debug print json object to a file
		// new MyWriter( jsonScanMapString, 0); //gives a strange result -
		// prints the \n instead of newline character in the file

		// System.out.println("ROVER_09 convert from json back to ScanMap
		// class");
		// convert from the json string back to a ScanMap object
		scanMap = gson.fromJson(jsonScanMapString, ScanMap.class);
	}

	// this takes the LOC response string, parses out the x and x values and
	// returns a Coord object
	public static Coord extractLOC(String sStr) {
		sStr = sStr.substring(4);
		if (sStr.lastIndexOf(" ") != -1) {
			String xStr = sStr.substring(0, sStr.lastIndexOf(" "));
			// System.out.println("extracted xStr " + xStr);

			String yStr = sStr.substring(sStr.lastIndexOf(" ") + 1);
			// System.out.println("extracted yStr " + yStr);
			return new Coord(Integer.parseInt(xStr), Integer.parseInt(yStr));
		}
		return null;
	}

	public static Coord extractCurrLOC(String sStr) {
		sStr = sStr.substring(4);
		if (sStr.lastIndexOf(" ") != -1) {
			String xStr = sStr.substring(0, sStr.lastIndexOf(" "));
			// System.out.println("extracted xStr " + xStr);

			String yStr = sStr.substring(sStr.lastIndexOf(" ") + 1);
			// System.out.println("extracted yStr " + yStr);
			return new Coord(Integer.parseInt(xStr), Integer.parseInt(yStr));
		}
		return null;
	}

	public static Coord extractStartLOC(String sStr) {

		sStr = sStr.substring(10);

		if (sStr.lastIndexOf(" ") != -1) {
			String xStr = sStr.substring(0, sStr.lastIndexOf(" "));
			// System.out.println("extracted xStr " + xStr);

			String yStr = sStr.substring(sStr.lastIndexOf(" ") + 1);
			// System.out.println("extracted yStr " + yStr);
			return new Coord(Integer.parseInt(xStr), Integer.parseInt(yStr));
		}
		return null;
	}

	public static Coord extractTargetLOC(String sStr) {
		sStr = sStr.substring(11);
		if (sStr.lastIndexOf(" ") != -1) {
			String xStr = sStr.substring(0, sStr.lastIndexOf(" "));
			System.out.println("extracted xStr " + xStr);

			String yStr = sStr.substring(sStr.lastIndexOf(" ") + 1);
			System.out.println("extracted yStr " + yStr);
			return new Coord(Integer.parseInt(xStr), Integer.parseInt(yStr));
		}
		return null;
	}

	// this takes the server response string, parses out the x and x values and
	// returns a Coord object
	public static Coord extractLocationFromString(String sStr) {
		int indexOf;
		indexOf = sStr.indexOf(" ");
		sStr = sStr.substring(indexOf + 1);
		if (sStr.lastIndexOf(" ") != -1) {
			String xStr = sStr.substring(0, sStr.lastIndexOf(" "));
			// System.out.println("extracted xStr " + xStr);

			String yStr = sStr.substring(sStr.lastIndexOf(" ") + 1);
			// System.out.println("extracted yStr " + yStr);
			return new Coord(Integer.parseInt(xStr), Integer.parseInt(yStr));
		}
		return null;
	}

	// gather science method
	public void gatherScience(boolean[] cardinals, MapTile[][] scanMapTiles, int centerIndex) {
		System.out.println("ROVER_09: scanMapTiles[centerIndex][centerIndex].getScience().getSciString() "
				+ scanMapTiles[centerIndex][centerIndex].getScience().getSciString());
		if (!scanMapTiles[centerIndex][centerIndex].getScience().getSciString().equals("O")) {
			System.out.println("ROVER_09 request GATHER");
			out.println("GATHER");
		}
	}

	// moving method
	private void roverMoving(boolean[] cardinals, MapTile[][] scanMapTiles, int centerIndex) {

		// logic if going in east
	
		if (cardinals[1]) {
			// Checks to see if there is science on current tile, if not
			// it moves East
			System.out.println("ROVER_09: scanMapTiles[centerIndex][centerIndex].getScience().getSciString() "
					+ scanMapTiles[centerIndex][centerIndex].getScience().getSciString());
			if (scanMapTiles[centerIndex + 1][centerIndex].getScience().equals("O")) {
				// move east
				out.println("MOVE E");
				System.out.println("ROVER_09 request move E");
				cardinals[0] = false; // S
				cardinals[1] = true; // E
				cardinals[2] = false; // N
				cardinals[3] = false; // W

			} else if (scanMapTiles[centerIndex][centerIndex + 1].getScience().equals("O")) {
				// move south
				out.println("MOVE S");
				System.out.println("ROVER_09 request move S");
				cardinals[0] = true; // S
				cardinals[1] = false; // E
				cardinals[2] = false; // N
				cardinals[3] = false; // W

			} else if (scanMapTiles[centerIndex][centerIndex - 1].getScience().equals("O")) {
				// move north
				out.println("MOVE N");
				System.out.println("ROVER_09 request move N");
				cardinals[0] = false; // S
				cardinals[1] = false; // E
				cardinals[2] = true; // N
				cardinals[3] = false; // W
			} else {
				// if next move to east is an obstacle
				if (scanMapTiles[centerIndex + 1][centerIndex].getHasRover()
						|| scanMapTiles[centerIndex + 1][centerIndex].getTerrain() == Terrain.NONE
						|| scanMapTiles[centerIndex + 1][centerIndex].getTerrain() == Terrain.NONE
						|| scanMapTiles[centerIndex + 1][centerIndex].getTerrain() == Terrain.SAND) {
					// check whether south is obstacle
					if (scanMapTiles[centerIndex][centerIndex + 1].getHasRover()
							|| scanMapTiles[centerIndex][centerIndex + 1].getTerrain() == Terrain.NONE
							|| scanMapTiles[centerIndex][centerIndex + 1].getTerrain() == Terrain.SAND) {
						// check whether north is obstacle
						if (scanMapTiles[centerIndex][centerIndex - 1].getHasRover()
								|| scanMapTiles[centerIndex][centerIndex - 1].getTerrain() == Terrain.NONE
								|| scanMapTiles[centerIndex][centerIndex - 1].getTerrain() == Terrain.SAND) {
							out.println("MOVE W");
							System.out.println("ROVER_09 request move W");
							cardinals[0] = false; // S
							cardinals[1] = false; // E
							cardinals[2] = false; // N
							cardinals[3] = true; // W
						} else {
							out.println("MOVE N");
							System.out.println("ROVER_09 request move N");
							cardinals[0] = false; // S
							cardinals[1] = false; // E
							cardinals[2] = true; // N
							cardinals[3] = false; // W
						}
					} else {
						out.println("MOVE S");
						System.out.println("ROVER_09 request move S");
						cardinals[0] = true; // S
						cardinals[1] = false; // E
						cardinals[2] = false; // N
						cardinals[3] = false; // W
					}
				}
				// when no obstacle is in next move to east
				else {
					out.println("MOVE E");
					System.out.println("ROVER_09 request move E");
					cardinals[0] = false; // S
					cardinals[1] = true; // E
					cardinals[2] = false; // N
					cardinals[3] = false; // W
				}
			}
		} else if (cardinals[3]) {
			// if next move to west is an obstacle
			if (scanMapTiles[centerIndex - 1][centerIndex].getHasRover()
					|| scanMapTiles[centerIndex - 1][centerIndex].getTerrain() == Terrain.NONE
					|| scanMapTiles[centerIndex - 1][centerIndex].getTerrain() == Terrain.SAND) {
				// check whether south is obstacle
				if (scanMapTiles[centerIndex][centerIndex + 1].getHasRover()
						|| scanMapTiles[centerIndex][centerIndex + 1].getTerrain() == Terrain.NONE
						|| scanMapTiles[centerIndex][centerIndex + 1].getTerrain() == Terrain.SAND) {
					// check whether north is obstacle
					if (scanMapTiles[centerIndex][centerIndex - 1].getHasRover()
							|| scanMapTiles[centerIndex][centerIndex - 1].getTerrain() == Terrain.NONE
							|| scanMapTiles[centerIndex][centerIndex - 1].getTerrain() == Terrain.SAND) {
						out.println("E");
						System.out.println("ROVER_09 request move E");
						cardinals[0] = false; // S
						cardinals[1] = true; // E
						cardinals[2] = false; // N
						cardinals[3] = false; // W
					} else {
						out.println("MOVE N");
						System.out.println("ROVER_09 request move N");
						cardinals[0] = false; // S
						cardinals[1] = false; // E
						cardinals[2] = true; // N
						cardinals[3] = false; // W
					}
				} else {
					out.println("MOVE S");
					System.out.println("ROVER_09 request move S");
					cardinals[0] = true; // S
					cardinals[1] = false; // E
					cardinals[2] = false; // N
					cardinals[3] = false; // W
				}
			}
			// when no obstacle is in next move to west
			else {
				out.println("MOVE W");
				System.out.println("ROVER_09 request move W");
				cardinals[0] = false; // S
				cardinals[1] = false; // E
				cardinals[2] = false; // N
				cardinals[3] = true; // W
			}
		} else if (cardinals[0]) {

			// check whether south is obstacle
			if (scanMapTiles[centerIndex][centerIndex + 1].getHasRover()
					|| scanMapTiles[centerIndex][centerIndex + 1].getTerrain() == Terrain.NONE
					|| scanMapTiles[centerIndex][centerIndex + 1].getTerrain() == Terrain.SAND) {
				// if next move to west is an obstacle

				if (scanMapTiles[centerIndex - 1][centerIndex].getHasRover()
						|| scanMapTiles[centerIndex - 1][centerIndex].getTerrain() == Terrain.NONE
						|| scanMapTiles[centerIndex - 1][centerIndex].getTerrain() == Terrain.SAND) {
					// check whether east is obstacle
					if (scanMapTiles[centerIndex + 1][centerIndex].getHasRover()
							|| scanMapTiles[centerIndex + 1][centerIndex].getTerrain() == Terrain.NONE
							|| scanMapTiles[centerIndex + 1][centerIndex].getTerrain() == Terrain.SAND) {
						out.println("MOVE N");
						System.out.println("ROVER_09 request move N");
						cardinals[0] = false; // S
						cardinals[1] = false; // E
						cardinals[2] = true; // N
						cardinals[3] = false; // W
					} else {
						out.println("MOVE E");
						System.out.println("ROVER_09 request move E");
						cardinals[0] = false; // S
						cardinals[1] = true; // E
						cardinals[2] = false; // N
						cardinals[3] = false; // W
					}
				} else {
					out.println("MOVE W");
					System.out.println("ROVER_09 request move W");
					cardinals[0] = false; // S
					cardinals[1] = false; // E
					cardinals[2] = false; // N
					cardinals[3] = true; // W
				}
			}
			// when no obstacle is in next move to south
			else {
				out.println("MOVE S");
				System.out.println("ROVER_09 request move S");
				cardinals[0] = true; // S
				cardinals[1] = false; // E
				cardinals[2] = false; // N
				cardinals[3] = false; // W
			}
		} else if (cardinals[2]) {

			// check whether north is obstacle
			if (scanMapTiles[centerIndex][centerIndex - 1].getHasRover()
					|| scanMapTiles[centerIndex][centerIndex - 1].getTerrain() == Terrain.NONE
					|| scanMapTiles[centerIndex][centerIndex - 1].getTerrain() == Terrain.SAND) {
				// if next move to west is an obstacle

				if (scanMapTiles[centerIndex - 1][centerIndex].getHasRover()
						|| scanMapTiles[centerIndex - 1][centerIndex].getTerrain() == Terrain.NONE
						|| scanMapTiles[centerIndex - 1][centerIndex].getTerrain() == Terrain.SAND) {
					// check whether east is obstacle
					if (scanMapTiles[centerIndex + 1][centerIndex].getHasRover()
							|| scanMapTiles[centerIndex + 1][centerIndex].getTerrain() == Terrain.NONE
							|| scanMapTiles[centerIndex + 1][centerIndex].getTerrain() == Terrain.SAND) {
						out.println("MOVE S");
						System.out.println("ROVER_09 request move S");
						cardinals[0] = true; // S
						cardinals[1] = false; // E
						cardinals[2] = false; // N
						cardinals[3] = false; // W
					} else {
						out.println("MOVE E");
						System.out.println("ROVER_09 request move E");
						cardinals[0] = false; // S
						cardinals[1] = true; // E
						cardinals[2] = false; // N
						cardinals[3] = false; // W
					}
				} else {
					out.println("MOVE W");
					System.out.println("ROVER_09 request move W");
					cardinals[0] = false; // S
					cardinals[1] = false; // E
					cardinals[2] = false; // N
					cardinals[3] = true; // W
				}
			}
			// when no obstacle is in next move to north
			else {
				out.println("MOVE N");
				System.out.println("ROVER_09 request move N");
				cardinals[0] = false; // S
				cardinals[1] = false; // E
				cardinals[2] = true; // N
				cardinals[3] = false; // W
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
