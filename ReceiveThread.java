//Dan Fincher
//Drew Ritter

//ReceiveThread.java

public class ReceiveThread extends Thread {
	
	private RTPProtocol rtp;
	
	public ReceiveThread(RTPProtocol rtp) {
		this.rtp = rtp;
	}

	//Start thread
	//Start listening for packets
	public void run() {
		rtp.listenForPackets();
	}
}