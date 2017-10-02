Dan Fincher Section B dfincher3@gatech.edu
Drew Ritter Section A dritter3@gatech.edu
11/25/2015
Programming Assignment 2

Platform tested on: OSX


!!!!
NOTE:
!!!!

Make sure to open server and client in different file directories when posting or getting data. This ensures that data being written to a pre-existing file with the same name in the directory is not infinitely looped.




Files Submitted:
makefile - compiles all java files
Updated Design Report.pdf - this highlights all changes we made to our design
FxAClient.java - you will run this to represent the client
FxAServer.java - you will run this to represent the server
ReceiveThread.java - creates a thread for incoming data
SendThread.java - creates a thread for sending data
RTPPacketHeader.java - contains the structure for the header (actual byte array, and variables for each field)
RTPProtocol.java - contains all the logic of the protocol (checksum, order, connection, etc.)
PacketQueue.java - helps with keeping the order of packets correct
Timer.java - helps with detecting Timeouts
Window.java - holds logic for window size (default window size is 1)
alice.txt - A text file used for testing RTP
Sample.txt - Sample output of what the client, server, and net emu print when in debug mode for POST and GET methods as well as connect and disconnect when the window size is 1 as well as 5


To compile all java files using makefile:
	type "make" (generates class files)
	to remove class files type "make clean"

How to run program:

To start the NetEmu:	python NetEmu.py "NetEmu port num" -l "loss prob" -c "corruption -d "duplication" -r "reorder" -D "avg packet delay"
To start client:	java FxAClient "even port num" "inet address" "NetEmu port num" -d(debug flag)
	Commands: connect, get "filename", post "filename", window "win size", disconnect
To start server:	java FxAServer "clientport+1" "inet address" "NetEmu port num" -d(debug flag)
	Commands: window "win size", terminate(shuts down server)

Known Bugs/Limitations:
terminating gracefully when the client is posting a file to server doesn't complete file transfer

