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
	List<String> dataList;
	boolean output_disable = false;

	public ServerThread(Socket s,int portNumber,int hostKey,String ipAddr,Node node,Finger finger,Node successorNode,Node predecessorNode,List<Finger> fingerTable,int M,List<String> dataList){
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
		this.dataList=dataList;
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
				//System.out.println("Request received for command " + modelObj.command);

				if (modelObj.command.equals("add")) {
					modelObj.response = addNodeToChord(modelObj);
				} else if (modelObj.command.equals("add_PassFingerTable")){
					updateNewHostFingerTable(modelObj);
					modelObj.response= true;
				}
				else if (modelObj.command.equals("fixFinger_validateRange")) {
                    modelObj.response_message =  fixFinger_validateRange(modelObj);
                } else if (modelObj.command.equals("delete")) {
					Operation.deleteMethod(modelObj,node,fingerTable);
				} else if (modelObj.command.equals("update after delete")) {
					// updating the finger table after neighbour is deleted i.e., when the successor or the the predecssor is deleted
					updateAfterDelete(modelObj);
					output_disable = true;
				}else if(modelObj.command.equals("updateSuccessor")){
					
					if (modelObj.successor != null) {
						node.setSuccessor(modelObj.successor);
					}
					modelObj.response= true;
				} else if (modelObj.command.equals("out")) {
					Operation.outMethod(modelObj,M,node,fingerTable,dataList);
					modelObj.response=true;
				} else if(modelObj.command.equals("in")) {
					Operation.inMethod(modelObj, M, node, fingerTable, dataList);
				} else if(modelObj.command.equals("successfully added")) {
					outSuccess(modelObj);
				} else if (modelObj.command.equals("successfully found")) {
					inSuccess(modelObj);
				}
			}
			
			if(!output_disable) //change by nidhi due to update Delete function
			{
				out.writeObject(modelObj);
			}
			else{
				output_disable = false;
			}
			

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

		if (Operation.checkSpanRange1(currentNodePredKey,currentNodeKey,newNodeKey,true, M)) {
			try{
				Node tempPred = node.getPredecessor();
				Node temp = new Node(newNodeKey,newNodeIp,newNodePort);
				node.setPredecessor(temp); // set new node as a predecessor
				updateFingerTable(modelObj,newNodeKey);
				passFingerTableToNewNode(modelObj,tempPred);

				//passDataToNewNode();
				//updateAntiFingerTable(modelObj,newNodeKey);

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

			if(Operation.checkSpanRange1(currentNodeKey,currentNodeScrKey,newNodeKey,true,M))
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
		
		return returnFlag;
	}


	private void updateAfterDelete(MyNetwork modelObj) {

		// updating the immediate successor if the successor node is deleted
		if(node.getSuccessor().getId() == modelObj.nodeToDeleteId) {
			
			//node.setSuccessor(modelObj.nodeToDelete.getSuccessor());
			node.setSuccessor(modelObj.successor);
		}
		// updating the immediate predecessor if the successor node is deleted
		else if(node.getPredecessor().getId() == modelObj.nodeToDeleteId) {
			//node.setPredecessor(modelObj.nodeToDelete.getPredecessor());
			node.setPredecessor(modelObj.predecessor);
		}
		for (Finger finger : fingerTable) {
			int keyStart = finger.getKey();
			int keyEnd = finger.getSpan();

			if(finger.getSuccessor() == modelObj.nodeToDeleteId){
				finger.setSuccessorNode(modelObj.successor.getId());
				finger.setip(modelObj.successor.getIp());
				finger.setPort(modelObj.successor.getPortNo());
			}
		}
		
		System.out.println("Finger table After removing node "+modelObj.nodeToDeleteId);
		Operation.printFingerTable(fingerTable);
	}
	
	//need to delete it...just kept it for reference
	public void updateFingerTable_proto(MyNetwork modelObj,int newNodeKey){
		for (Finger finger : fingerTable) {
			int keyStart = node.getId();
			int keyEnd = node.getPredecessor().getId();
			
			//if( newNodeKey >= keyStart && finger.getSuccessor() > newNodeKey)
			System.out.println("keyStart :"+keyStart);
			System.out.println("keyEnd :"+keyEnd);
			System.out.println("newNodeKey :"+newNodeKey);
			if(Operation.checkSpanRange(keyStart,keyEnd,newNodeKey,true,M)){
				finger.setSuccessorNode(newNodeKey);
			}
		}
		//System.out.println("updateFingerTable:");
		//Operation.printFingerTable(fingerTable);
		
	}
	
	public void updateFingerTable(MyNetwork modelObj,int newNodeKey){
		int selfId =node.getId();
		//System.out.println("selfId: "+selfId);
		for (Finger finger : fingerTable) {
			int successorNodeId = finger.getSuccessor();
			
			int temp_newNodeKey = (newNodeKey+1)%((int)Math.pow(2, M));
			
			boolean rangeCheckFlag = Operation.checkSpanRange1(temp_newNodeKey, selfId, finger.getKey(), true, M);
			
			if (successorNodeId ==selfId && (rangeCheckFlag==false)) {
				finger.setSuccessorNode(newNodeKey);
				finger.setip(modelObj.addObject.get(1));
				finger.setPort(Integer.parseInt(modelObj.addObject.get(2)));
			}
		}
		
		//Operation.printFingerTable(fingerTable);
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
			if (Operation.checkSpanRange1(updateRangeStart,updateRangeEnd,tempKey,true,M)) {	//if key falls between span
				finger.setSuccessorNode(updateRangeEnd);
				finger.setip(node.getSuccessor().getIp());
				finger.setPort(node.getSuccessor().getPortNo());
			}else{	//calculate it from successor's finger table
				for (Finger finger2 : succFingerTable) {
					int temp_start = finger2.getKey();
					int temp_end = finger2.getSpan();
					boolean flag = Operation.checkSpanRange1(temp_start,temp_end,tempKey,false,M);
					if (flag) {
						finger.setSuccessorNode(finger2.getSuccessor());
						finger.setip(finger2.getIp());
						finger.setPort(finger2.getPort());
						break;
					}
				}
			}	
		}
		
		//Operation.printFingerTable(fingerTable);
		System.out.println("added in Chord Network");
		System.out.print("chord >");
	}

	//public Node fixFinger_validateRange(MyNetwork modelObj)
	public String fixFinger_validateRange(MyNetwork modelObj){

		System.out.println("fixFinger_validateRange "+modelObj.keyTobeValidate);
		Node responsibleNode = null;
		int keyTobeValidate = modelObj.keyTobeValidate;
		boolean validate = false;
		if(node.getPredecessor().getId() == node.getId()){
			validate = true;
		}
		else{
			validate = (keyTobeValidate > node.getPredecessor().getId() &&
					keyTobeValidate <= node.getId()) ? true : false;
		}

		//if Key range to be validated doesn't fall into local host range then check the finger table
		if(!validate){
			for(Finger finger : fingerTable){
				boolean check = Operation.checkSpanRange(finger.getKey(),finger.getSpan(),keyTobeValidate,true,M);
				if(check){
					responsibleNode = new Node(finger.getKey(),finger.getIp(),finger.getPort());
					break;
				}
			}
		}
		else if(validate){
			responsibleNode = new Node(node.getId(),node.getIp(),node.getPortNo());
		}

		String response_string = node.getId()+"/"+node.getIp()+"/"+node.getPortNo();
		//System.out.println("fixFinger_validateRange responsible node found : " +responsibleNode.getId());
		//System.out.println("response_string "+response_string);
		return response_string;

	}

	public void passDataToNewHost(MyNetwork modelObj){

	}

	/*public void passAntiFingerTableToNewNode(int newNodeKey){

    }*/

	public void outSuccess(MyNetwork modelObj) {
		System.out.println("The data "+modelObj.dataString +"is successfully added in"+ modelObj.respondedNodeId);
	}
	
	public void inSuccess(MyNetwork modelObj) {
		System.out.println("The data "+modelObj.dataString +"is successfully found in"+ modelObj.respondedNodeId);
	}


}

