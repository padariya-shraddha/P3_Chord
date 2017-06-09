package chord;

import java.io.Serializable;

public class Node implements Serializable{
	private int id;
	private String ip;
	private int portNo;
	private Node successor;
	private Node predecessor;
	String key;
    Node nodeInfo;//for caching we are using to store node information
    Node pre; //for caching we are using to store node information
    Node next;//for caching we are using to store node information
 
	
	public Node(int id, String ip, int portNo) {
		this.id =id;
		this.ip = ip;
		this.portNo = portNo;
		this.successor= this;
		this.predecessor= this;
	}
	    

	public Node(String key, Node nodeInfo){
	        this.key = key;
	        this.nodeInfo = nodeInfo;
	}
	

	public void setId(int id) {
		this.id = id;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public void setPortNo(int portNo) {
		this.portNo = portNo;
	}

	public int getId() {
		return this.id;
	}

	public String getIp() {
		return this.ip;
	}

	public int getPortNo() {
		return this.portNo;
	}

	public Node getSuccessor() {
		return successor;
	}

	public void setSuccessor(Node successor) {
		this.successor = successor;
	}

	public Node getPredecessor() {
		return predecessor;
	}

	public void setPredecessor(Node predecessor) {
		this.predecessor = predecessor;
	}

}
