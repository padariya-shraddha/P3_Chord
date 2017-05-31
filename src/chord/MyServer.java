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
	int totalNodes= 64;
	int M;


	public MyServer(ServerSocket serverSocket,int hostKey,String ipAddr,int portNumber,List<Finger> fingerTable,Node node,Finger finger,Node successorNode,Node predecessorNode,int M){

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
	} 

	public void run(){
		Socket s=null;
		ServerSocket ss=null;

		try{
			while(true){
				s= serverSocket.accept();
				ServerThread st=new ServerThread(s,portNumber,hostKey,ipAddr,node,finger,successorNode,predecessorNode,fingerTable,M);
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

class ServerThread extends Thread{ 
	int portNumber;
	int hostKey;
	String ipAddr;
	Socket s=null;
	Node node;
	Finger finger;
	Node successorNode;
	Node predecessorNode;
	List<Finger> fingerTable;
	int totalNodes= 64;
	int M;

	public ServerThread(Socket s,int portNumber,int hostKey,String ipAddr,Node node,Finger finger,Node successorNode,Node predecessorNode,List<Finger> fingerTable,int M){
		this.s = s;
		this.portNumber = portNumber;
		this.hostKey = hostKey;
		this.ipAddr = ipAddr;
		this.node= node;
		this.finger = finger;
		this.predecessorNode = predecessorNode;
		this.successorNode = successorNode;
		this.fingerTable = fingerTable;
		this.M = M;
	}

	public void run() {
		//process request
		ObjectInputStream in = null;
		ObjectOutputStream out = null;
		MyNetwork modelObj = null;
		try {
			out= new ObjectOutputStream(s.getOutputStream());
			in=new ObjectInputStream(s.getInputStream());
			modelObj = (MyNetwork) in.readObject();

			if (modelObj != null) {
				System.out.println("Request received for command " + modelObj.command);

				//process query here
				if (modelObj.command=="add") {
					modelObj.response = addNodeToChord(modelObj);
				} else if (modelObj.command == "add_PassFingerTable"){
					updateNewHostFingerTable(modelObj);
				}
			}
			out.writeObject(modelObj);

		}catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();

		}
		finally{
			if (s!=null){
				try {
					s.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public boolean addNodeToChord(MyNetwork modelObj){
		boolean returnFlag = true;
		int newNodeKey = Integer.parseInt(modelObj.addObject.get(0));
		String newNodeIp  = modelObj.addObject.get(1);
		int newNodePort = Integer.parseInt(modelObj.addObject.get(2));
		//check key to add is in self range
		int currentNodeKey = node.getId();
		int currentNodeScrKey = node.getSuccessor().getId();
		int currentNodePredKey = node.getPredecessor().getId();

		if (checkSpanRange(currentNodePredKey,currentNodeKey,newNodeKey,true)) {
			try{
				Node tempPred = node.getPredecessor();
				Node temp = new Node(newNodeKey,newNodeIp,newNodePort);
				node.setPredecessor(temp); // set new node as a predecessor
				updateFingerTable(modelObj,newNodeKey);
				passFingerTableToNewNode(modelObj,tempPred);
				
				//passDataToNewNode();
				//updateAntiFingerTable(modelObj,newNodeKey);
			}
			catch(Exception e){
				returnFlag = false;
			}
			returnFlag = true;
		}
		else{	//else pass it to next Successor;

			String ip = null;
			int port = -1;
			
			//newNodeKey > currentNodeKey && (newNodeKey <= currentNodeScrKey |(newNodeKey <= currentNodeScrKey + Math.pow(2, M))))
			if(checkSpanRange(currentNodeKey,currentNodeScrKey,newNodeKey,true))
			{
				ip = node.getSuccessor().getIp();
				port = node.getSuccessor().getPortNo();
			}
			else{
				for (Finger finger : fingerTable) {
					int keyStart = finger.getKey();
					int keyEnd = (finger.getSpan() < keyStart) ? finger.getSpan() : (int) (finger.getSpan() + Math.pow(2, M));
					if(newNodeKey >= keyStart && newNodeKey < keyEnd){
						ip = finger.getIp();
						port = finger.getPort();
					}
				}
			}

			Socket s1;
			try {
				s1 = new Socket(ip, port);
				ObjectOutputStream out = new ObjectOutputStream(s1.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(s1.getInputStream());
				out.writeObject(modelObj);
				MyNetwork response = (MyNetwork) in.readObject();
				returnFlag = response.response;
				in.close();
				out.close();
				s1.close();

			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			} 
		}

		System.out.println("updated finger table After adding "+newNodeKey+ " ");
		P3 p3 = new P3();
		p3.printFingerTable();
		System.out.println();

		return returnFlag;
	}

    public boolean checkSpanRange(int start,int end,int searchKey,boolean flag){
    	boolean result = false;
    	int keyStart;
    	int keyEnd;
    	
    	keyEnd = (start<end) ? end : (int) (end + Math.pow(2, M));
    	
    	if(flag && (searchKey >= start && searchKey <= end)) {result = true;}
    	if(!flag && (searchKey >= start && searchKey < end)) {result = true;}
    	
    	return result;
    	
    }

	public void updateFingerTable(MyNetwork modelObj,int newNodeKey){

		for (Finger finger : fingerTable) {
			int keyStart = finger.getKey();
			int keyEnd = finger.getSpan();

			if(newNodeKey >= keyStart && finger.getSuccessor() > newNodeKey){
				finger.setSuccessorNode(newNodeKey);
			}
		}
	}

	//To-Do
	/*public void updateAntiFingerTable(MyNetwork modelObj,int newNodeKey){

    }*/


	public void passFingerTableToNewNode(MyNetwork modelObj,Node previousPred){

		String ip = modelObj.addObject.get(1);
		int port = Integer.parseInt(modelObj.addObject.get(2)); 
		Socket s1;
		try {
			s1 = new Socket(ip, port);
			ObjectOutputStream out = new ObjectOutputStream(s1.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(s1.getInputStream());
			MyNetwork obj = new MyNetwork();
			obj.command = "add_PassFingerTable";
			obj.fingerTable = fingerTable;
			obj.predecessor= previousPred;
			obj.successor=node;
			out.writeObject(obj);
			MyNetwork response = (MyNetwork) in.readObject();
			in.close();
			out.close();
			s1.close();

		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		} 
	}

	public void updateNewHostFingerTable(MyNetwork modelObj){
		

		//get successor's finger table 
		List<Finger> succFingerTable = modelObj.fingerTable;
		
		//update pred
		node.setPredecessor(modelObj.predecessor);
		
		//update succ
		node.setSuccessor(modelObj.successor);
		
		//update finger table
		int updateRangeStart =( node.getId()+1)% totalNodes ;
		int updateRangeEnd = node.getSuccessor().getId();
		
		for (Finger finger : fingerTable) {
			
			int tempKey =finger.getKey();
			if (checkSpanRange(updateRangeStart,updateRangeEnd,tempKey,true)) {	//if key falls between span
				finger.setSuccessorNode(updateRangeEnd);
			}else{	//calculate it from successor's finger table
				for (Finger finger2 : succFingerTable) {
					
				}
			}
			
		}

	}


	public void passDataToNewHost(MyNetwork modelObj){



	}

	/*public void passAntiFingerTableToNewNode(int newNodeKey){

    }*/


}
