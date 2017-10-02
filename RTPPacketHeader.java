//Dan Fincher
//Drew Ritter

//RTPPacketHeader.java


/*
	Notes:
	| = setter
	& = getter
*/	


public class RTPPacketHeader {

	public byte[] packetHeader;
	final static int header_length = 18;	
	public short srcPort;		
	public short destPort;		
	public int seqNum;			
	public int ackNum;		
	public int checksum;


	/*
		Packet Flags
	*/
	public boolean syn;		
	public boolean ack;		
	public boolean fin;		
	public boolean eof;		
	public boolean con;		
	public boolean dat;		

	//Flags for client commands
	public boolean get;		
	public boolean pos;	


	//Flag defines
	private final static int synFlag = 0x01;
	private final static int ackFlag = 0x02;
	private final static int finFlag = 0x04;
	private final static int eofFlag = 0x08;
	private final static int conFlag = 0x10;
	private final static int datFlag = 0x20;
	private final static int getFlag = 0x40;
	private final static int posFlag = 0x80;



	public RTPPacketHeader() {
		packetHeader = new byte[header_length]; 
	}




	public RTPPacketHeader(int seqNum, int ackNum, short srcPort, short destPort) {
		this.seqNum = seqNum;
		this.ackNum = ackNum;
		this.srcPort = srcPort;
		this.destPort = destPort;
		packetHeader = new byte[header_length]; 
	}


	public byte[] createHeader() {

		//2 bytes
		this.packetHeader[0] = (byte)(this.srcPort >> 8);
		this.packetHeader[1] = (byte)(this.srcPort & 0xFF);

		//2 bytes
		this.packetHeader[2] = (byte)(this.destPort >> 8);
		this.packetHeader[3] = (byte)(this.destPort & 0xFF);

		//4 bytes
		this.packetHeader[4] = (byte)(this.seqNum >> 24);
		this.packetHeader[5] = (byte)(this.seqNum >> 16);
		this.packetHeader[6] = (byte)(this.seqNum >> 8);
		this.packetHeader[7] = (byte)(this.seqNum & 0xFF);

		//4 bytes
		this.packetHeader[8] = (byte)(this.ackNum >> 24);
		this.packetHeader[9] = (byte)(this.ackNum >> 16);
		this.packetHeader[10] = (byte)(this.ackNum >> 8);
		this.packetHeader[11] = (byte)(this.ackNum & 0xFF);

		//4 bytes
		this.packetHeader[12] = (byte)(this.checksum >> 24);
		this.packetHeader[13] = (byte)(this.checksum >> 16);
		this.packetHeader[14] = (byte)(this.checksum >> 8);
		this.packetHeader[15] = (byte)(this.checksum & 0xFF);

		//1 byte
		this.packetHeader[16] = (byte)(this.header_length & 0xFF);

		//Flags - 1 byte
		setHeaderFlags();

		return this.packetHeader;
	}



	public void setHeaderFlags() {
		this.packetHeader[17] = 0;

		//Multiple if statements because a packet can have multiple flags
		if (syn) {
			this.packetHeader[17] = (byte)(this.packetHeader[17] | synFlag);
		}
		if (ack) {
			this.packetHeader[17] = (byte)(this.packetHeader[17] | ackFlag);
		}
		if (fin) {
			this.packetHeader[17] = (byte)(this.packetHeader[17] | finFlag);
		}
		if (eof) {
			this.packetHeader[17] = (byte)(this.packetHeader[17] | eofFlag);
		}
		if (con) {
			this.packetHeader[17] = (byte)(this.packetHeader[17] | conFlag);
		}
		if (dat) {
			this.packetHeader[17] = (byte)(this.packetHeader[17] | datFlag);
		}
		if (get) {
			this.packetHeader[17] = (byte)(this.packetHeader[17] | getFlag);
		}
		if (pos) {
			this.packetHeader[17] = (byte)(this.packetHeader[17] | posFlag);
		}
	}


	public void makeHeaderFromPacketData(byte[] packetData) {

		//Combine first 2 bytes of packet data to get source port
	 	this.srcPort = (short)(packetData[0] << 8 | ((short)0 | 0xFF) & packetData[1]);

		//Combine second 2 bytes of packet data to get destination port
		this.destPort = (short)(packetData[2] << 8 | ((short)0 | 0xFF) & packetData[3]);	

		//Combine next 4 bytes of packet data to get sequence number
		this.seqNum = (int)(packetData[4] << 24 | packetData[5] << 16 | packetData[6] << 8 | ((short)0 | 0xFF) & packetData[7]);		

		//Combine next 4 bytes of packet data to get acknowledgement number
		this.ackNum = (int)(packetData[8] << 24 | packetData[9] << 16 | packetData[10] << 8 | ((short)0 | 0xFF) & packetData[11]);		

		//Combine next 4 bytes of packet data to get checksum
		this.checksum = (int)(packetData[12] << 24 | packetData[13] << 16 | packetData[14] << 8 | packetData[15]);	

		//Get last byte of packet data to get header flags
		getHeaderFlags(packetData);
	}


	public void getHeaderFlags(byte[] packetData) {

		//Multiple if statements because a packet can have multiple flags
		if ((byte)(packetData[17] & synFlag) == (byte)synFlag) {
			this.syn = true;
		}
		if ((byte)(packetData[17] & ackFlag) == (byte)ackFlag) {
			this.ack = true;
		}
		if ((byte)(packetData[17] & finFlag) == (byte)finFlag) {
			this.fin = true;
		}
		if ((byte)(packetData[17] & eofFlag) == (byte)eofFlag) {
			this.eof = true;
		}
		if (((byte)packetData[17] & conFlag) == (byte)conFlag) {
			this.con = true;
		}
		if ((byte)(packetData[17] & datFlag) == (byte)datFlag) {
			this.dat = true;
		}
		if ((byte)(packetData[17] & getFlag) == (byte)getFlag) {
			this.get = true;
		}
		if ((byte)(packetData[17] & posFlag) == (byte)posFlag) {
			this.pos = true;
		}
	}
}

