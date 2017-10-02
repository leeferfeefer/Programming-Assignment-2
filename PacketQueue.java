//Dan Fincher
//Drew Ritter


//PacketQueue.java

import java.util.ArrayList;

public class PacketQueue<T> {
	
    //Inner class
    private class Node {
        private T data;
        private Node next;
    }

    private Node head;
    private Node tail;
    private int size;



    /*
        Queue Constructor
    */
    public PacketQueue() {
    	head = null;
    	tail = null;
    }



    /*
        Add Methods
    */
        
    synchronized public void enqueue(T data) {
        Node oldTail = tail; 
        tail = new Node();
        tail.data = data;
        size++;
        if (oldTail == null) {
            head = tail;
        } else {
            oldTail.next = tail;
        }
    }




    /*
        Remove Methods
    */

    synchronized public void dequeue() {
        if (!isEmpty()) {
            //New head is next 
            head = head.next;
            size--;
            if (size == 0) {
                head = null;
                tail = null;
            }
        } 
    }



    /*
        Array List Methods
    */

    public ArrayList<T> returnArrayList(){
    	ArrayList<T> list = new ArrayList<T>();
    	Node traverseNode = head;
    	for (int i = 0; i < size; i++) {
            //Add to list
    		list.add(traverseNode.data);
            //set next node
    		traverseNode = traverseNode.next;
    	}
    	return list;
    }



    /*
        Is Methods
    */

	public boolean isEmpty() {
		return (head == null);
	}
}