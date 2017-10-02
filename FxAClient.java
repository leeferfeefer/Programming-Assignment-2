//Dan Fincher
//Drew Ritter

//FxAClient.java

import java.net.*;
import java.io.*;
import java.util.*;


public class FxAClient {

	//Debugging
	private static boolean isDebugMode;
	public static boolean isConnected;

	//Inputted data
	private static int client_portNumber;
	private static InetAddress emu_ipAddress;
	private static int emu_portNumber;

	private static String fileName;
	private static String sizeOfWindow;


	//For sending and receiving data
	private static RTPProtocol protocol;
	private static ReceiveThread receiveThread;
	private static SendThread sendThread;


	public static boolean isListening = true;


	private static final String errorRunMsg = "To run, type: \n"
		+ "java FxAClient X A P -d \n"
		+ "X = port number that FxAClient must bind to (must be an even number) \n"
		+ "A = ip address of NetEmu \n" 
		+ "P = port number of NetEmu \n" 
		+ "-d = debug mode (optional) \n";

	private static final String errorCommandMsg = "Valid commands are: \n"
		+ "connect \n"
		+ "get 'F' \n"
		+ "post 'F' \n"
		+ "window 'W' \n"
		+ "disconnect \n\n"
		+ "F = filename \n"
		+ "W = window size";




	public static void main(String[] args) {

		//Start Client
		startClient();

		int argsLength = args.length;
		if (argsLength >= 3 && argsLength < 5) {

			//Debug mode
			if (argsLength == 4) {
				String debugArg = args[3];
				if (debugArg.equals("-d")) {
					debugMode();
				}
			}

			//Get server/port argument
			client_portNumber = Integer.parseInt(args[0]);

			//If port is valid
			if (portTester(client_portNumber)) {

				//If client port number is even
				if (client_portNumber % 2 == 0) {

					String emu_ipAddress_string = args[1];

					try {
						//If ip address is valid
						if (ipTester(emu_ipAddress_string)) {

							emu_ipAddress = InetAddress.getByName(emu_ipAddress_string);
							emu_portNumber = Integer.parseInt(args[2]);

							//If port is valid
							if (portTester(emu_portNumber)) {

								System.out.println("Enter command:");

								Scanner scanny = new Scanner(System.in);

								//Server port number (destination port number) is always 1 more than client port number
								int server_portNumber = client_portNumber + 1;

								//For commands
								while (isListening) {
									String input = scanny.nextLine();
									String[] commands = input.split(" ");

									//For get, post, and window
									if (commands.length == 2) {
										if (commands[0].equals("get")) {
											fileName = commands[1];
											protocol.requestData(fileName);
										} else if (commands[0].equals("post")) {
											fileName = commands[1];
											sendThread = new SendThread(fileName, protocol);
											sendThread.start();
										} else if (commands[0].equals("window")) {
											sizeOfWindow = commands[1];
											protocol.changeSizeOfWindow(Integer.parseInt(sizeOfWindow));											
										} else {
											System.out.println("Command not recognized!\n");
											displayCommandError();
										}
									} else if (commands.length == 1) {
										if (commands[0].equals("connect")) {
											if (!isConnected) {
												if (isDebugMode) {
													System.out.println("/--------------------------\\");
													System.out.println("   Connecting to server...");
													System.out.println("/--------------------------\\");
												}
												protocol = new RTPProtocol(client_portNumber, server_portNumber, emu_portNumber, emu_ipAddress, isDebugMode);

												//Start receive thread which calls the listen() method for incoming packets
												receiveThread = new ReceiveThread(protocol);
												receiveThread.start();

												//Send connect packet to server - establish a connection
												protocol.startConnection();
											} else {
												System.out.println("Client is already connected to server!!!");
											}
										} else if (commands[0].equals("disconnect")) {
											protocol.endConnection();

											if (sendThread != null) {
												sendThread.stop();
											}
											receiveThread.stop();
											protocol.currentSocket.close();
											// isListening = false;
										} else {
											System.out.println("Command not recognized!\n");
											displayCommandError();
										}
									} else {
										System.out.println("Invalid number of command arguments!\n");
										displayCommandError();
									}
								}							
							} else {
								System.out.println("/--------------------------\\");
								System.out.println("INVALID PORT NUMBER!");
								System.out.println("/--------------------------\\");
								System.out.println("You must specify a port number between 1023 and 65535.");
								displayRunError();
							}
						} else {
							System.out.println("/--------------------------\\");
							System.out.println("INVALID IP ADDRESS!");
							System.out.println("/--------------------------\\");
							System.out.println("You must specify an ip address in the format of: x.x.x.x");
							System.out.println("With x being in between 0 - 255.");
							displayRunError();
						}
					} catch (Exception e) {
						System.out.println("Error");
					}
				} else {
					System.out.println("/--------------------------\\");
					System.out.println("Client port number is not an even number.");
					System.out.println("/--------------------------\\");
					displayRunError();
				}
			} else {
				System.out.println("/--------------------------\\");
				System.out.println("INVALID PORT NUMBER!");
				System.out.println("/--------------------------\\");
				System.out.println("You must specify a port number between 1023 and 65535.");
				displayRunError();
			}
		} else {
			System.out.println("/--------------------------\\");
			System.out.println("INCORRECT NUMBER OF ARGUMENTS!");
			System.out.println("/--------------------------\\");
			displayRunError();
		}
	}



	/*
		Helper Methods
	*/


	/*
	*	Displays error message for running FxAClient
	*/
	private static void displayRunError(){
		System.out.println(errorRunMsg);
	}

	/*
	*	Displays error message for FxAClient commands
	*/
	private static void displayCommandError(){
		System.out.println(errorCommandMsg);
	}

	/*
	*	Displays welcome message
	*/	
	private static void startClient(){
		System.out.println("\n");
		System.out.println("/--------------------------\\");
		System.out.println("  FxA Client");
		System.out.println("/--------------------------\\");
		System.out.println("\n\n");
	}	

	/*
	*	Enables debugging mode
	*/	
	private static void debugMode(){
		isDebugMode = true; //Debug mode initiated
		System.out.println("/~~~~~~~~~~~~~~~~~~~~~~~~~~~\\");
		System.out.println("  DEBUG MODE INITIATED");
		System.out.println("/~~~~~~~~~~~~~~~~~~~~~~~~~~~\\");
		System.out.println("\n\n");
	}



	/*
	*	Tests IP for validity
	*	@param server_ip - Ip address of server for datagramsocket conenction - ip address to be tested
	*	Return boolean stating if ip is valid
	*/ 
	private static boolean ipTester(String server_ip) {

		if (!server_ip.equals("localhost")) {
			
			//Split individual IP quad notation to test if port is valid
			String[] ip_array = server_ip.split("\\.");

			int ipLength = ip_array.length;
			//If not enough quadrants
			if (ipLength == 4) {
				for (int i = 0; i < ipLength; i++) {
					int ipQuad = Integer.parseInt(ip_array[i]);
					//If quadrant is out of ip range
					if (ipQuad > 255 || ipQuad < 0) {
						return false;
					}
				}
			} else {
				return false;
			}
		}
		return true;
	}


	/*
	*	Tests port number for validity
	*	@param port_number - port number of client to be tested 
	*	Return boolean stating if port number is valid
	*/ 
	private static boolean portTester(int port_number) {
		if (port_number > 1023 && port_number < 65535) {
			return true;
		} else {
			return false;
		}
	}



}