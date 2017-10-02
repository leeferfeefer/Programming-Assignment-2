//Dan Fincher
//Drew Ritter

//RTPProtocol.java

import java.net.*;
import java.util.*;
import java.io.*;


//Checksum
import java.util.zip.*;


public class RTPProtocol {

	private static FxAClient client;


	//Debugging
	private static boolean isDebugMode;


	//Variables
	private int srcPort;
	private int destPort;
	private int emu_portNumber;
	private InetAddress emu_ipAddress;


	//Socket
	public DatagramSocket currentSocket;

	//Packets
	private DatagramPacket sndPacket;
	private DatagramPacket rcvPacket;
	private static final int MAXBUFFERSIZE = 255;
	private static final boolean corrupted = true;
	private static final boolean notCorrupted = false;


	//Header state info (to keep track of what was sent to compare it to packet that was received)
	private RTPPacketHeader sentHeader;


	/*
		States
	*/

	//Connection states
	private enum ConnectionState {
		Listening, SynAck, Connected, Closing, ConnectionExit
	};
	private ConnectionState conState;

	//Post states
	private enum PostState {
		PostSYN, PostACK, PostExit
	};
	private PostState postState;

	//Get states
	private enum GetState {
		GetSYN, GetACK, GetExit
	};
	private GetState getState;




	//Essential variables
	private Timer timer;
	private Window window;
	private int receiveDataIndex;

	//Keep track of send threads
	public ArrayList<SendThread> threadArrayList;

	//Keep track of packets (in correct order)
	private PacketQueue<byte[]> packetQueue;



	/*
		File IO
	*/
	private FileOutputStream receivingStream;
	private BufferedOutputStream receivedBuffer;




	/*
		In order packet array list
	*/
	public ArrayList<byte[]> packetArrayList;

	private boolean listen;


	public RTPProtocol(int srcPort, int destPort, int emu_portNumber, InetAddress emu_ipAddress, boolean isDebugMode) {
		try {
			this.srcPort = srcPort;
			this.destPort = destPort;
			this.emu_portNumber = emu_portNumber;
			this.emu_ipAddress = emu_ipAddress;


			//Bind socket to source's port
			currentSocket = new DatagramSocket(srcPort);

			sentHeader = new RTPPacketHeader(0, 0, (short)srcPort, (short)destPort);

			//Reset all variables
			reset();

			//Debug mode
			this.isDebugMode = isDebugMode;

		} catch (Exception e) {
			System.out.println("Error binding socket...");
			e.printStackTrace();
		}
	}


	//Reset variables and objects (after disconnection and after failed attempts to connect)
	public void reset() {
		timer = new Timer();
		window = new Window();
		receiveDataIndex = 0;

		//States
		conState = ConnectionState.Listening;
		getState = GetState.GetSYN;
		postState = PostState.PostSYN;

		packetQueue = new PacketQueue<byte[]>();
		threadArrayList = new ArrayList<SendThread>();
		packetArrayList = new ArrayList<byte[]>();

		//Create send and receive packets
		sndPacket = new DatagramPacket(new byte[MAXBUFFERSIZE], MAXBUFFERSIZE);
		rcvPacket = new DatagramPacket(new byte[MAXBUFFERSIZE], MAXBUFFERSIZE);

		client.isConnected = false;

		listen = true;
	}




	/*
		Window Methods
	*/

	public void changeSizeOfWindow(int sizeOfWindow) {
		if (conState != ConnectionState.Connected) {
			System.out.println("No connection with server established");
		} else {
			window.sizeOfWindow = sizeOfWindow;
			if (isDebugMode) {
				System.out.println("/--------------------------\\");
				System.out.println("    Window size is now" + sizeOfWindow);
				System.out.println("/--------------------------\\");
			}	
		}
	}




	/*
		Client Methods
	*/

	public void startConnection() {

		sentHeader.seqNum = 0;

		sentHeader.syn = true;
		sentHeader.con = true;
		sendPacket(null);

		//Start timer
		timer.start();

		//While we are waiting to be connected with server
		//If timedout, resend connection SYN packet
		while (conState == ConnectionState.Listening) {
			if (isDebugMode) {
				System.out.println("/--------------------------\\");
				System.out.println("Connection state is listening");
				System.out.println("/--------------------------\\");
			}
			if (timer.isTimeout()) {
				if (isDebugMode) {
					System.out.println("  /--------------------------\\");
					System.out.println("Timeout waiting for connection SynAck");
					System.out.println("  /--------------------------\\");
				}

				if (timer.isMoreTries()) {
					//Resend connect packet
					sentHeader.seqNum = 0;

					sentHeader.syn = true;
					sendPacket(null);

					//Restart timer
					timer.start();
				} else {
					System.out.println("Timed out. Could not establish a connection with the server. Try again");
					conState = ConnectionState.ConnectionExit;
					break;
				}
			}
		}
		timer.resetTries();


		if (conState != ConnectionState.ConnectionExit) {
			while (conState == ConnectionState.SynAck) {
				if (isDebugMode) {
					System.out.println("/--------------------------\\");
					System.out.println("Connection state is SynAck");
					System.out.println("/--------------------------\\");
				}
				if (timer.isTimeout()) {
					if (isDebugMode) {
						System.out.println("  /--------------------------\\");
						System.out.println("Timeout waiting for connection ACK");
						System.out.println("  /--------------------------\\");
					}

					if (timer.isMoreTries()) {
						//Resend acknowledgement of SYN/ACK
						sentHeader.seqNum = 1;

						sentHeader.syn = false;
						sendPacket(null);

						//Restart timer
						timer.start();
					} else {
						System.out.println("Timed out. Could not establish a connection with the server. Try again");
						conState = ConnectionState.ConnectionExit;	
						break;
					}
				} 
			}
		}
		timer.resetTries();
		sentHeader.con = false;

		if (conState == ConnectionState.ConnectionExit) {
			reset();
		}
	}


	public void endConnection() {

		sentHeader.seqNum = 0;

		sentHeader.con = true;
		sentHeader.fin = true;
		sendPacket(null);
		// sentHeader.con = false;
		// sentHeader.fin = false;

		//Start timer
		timer.start();

		while (conState == ConnectionState.Connected) {
			if (isDebugMode) {
				System.out.println("/--------------------------\\");
				System.out.println("Connection state is connected");
				System.out.println("/--------------------------\\");
			}
			if (timer.isTimeout()) {
				if (isDebugMode) {
					System.out.println("   /--------------------------\\");
					System.out.println("Timeout waiting for connection close SynAck");
					System.out.println("   /--------------------------\\");
				}

				if (timer.isMoreTries()) {
					//resend close connection packet
					sentHeader.seqNum = 0;

					// sentHeader.con = true;
					sentHeader.fin = true;
					sendPacket(null);
					// sentHeader.con = false;
					sentHeader.fin = false;

					//Restart timer
					timer.start();
				} else {
					System.out.println("Timed out. Could not close the connection with the server. Try again");
					conState = ConnectionState.ConnectionExit;
					break;
				}
			}
		}
		timer.resetTries();


		if (conState != ConnectionState.ConnectionExit) {
			while (conState == ConnectionState.Closing) {
				if (isDebugMode) {
					System.out.println("/--------------------------\\");
					System.out.println("Connection state is close wait");
					System.out.println("/--------------------------\\");
				}
				if (timer.isTimeout()) {
					if (isDebugMode) {
						System.out.println("    /--------------------------\\");
						System.out.println("Timeout waiting for connection close ACK");
						System.out.println("    /--------------------------\\");
					}

					if (timer.isMoreTries()) {
						//Resent close aonncetion acknowledgement packet
						sentHeader.seqNum = 1;

						sentHeader.fin = false;
						sendPacket(null);

						//Restart timer
						timer.start();
					} else {
						System.out.println("Timed out. Could not close the connection with the server. Try again");
						conState = ConnectionState.Connected;
					}
				}
			}
		}
		timer.resetTries();
		sentHeader.con = false;
		reset();
	}

	













	/*
		Client & Server Methods
	*/

	public void listenForPackets() {
		while (true) {
			try {
				currentSocket.receive(rcvPacket);
				byte[] packetData = new byte[rcvPacket.getLength()];
				System.arraycopy(rcvPacket.getData(), 0, packetData, 0, rcvPacket.getLength());

				if (checkPacketChecksum(packetData) == corrupted) {
					if (isDebugMode) {
						System.out.println("/--------------------------\\");
						System.out.println("  Received corrupted packet");
						System.out.println("/--------------------------\\");
					}
				} else {
					if (this.isDebugMode) {
						System.out.println("/--------------------------\\");
						System.out.println("     Received packet");
						System.out.println("/--------------------------\\");
					}


					RTPPacketHeader receivedHeader = new RTPPacketHeader();
					receivedHeader.makeHeaderFromPacketData(packetData);
					// receivedHeader = receivedHeader.createHeader();

					//If received packet is a connection packet
					if (receivedHeader.con)  {
						if (isDebugMode) {
							System.out.println("/--------------------------\\");
							System.out.println("Packet is a connection packet");
							System.out.println("/--------------------------\\");
						}
						receivedConnectionHeader(receivedHeader);

					//If received packet is a data packet (packet that contains file data)
					} else if (receivedHeader.dat) {
						if (isDebugMode) {
							System.out.println("/--------------------------\\");
							System.out.println("  Packet is a data packet");
							System.out.println("/--------------------------\\");
						}
						receivedDataHeader(receivedHeader, packetData);

					//If received packet is a get file packet
					} else if (receivedHeader.get) {
						if (isDebugMode) {
							System.out.println("/--------------------------\\");
							System.out.println("Packet is a get file packet");
							System.out.println("/--------------------------\\");
						}
						receivedGetHeader(receivedHeader, packetData);

					//If received packet is a post file packet
					} else if (receivedHeader.pos) {
						if (isDebugMode) {
							System.out.println("/--------------------------\\");
							System.out.println("Packet is a post file packet");
							System.out.println("/--------------------------\\");
						}	
						receivedPostHeader(receivedHeader, packetData);		
					// } else if () {


					} else {
						if (isDebugMode) {
							System.out.println("/--------------------------\\");
							System.out.println(" RECEIVED UNKNOWN PACKET");
							System.out.println("/--------------------------\\");
						}	
					}
				}
			} catch (Exception e) {
				System.out.println("Error occurred: could not get data from received packet in listenForPackets()");
				e.printStackTrace();
			}
		}
	}




	//Server and client method
	public void receivedConnectionHeader(RTPPacketHeader receivedHeader) {
		//Sent packet's acknowledgment number is now the received packet's sequence number
		sentHeader.ackNum = receivedHeader.seqNum;

		if (conState == ConnectionState.Listening) {

			//Server side
			//Received SYN from client, now sending SYN-ACK
			if (receivedHeader.syn) {
				sentHeader.con = true;
				sendAckPacket();
				conState = ConnectionState.SynAck;

			//Client side
			//Received SYN-ACK from server, now sending just connection packet
			} else if (sentHeader.syn && receivedHeader.ack) {
				sentHeader.seqNum = 1;
				sentHeader.syn = false;
				sendPacket(null);
				conState = ConnectionState.SynAck;

			//TIMEOUT
			//RESEND ACK
			} else if (!receivedHeader.fin && !receivedHeader.ack) {
				sentHeader.con = true;
				sendAckPacket();
				sentHeader.con = false;
			}


		} else if (conState == ConnectionState.SynAck) {

			//Server side
			//Received connection packet from client (NON ACK/SYN) to denote that it is connected
			//Send ACK to client to tell it that it is connected
			if (!receivedHeader.syn && !receivedHeader.ack) {
				conState = ConnectionState.Connected;
				sendAckPacket();
				sentHeader.con = false;

				System.out.println("/--------------------------\\");
				System.out.println("   Connection Established");
				System.out.println("/--------------------------\\");
				
			

			//Client side
			//Received ACK from server. Change state to connected
			} else if (receivedHeader.ack){
				conState = ConnectionState.Connected;

				System.out.println("/--------------------------\\");
				System.out.println("   Connection Established");
				System.out.println("/--------------------------\\");
				
				client.isConnected = true;
			

			//TIMEOUT
			//RESEND ACK
			} else if (receivedHeader.syn && receivedHeader.seqNum == 0) {
				sentHeader.con = true;
				sendAckPacket();
				sentHeader.con = false;
			}

		} else if (conState == ConnectionState.Connected) {

			//Server side/Client side
			//Server receives fin packet from client. Send acknowledgement to client
			//Connection state changes to close wait until the client acknowledges the server's acknowledgement
			if (receivedHeader.fin) {
				sentHeader.con = true;
				// sentHeader.fin = true;
				sendAckPacket();
				conState = ConnectionState.Closing;


			//Client side
			//Client receives acknowledgement from server that states that the server acknowledged the client's request to close the connection
			//Connection state changes to close wait
			//Send empty packet to server (NON ACK/SYN)
			} else if (sentHeader.fin && receivedHeader.ack) {
				sentHeader.seqNum = 1;
				sentHeader.fin = false;
				sendPacket(null);
				conState = ConnectionState.Closing;


			//TIMEOUT
			//RESEND ACK
			} else if (!receivedHeader.syn && !receivedHeader.ack) {
				sentHeader.con = true;
				sendAckPacket();
				sentHeader.con = false;
			}

		} else if (conState == ConnectionState.Closing) {

			//Server side/client side
			//Received empty packet (NON ACK/SYN) from client
			//Send acknowledgement to close
			//Connection is closed on server side and is now listening for new clients
			if (!receivedHeader.fin && !receivedHeader.ack) {
				conState = ConnectionState.Listening;
				sendAckPacket();
				sentHeader.con = false;
				reset();

				System.out.println("/--------------------------\\");
				System.out.println("Connection is closed");
				System.out.println("/--------------------------\\");
				


			//Client side/server side
			//Received acknowledgement from server to close
			//Client can now close
			} else if (receivedHeader.ack) {
				conState = ConnectionState.Listening;

				System.out.println("/--------------------------\\");
				System.out.println("Connection is closed");
				System.out.println("/--------------------------\\");
				
				// client.isListening = false;
				// server.isListening = false;


			//TIMEOUT
			//RESEND ACK
			} else if (receivedHeader.fin && receivedHeader.seqNum == 0) {
				sentHeader.con = true;
				sendAckPacket();
				sentHeader.con = false;
			}
		}

		//Flush packet here - Remove data that was used for packet
	 	if (isDebugMode) {
			System.out.println("/--------------------------\\");
			System.out.println("     Flushing Packet...");
			System.out.println("/--------------------------\\");
		}
		rcvPacket = new DatagramPacket(new byte[MAXBUFFERSIZE], MAXBUFFERSIZE);
	}


	//Server and client method
	synchronized public void receivedDataHeader(RTPPacketHeader receivedHeader, byte[] packetData) {

		//Server side/Client (both can send/receive data)
		if (!receivedHeader.ack) {
			if (receivedBuffer != null) {
				try {
					if (receivedHeader.seqNum == receiveDataIndex) {

						// packetArrayList.add(packetData);
						receiveDataIndex++;

						receivedBuffer.write(unPackData(packetData), 0, unPackData(packetData).length);
						receivedBuffer.flush();


						if (receivedHeader.eof) {
							// Sort packet array list
							// sortPackets(packetArrayList);
							// //Create file with the receiving stream - In order
							// for (byte[] packet : packetArrayList) {
							// 	//Place received data packets into the received buffer to be writter to file
							// 	receivedBuffer.write(unPackData(packet), 0, unPackData(packet).length);
							// }	

							//Write contents of buffer to the file
							// receivedBuffer.flush();
							receivingStream.close();

							System.out.println("/--------------------------\\");
							System.out.println("   File received...");
							System.out.println("/--------------------------\\");
						}
					}
				} catch (Exception e) {
					System.out.println("The file could not be written");
					e.printStackTrace();

				}
				
				// (in order)
				//Change ack nums depending on sequence number or index of data in window
				if (receiveDataIndex > receivedHeader.seqNum) {
					//Sent packet's acknowledgment number is now the received packet's sequence number
					sentHeader.ackNum = receivedHeader.seqNum;
				} else if (receiveDataIndex < receivedHeader.seqNum) {
					//Sent packet's acknowledgment number is now the index - 1
					sentHeader.ackNum = receiveDataIndex - 1;
				}

				// Send acknowdlegement that we received the data
				sentHeader.dat = true;
				sendAckPacket();
				sentHeader.dat = false;
			}
		//Client side/Server (both can send/receive data)
		} else {
			if (receivedHeader.ackNum == window.startOfWindow) {

				packetQueue.dequeue();

				//Change window parameters - set up for next packet
				window.startOfWindow++;
				window.endOfWindow++;

				//Start timer
				timer.start();
			}
		}

		//Flush packet here - Remove data that was used for packet
	 	if (isDebugMode) {
			System.out.println("/--------------------------\\");
			System.out.println("     Flushing Packet...");
			System.out.println("/--------------------------\\");
		}
		rcvPacket = new DatagramPacket(new byte[MAXBUFFERSIZE], MAXBUFFERSIZE);
	}



	/*
		Server Methods
	*/

	//Server method
	synchronized public void receivedGetHeader(RTPPacketHeader receivedHeader, byte[] packetData) {
		//Sent packet's acknowledgment number is now the received packet's sequence number
		sentHeader.ackNum = receivedHeader.seqNum;

		//Server side
		if (!receivedHeader.ack) {

			if (getState == GetState.GetSYN) {
				getState = GetState.GetACK;

				SendThread sendThread = new SendThread(new String(unPackData(packetData)), this);
				//Add to thread list (for keeping track of send threads)
				threadArrayList.add(sendThread);
				sendThread.start();
			}

			sentHeader.get = true;
			sendAckPacket();
			sentHeader.get = false;

		//Client side - Server sends ackwnoledgement to client letting it know that it receveived its request to get a file
		} else {
			getState = GetState.GetACK;
		}

		//Flush packet here - Remove data that was used for packet
	 	if (isDebugMode) {
			System.out.println("/--------------------------\\");
			System.out.println("     Flushing Packet...");
			System.out.println("/--------------------------\\");
		}
		rcvPacket = new DatagramPacket(new byte[MAXBUFFERSIZE], MAXBUFFERSIZE);
	}



	//Server method
	public void receivedPostHeader(RTPPacketHeader receivedHeader, byte[] packetData) {
		try {
			//Sent packet's acknowledgment number is now the received packet's sequence number
			sentHeader.ackNum = receivedHeader.seqNum;

			if (postState == PostState.PostSYN) {
				//If we sent an acknowledgement to client
				if (receivedHeader.ack) {
					postState = PostState.PostACK;
				} else {
					sentHeader.pos = true;

					//Receiving file stream - write to file - unpackdata -> gets file name
					receivingStream = new FileOutputStream(System.getProperty("user.dir") + "/" + new String(unPackData(packetData)).trim(), true);
					receivedBuffer = new BufferedOutputStream(receivingStream);
					sendAckPacket();
					sentHeader.pos = false;
				}
			}

		//Flush packet here - Remove data that was used for packet
	 	if (isDebugMode) {
			System.out.println("/--------------------------\\");
			System.out.println("     Flushing Packet...");
			System.out.println("/--------------------------\\");
		}
		rcvPacket = new DatagramPacket(new byte[MAXBUFFERSIZE], MAXBUFFERSIZE);
		} catch (Exception e) {
			System.out.println("file not found... in receuved post header methdd");
			e.printStackTrace();
		}
	}
















	/*
		Packet Methods
	*/


	public void sendPacket(byte[] packetData) {
		try {
			//Dataless packet
			if (packetData == null) {
				if (sentHeader.syn) {
					if (isDebugMode) {
						System.out.println("/--------------------------\\");
						System.out.println("    Sending SYN packet...");
						System.out.println("/--------------------------\\");
					}
				}
				if (sentHeader.con) {
					if (isDebugMode) {
						System.out.println("/--------------------------\\");
						System.out.println("    Sending con packet...");
						System.out.println("/--------------------------\\");
					}
				}
				if (sentHeader.eof) {
					if (isDebugMode) {
						System.out.println("/--------------------------\\");
						System.out.println("    Sending eof packet...");
						System.out.println("/--------------------------\\");
					}
				} 
				if (sentHeader.fin) {
					if (isDebugMode) {
						System.out.println("/--------------------------\\");
						System.out.println("    Sending FIN packet...");
						System.out.println("/--------------------------\\");
					}
				} 
			} else {
				//Dataful packet
				if (sentHeader.pos) {
					if (isDebugMode) {
						System.out.println("/--------------------------\\");
						System.out.println("   Sending post packet...");
						System.out.println("/--------------------------\\");
					}
				}
				if (sentHeader.get) {
					if (isDebugMode) {
						System.out.println("/--------------------------\\");
						System.out.println("   Sending get packet...");
						System.out.println("/--------------------------\\");
					}
				} 
				if (sentHeader.dat) {
					if (isDebugMode) {
						System.out.println("/--------------------------\\");
						System.out.println("   Sending data packet...");
						System.out.println("/--------------------------\\");
					}
				}
			}

			sentHeader.ack = false;
			byte[] packet = packData(sentHeader.createHeader(), packetData);
			packet = addChecksumToPacketData(packet);
			sndPacket = new DatagramPacket(packet, packet.length, emu_ipAddress, emu_portNumber);


			if (isDebugMode) {
				System.out.println("\n\n/--------------------------\\");
				System.out.println("The sent packet has flags:");
				if (sentHeader.get) {
								System.out.println("Get Flag");

				}
				if (sentHeader.pos) {
								System.out.println("post flag");

				}
				if (sentHeader.dat) {
								System.out.println("data flag");

				}
				if (sentHeader.con) {
								System.out.println("connection flag");

				}
				if (sentHeader.syn) {
								System.out.println("syn flag");

				}
				if (sentHeader.ack) {
								System.out.println("ack flag");

				}
				if (sentHeader.fin) {
								System.out.println("fin flag");

				}
				if (sentHeader.eof) {
								System.out.println("fin flag");

				}
				System.out.println("/--------------------------\\\n\n");
			}
			currentSocket.send(sndPacket);

		} catch (Exception e) {
			System.out.println("error sending packet in send() method");
			e.printStackTrace();
		}
	}
	


	public void sendAckPacket() {	
		try {
		 	if (isDebugMode) {
				System.out.println("/--------------------------\\");
				System.out.println("      Sending ACK...");
				System.out.println("/--------------------------\\");
			}
			sentHeader.ack = true;
			byte[] packetData = sentHeader.createHeader();
			addChecksumToPacketData(sentHeader.createHeader());
			sndPacket = new DatagramPacket(packetData, RTPPacketHeader.header_length, emu_ipAddress, emu_portNumber);


			if (isDebugMode) {
				System.out.println("\n\n/--------------------------\\");
				System.out.println("The sent packet has flags:");
				if (sentHeader.get) {
								System.out.println("Get Flag");

				}
				if (sentHeader.pos) {
								System.out.println("post flag");

				}
				if (sentHeader.dat) {
								System.out.println("data flag");

				}
				if (sentHeader.con) {
								System.out.println("connection flag");

				}
				if (sentHeader.syn) {
								System.out.println("syn flag");

				}
				if (sentHeader.ack) {
								System.out.println("ack flag");

				}
				if (sentHeader.fin) {
								System.out.println("fin flag");

				}
				if (sentHeader.eof) {
								System.out.println("fin flag");

				}
				System.out.println("/--------------------------\\\n\n");
			}
			currentSocket.send(sndPacket);

		} catch (Exception e) {
			System.out.println("error sending packet in sendAck() method");
			e.printStackTrace();
		}	
	}
	 

	public static byte[] packData(byte[] packetHeader, byte[] packetData) {
		int headerLength = 18;

		//Just an SYN/CON/ACK packet with no data
		if (packetData == null) {
			return packetHeader;
		} else {

			byte[] sendingPacket = new byte[packetData.length + headerLength];
 
 			//Copy over header
			for (int i = 0; i < headerLength; i++){
				sendingPacket[i] = packetHeader[i];
			}
			
			//Copy over packet data (payload)
			for (int j = 0; j < packetData.length; j++) {
				sendingPacket[j + headerLength] = packetData[j];
			}
			return sendingPacket;
		}
	}

	public static byte[] unPackData(byte[] packetData) {
		int headerLength = 18;
		// byte[] payload = new byte[packetData.length - headerLength];

		// for (int i = headerLength; i < (packetData.length - headerLength); i++) {
		// 	payload[i - headerLength] = packetData[i];
		// }

		// return payload;

		byte[] payload = new byte[packetData.length - headerLength];
		System.arraycopy(packetData, headerLength, payload, 0, packetData.length - headerLength);
		return payload;
	}












	/*
		Get and Post Methods
	*/

	//GET
	public void requestData(String fileName) {
		try {
			//Make sure client and server are connected
			if (conState == ConnectionState.Connected) {
				sentHeader.seqNum = 0;

				sentHeader.get = true;
				//Send packet containing byte array from file
				sendPacket(fileName.getBytes());
				sentHeader.get = false;

				//Start timer
				timer.start();

				//File request not acknowledged yet
				while (getState == GetState.GetSYN) {

					if (timer.isTimeout()) {
						if (isDebugMode) {
							System.out.println("   /--------------------------\\");
							System.out.println("Timeout waiting for request data ACK");
							System.out.println("   /--------------------------\\");
						}
						if (timer.isMoreTries()) {
							//Resend packet with file data (filename) (resent rewuest to request data)
							sentHeader.seqNum = 0;
							sentHeader.get = true;

							//Send packet containing byte array from file
							sendPacket(fileName.getBytes());

							sentHeader.get = false;

							//Reset timer
							timer.start();
						} else {
							System.out.println("Timed out. Could not request file from server. Try again");
							getState = GetState.GetExit;
							break;
						}	
					}
				}
				timer.resetTries();

				if (getState == GetState.GetExit) {
					getState = GetState.GetSYN;
				} else {
				 	if (isDebugMode) {
						System.out.println("/--------------------------\\");
						System.out.println("     Receiving file...");
						System.out.println("/--------------------------\\");
					}
				}
			} else {
				System.out.println("No connection with server established");
			}
		} catch (Exception e) {
			System.out.println("Error requesting file in requestFile() method");
			e.printStackTrace();
		}
	}




	//POST
	public void sendData(String fileName) {
		try {
			//Make sure client and server are connected
			if (conState == ConnectionState.Connected) {

				//Get file stream from file to be sent to server
				FileInputStream sendingStream = new FileInputStream(System.getProperty("user.dir") + "/" + fileName.trim());

				sentHeader.seqNum = 0;

				//Send filename
				sentHeader.pos = true;
				sendPacket(fileName.getBytes());
				sentHeader.pos = false;

				//Start timer
				timer.start();

				//Request to send file not acknowledged yet
				while (postState == PostState.PostSYN) {

					if (timer.isTimeout()) {
						if (isDebugMode) {
							System.out.println("/--------------------------\\");
							System.out.println("	Timeout sending data");
							System.out.println("/--------------------------\\");
						}

						if (timer.isMoreTries()) {
							//Resend filename
							sentHeader.seqNum = 0;

							sentHeader.pos = true;
							sendPacket(fileName.getBytes());
							sentHeader.pos = false;

							//Reset timer
							timer.start();
						} else {
							System.out.println("Timed out. Could not send data to host. Try again");
							postState = PostState.PostExit;
							break;
						}
					}
				}
				timer.resetTries();

				if (postState != PostState.PostExit) {
					byte[] fileData = new byte[MAXBUFFERSIZE - 18];
					// return the total number of bytes read into the buffer, 
					//or -1 if there is no more data because the end of the file has been reached.
					int packetDataLength = sendingStream.read(fileData);
					//Data from the packet is being read into the fileData

					byte[] packetData = null;

					//Start timer
					timer.start();

					//When payload length = -1, it is EOF
					//loop until no more items in buffer or there is no more data read into the fileData
					while (!packetQueue.isEmpty() || packetDataLength != -1) {
						if (timer.isTimeout()) {

							if (isDebugMode) {
								System.out.println("/--------------------------\\");
								System.out.println("		Timeout ");
								System.out.println("/--------------------------\\");
							}

							if (timer.isMoreTries()) {

								//Reset timer
								timer.start();

								window.next = window.startOfWindow;

								//Ensures correct packet ordering
								ArrayList<byte[]> packetList = packetQueue.returnArrayList();
								for (int i = 0; i < packetList.size(); i++) {
									if (packetDataLength == -1) {
										if (i == (packetList.size() - 1)) {
											sentHeader.eof = true;
										}
									}
									sentHeader.seqNum = window.next;
									sentHeader.dat = true;
									sendPacket(packetList.get(i));
									sentHeader.dat = false;
									sentHeader.eof = false;

									window.next++;
								}
							} else {
								System.out.println("Timed out. Could not send data to the server. Try again");
								break;
							}
						}

						if (packetDataLength != -1 && window.next <= window.endOfWindow) {		

							//Create packet with data to be sent from file			
							packetData = new byte[packetDataLength];

							//Copy contents of file data into PacketData to be sent
							System.arraycopy(fileData, 0, packetData, 0, packetDataLength);

							packetDataLength = sendingStream.read(fileData);

							//If end of packet data
							if (packetDataLength == -1) {
								if (isDebugMode) {
									System.out.println("/--------------------------\\");
									System.out.println("	Sending EOF packet... ");
									System.out.println("/--------------------------\\");
								}
								sentHeader.eof = true;
							}

							sentHeader.seqNum = window.next;

							sentHeader.dat = true;
							sendPacket(packetData);
							sentHeader.dat = false;
							sentHeader.eof = false;

							window.next++;

							packetQueue.enqueue(packetData);
						}
					}
					timer.resetTries();

					//Close input stream
					sendingStream.close();
					sentHeader.eof = false;

					//Reset file send/get states
					postState = PostState.PostSYN;
					getState = GetState.GetSYN;
				} else {
					postState = PostState.PostSYN;
				}
			} else {
				System.out.println("No connection with server established");
			}
		} catch (Exception e) {
			System.out.println("Error with input stream in sendData() method");
			e.printStackTrace();
		}
	}








	/*
		Checksum Methods
	*/

	//For sent packet
	public byte[] addChecksumToPacketData(byte[] packetData) {

		//Reset checksum in packet data
		packetData[12] = 0x00;
		packetData[13] = 0x00;
		packetData[14] = 0x00;
		packetData[15] = 0x00;

		Checksum checksum = new CRC32();
		checksum.update(packetData, 0, packetData.length);
		int checksumValue = (int)checksum.getValue();

		packetData[12] = (byte)(checksumValue >> 24);
		packetData[13] = (byte)(checksumValue >> 16);
		packetData[14] = (byte)(checksumValue >> 8);
		packetData[15] = (byte)(checksumValue);

		return packetData;
	}

	//For received packet
	public boolean checkPacketChecksum(byte[] packetData) {

		int checksumFromPacket = (int)(packetData[12] << 24 | packetData[13] << 16 | packetData[14] << 8 | packetData[15]);

		packetData[12] = 0x00;
		packetData[13] = 0x00;
		packetData[14] = 0x00;
		packetData[15] = 0x00;

		Checksum checksum = new CRC32();
		checksum.update(packetData, 0, packetData.length);
		int checksumValue = (int)checksum.getValue();

		byte[] checksumValueByte = new byte[4];
		checksumValueByte[0] = (byte)(checksumValue >> 24);
		checksumValueByte[1] = (byte)(checksumValue >> 16);
		checksumValueByte[2] = (byte)(checksumValue >> 8);
		checksumValueByte[3] = (byte)(checksumValue);

		int checksumFromValueByte = (int)(checksumValueByte[0] << 24 | checksumValueByte[1] << 16 | checksumValueByte[2] << 8 | checksumValueByte[3]);


		if (checksumFromValueByte == checksumFromPacket) {
			return notCorrupted;
		} else {
			return corrupted;
		}
	}



	/*
			Bubble sort for in order packets
	*/

	public void sortPackets(ArrayList<byte[]> arrayList) {
		boolean swapped = true;
	    int j = 0;
	    while (swapped) {
	        swapped = false;
	        j++;
	        for (int i = 0; i < arrayList.size() - j; i++) {  
	        	byte[] packetBefore = arrayList.get(i);
	        	byte[] packetAfter = arrayList.get(i + 1);

    			//Combine next 4 bytes of packet data to get sequence number
				int seqNumBefore = (int)(packetBefore[4] << 24 | packetBefore[5] << 16 | packetBefore[6] << 8 | packetBefore[7]);	
				int seqNumAfter = (int)(packetAfter[4] << 24 | packetAfter[5] << 16 | packetAfter[6] << 8 | packetAfter[7]);

	            if (seqNumBefore > seqNumAfter) {       
	            	if (isDebugMode) {
						System.out.println("/--------------------------\\");
						System.out.println("	A packet was out of order");
						System.out.println("/--------------------------\\"); 	
            	    }    
                    arrayList.set(i, packetAfter);
                    arrayList.set(i + 1, packetBefore);
                    swapped = true;
                }
            }                
        }
	}

	

	
	



}