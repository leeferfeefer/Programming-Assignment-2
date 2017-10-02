//Dan Fincher
//Drew Ritter

//SendThread.java

public class SendThread extends Thread {
	
	private String file;
	private RTPProtocol rtp;
	
	public SendThread(String file, RTPProtocol rtp) {
		this.file = file;
		this.rtp = rtp;
	}

	//Start thread
	//Start sending data
	public void run() {
		rtp.sendData(file);
	}
}