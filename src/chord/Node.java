package chord;

import java.io.Serializable;
/**
 * @author Team-6
 * @description This class has been used to store all the information about node IP address,port and unique
 * identifier which has been assigned
 */
public class Node implements Serializable{
	private int id;
	private String ip;
	private int portNo;
	private Node successor;
	private Node predecessor;
    
 
	
	public Node(int id, String ip, int portNo) {
		this.id =id;
		this.ip = ip;
		this.portNo = portNo;
		this.successor= this;
		this.predecessor= this;
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
