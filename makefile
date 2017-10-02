# Dan Fincher
# Programming Assignment 2 makefile
# Platform: OSX


# Define compiler and flag variables
JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
		$(JC) $(JFLAGS) $*.java

# Macro defining classes to compile
CLASSES = \
		FxAClient.java \
		FxAServer.java \
		RTPProtocol.java \
		RTPPacketHeader.java \
		ReceiveThread.java \
		SendThread.java \
		PacketQueue.java \
		Timer.java \
		Window.java 

# Default make target entry
default: classes

# Use macro to define .java = .class suffix
classes: $(CLASSES:.java=.class)

# Remove class files
clean: 
		$(RM) *.class