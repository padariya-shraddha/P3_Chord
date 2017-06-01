package chord;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

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
	int totalNodes;
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
		totalNodes =  (int) Math.pow(2, M);
	}

	//COMMAND NAME UPADTE: "update finger table after neighbour is deleted"
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
					modelObj.response= true;
				}
				else if (modelObj.command.equals("delete")) {
					Operation.deleteMethod(modelObj,node,fingerTable);
				}
				else if (modelObj.command.equals("update after delete")) {
					// updating the finger table after neighbour is deleted i.e., when the successor or the the predecssor is deleted
					updateAfterDelete(modelObj);
				}else if(modelObj.command.equals("updateSuccessor")){
					if (modelObj.successor != null) {
						node.setSuccessor(modelObj.successor);
					}
					modelObj.response= true;
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

		if (Operation.checkSpanRange(currentNodePredKey,currentNodeKey,newNodeKey,true, M)) {
			try{
				Node tempPred = node.getPredecessor();
				Node temp = new Node(newNodeKey,newNodeIp,newNodePort);
				node.setPredecessor(temp); // set new node as a predecessor
				updateFingerTable(modelObj,newNodeKey);
				passFingerTableToNewNode(modelObj,tempPred);

				//passDataToNewNode();
				//updateAntiFingerTable(modelObj,newNodeKey);

				System.out.println("updated finger table After adding "+newNodeKey+ " ");
				P3 p3 = new P3();
				p3.printFingerTable();
				System.out.println();

				//SEND REQUEST TO PREVIOUS PREDE TO UPDATE ITS SUCCESSOR
				MyNetwork obj = new MyNetwork();
				obj.command = "updateSuccessor";
				obj.successor= temp;
				returnFlag = Operation.sendRequest(tempPred.getIp(), tempPred.getPortNo(), obj);
			}
			catch(Exception e){
				returnFlag = false;
			}
			returnFlag = true;
		}
		else{	//else pass it to next Successor;

			String ip = null;
			int port = -1;

			if(Operation.checkSpanRange(currentNodeKey,currentNodeScrKey,newNodeKey,true,M))
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
						break;
					}
				}
			}
			//reroute add node request 
			returnFlag= Operation.sendRequest(ip,port,modelObj);
		}
		System.out.println("updated finger table After adding "+newNodeKey+ " ");
		P3 p3 = new P3();
		p3.printFingerTable();
		System.out.println();

		return returnFlag;
	}

	
	private void updateAfterDelete(MyNetwork modelObj) {

		// updating the immediate successor if the successor node is deleted
		if(node.getSuccessor().getId() == modelObj.nodeToDeleteId) {
			node.setSuccessor(modelObj.nodeToDelete.getSuccessor());
		}
		// updating the immediate predecessor if the successor node is deleted
		else if(node.getPredecessor().getId() == modelObj.nodeToDeleteId) {
			node.setPredecessor(modelObj.nodeToDelete.getPredecessor());
		}
		for (Finger finger : fingerTable) {
			int keyStart = finger.getKey();
			int keyEnd = finger.getSpan();

			if(finger.getSuccessor() == modelObj.nodeToDelete.getId()){
				finger.setSuccessorNode(modelObj.nodeToDelete.getSuccessor().getId());
				finger.setip(modelObj.nodeToDelete.getSuccessor().getIp());
				finger.setPort(modelObj.nodeToDelete.getSuccessor().getPortNo());
			}
		}
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
		MyNetwork obj = new MyNetwork();
		obj.command = "add_PassFingerTable";
		obj.fingerTable = fingerTable;
		obj.predecessor= previousPred;
		obj.successor=node;

		Operation.sendRequest(ip,port,obj);
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
			if (Operation.checkSpanRange(updateRangeStart,updateRangeEnd,tempKey,true,M)) {	//if key falls between span
				finger.setSuccessorNode(updateRangeEnd);
			}else{	//calculate it from successor's finger table
				for (Finger finger2 : succFingerTable) {
					int temp_start = finger2.getKey();
					int temp_end = finger2.getSpan();
					boolean flag = Operation.checkSpanRange(temp_start,temp_end,tempKey,false,M);
					if (flag) {
						finger.setSuccessorNode(finger2.getSuccessor());
						break;
					}
				}
			}	
		}
	}

	public void passDataToNewHost(MyNetwork modelObj){

	}

	/*public void passAntiFingerTableToNewNode(int newNodeKey){

    }*/

}

