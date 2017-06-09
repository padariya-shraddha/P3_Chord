package chord;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class MyServer extends Thread{

	int portNumber;
	int hostKey;
	String ipAddr;
	ServerSocket serverSocket;
	Node node;
	Finger finger;
	Node successorNode;
	Node predecessorNode;
	List<Finger> fingerTable;
	int totalNodes;
	int M;
	List<String> dataList;
	List<AntiFinger> antiFingerTable;
	LRUCache cache;

	public MyServer(ServerSocket serverSocket,int hostKey,String ipAddr,int portNumber,List<Finger> fingerTable,Node node,Finger finger,Node successorNode,Node predecessorNode,int M,List<String> dataList,List<AntiFinger> antiFingerTable, LRUCache cache){
		//it will have finger table, successor, predecessor as arguments
		this.serverSocket = serverSocket;
		this.portNumber = portNumber;
		this.hostKey = hostKey;
		this.ipAddr = ipAddr;
		this.node = node;
		this.finger = finger;
		this.predecessorNode = predecessorNode;
		this.successorNode = successorNode;
		this.fingerTable = fingerTable;
		this.M = M;
		totalNodes =  (int) Math.pow(2, M);
		this.dataList=dataList;
		this.antiFingerTable=antiFingerTable;
		this.cache = cache;
	} 

	public void run(){
		Socket s=null;
		ServerSocket ss=null;

		try{
			while(true){
				s= serverSocket.accept();
				ServerThread st=new ServerThread(s,portNumber,hostKey,ipAddr,node,finger,successorNode,predecessorNode,fingerTable,M,dataList,antiFingerTable,cache);
				st.start();
			}
		}
		catch(IOException e){
			e.printStackTrace();
			System.out.println("Server error");

		}finally{
			try {
				if (ss != null) {
					ss.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}

