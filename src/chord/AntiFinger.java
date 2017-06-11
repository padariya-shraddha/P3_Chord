package chord;

import java.io.Serializable;

/**
 * @author Team-6
 * @description This class contains a anti-finger table which has been used for bidirectional lookup chord. 
 */
public class AntiFinger implements Serializable{
	private int key;
	private int span;
	private int successorNode;
	private String ip;
	private int port;
	AntiFinger(int gap,int span,int successorNode, String ip, int port) {
		
		this.key = gap;
		this.span = span;
		this.successorNode = successorNode;
		this.ip = ip;
		this.port = port;
	}
	public void setKey(int key) {
		this.key = key;
	}
	
	public void setRange(int span) {
		this.span = span;
	}
	
	public void setSuccessorNode(int successorNode) {
		this.successorNode = successorNode;
	}
	
	public void setip(String ip) {
		this.ip = ip;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	public int getKey() {
		return this.key;
	}
	
	public int getSpan() {
		return this.span;
	}
	
	public int getSuccessor()  {
		return this.successorNode;
	}
	
	public String getIp(){
		return this.ip;
	}
	
	public int getPort(){
		return this.port;
	}
	
	public void  print () {
		System.out.print("Key =" +this.key);
		System.out.print(" : range =" +this.span);
		System.out.print(" : successor =" +this.successorNode);
		System.out.print(" : ip =" +this.ip);
		System.out.print(" :port =" +this.port);
	}
	
}
