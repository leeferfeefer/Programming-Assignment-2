//Dan Fincher
//Drew Ritter

//Timer.java

public class Timer {

	private static final boolean timedOut = true;
	private static final boolean notTimedOut = false;

	private static final int MILLSECSINSEC = 1000;
	//Half a second
	private static final double TIMEOUT = .5;
	public static final int MAXTRIES = 10;
	private long startTime;
	public static int tries;



	/*
		Timer Constructor
	*/
	public Timer() {
		startTime = 0;
		tries = 0;
	}
	
	//Start timer
	public void start() {
		//Set starting time to current system time (in milliseconds)
		startTime = System.currentTimeMillis();
	}

	//Reset timer tries
	public void resetTries() {
		tries = 0;
	}




	/*
		Is Methods
	*/

	//Checks if timeout
	public boolean isTimeout() {
		if ((TIMEOUT * MILLSECSINSEC) < (System.currentTimeMillis() - startTime)){
			return timedOut;
		} else {
			return notTimedOut;
		}
	}

	//Checks if more ties are available
	public boolean isMoreTries() {
		tries++;
		if (tries <= MAXTRIES) {
			return true;
		} else {
			return false;
		}
	}
}