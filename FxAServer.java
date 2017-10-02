//Dan Fincher
//Drew Ritter

//FxAServer.java

import java.net.*;
import java.io.*;
import java.util.*;

public class FxAServer {

	//Variables
	private static boolean isDebugMode;

	//Inputted data
	private static int server_portNumber;
	private static InetAddress emu_ipAddress;
	private static int emu_portNumber;

	private static String sizeOfWindow;


	//For sending and receiving data
	private static RTPProtocol protocol;
	private static ReceiveThread receiveThread;
	private static SendThread sendThread;


	public static boolean isListening = true;


	private static final String errorRunMsg = "\nTo run, type: \n"
	+ "java FxAServer X A P -d \n"
	+ "X = port number that FxAServer must bind to (must be an odd number) \n"
	+ "A = ip address of NetEmu \n" 
	+ "P = port number of NetEmu \n" 
	+ "-d = debug mode (optional) \n";

	private static final String errorCommandMsg = "\nValid commands are: \n"
	+ "window 'W' \n"
	+ "terminate \n\n"
	+ "W = window size";

	
	public static void main(String[] args) {

		//Start Server
		startServer();

		int argsLength = args.length;
		if (argsLength >= 3 && argsLength < 5) {

			//Debug mode
			if (argsLength == 4) {
				String debugArg = args[3];
				if (debugArg.equals("-d")) {
					debugMode();
				}
			}

			server_portNumber = Integer.parseInt(args[0]);

			//If client port number is valid
			if (portTester(server_portNumber)) {

				//If server port number is odd
				if (server_portNumber % 2 == 1) {

					String emu_ipAddress_string = args[1];

					try {
						//If emu ip address is valid
						if (ipTester(emu_ipAddress_string)) {

							emu_ipAddress = InetAddress.getByName(emu_ipAddress_string);
							emu_portNumber = Integer.parseInt(args[2]);

							//If emu ip address is valid
							if (portTester(emu_portNumber)) {

								System.out.println("Enter command:");

								Scanner scanny = new Scanner(System.in);

								//Server port number (destination port number) is always 1 more than client port number
								int client_portNumber = server_portNumber - 1;

								protocol = new RTPProtocol(server_portNumber, client_portNumber, emu_portNumber, emu_ipAddress, isDebugMode);

								//Start receive thread which calls the listen() method for incoming packets
								receiveThread = new ReceiveThread(protocol);
								receiveThread.start();

								//For commands
								while (isListening) {
									String input = scanny.nextLine();
									String[] commands = input.split(" ");

									//For get, post, and window
									if (commands.length == 2) {
										if (commands[0].equals("window")) {
											sizeOfWindow = commands[1];
											protocol.changeSizeOfWindow(Integer.parseInt(sizeOfWindow));											
										} else {
											System.out.println("Command not recognized!\n");
											displayCommandError();
										}
									} else if (commands.length == 1) {
										if (commands[0].equals("terminate")) {

											//For posting
											//Let threads finish
											for (SendThread thread : protocol.threadArrayList) {
												if (thread.getState() != Thread.State.NEW) {	
													if (thread.getState() != Thread.State.TERMINATED) {
														System.out.println("/--------------------------\\");
														System.out.println("  Finishing file transfer...");
														System.out.println("/--------------------------\\");										
													}												
													while (thread.getState() != Thread.State.TERMINATED) {}
													thread.stop();
												System.out.println("/--------------------------\\");
												System.out.println("  File transfer complete...");
												System.out.println("/--------------------------\\");
												} 
											}

											// if (receiveThread.getState != ) {
												
											// }
											receiveThread.stop();

												
											protocol.endConnection();
											protocol.currentSocket.close();
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
					System.out.println("Server port number is not an odd number.");
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
	*	Displays error message
	*/
	private static void displayRunError(){
		System.out.println(errorRunMsg);
	}

	/*
	*	Displays error message for FxAServer commands
	*/
	private static void displayCommandError(){
		System.out.println(errorCommandMsg);
	}

	/*
	*	Displays welcome message
	*/	
	private static void startServer(){
		System.out.println("\n/--------------------------\\");
		System.out.println("   FxA Server");
		System.out.println("/--------------------------\\\n");
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