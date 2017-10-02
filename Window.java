//Dan Fincher
//Drew Ritter


//Window.java 

public class Window {

	public int startOfWindow;
	public int endOfWindow;
	public int sizeOfWindow;

	public int next;
	

	/*
		Window Constructor
	*/
	public Window() {
		sizeOfWindow = 1;
		startOfWindow = 0;
		endOfWindow = sizeOfWindow - 1;

		next = 0;
	}
}