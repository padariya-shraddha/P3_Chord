package chord;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
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
	List<AntiFinger> antiFingerTable;

	public ServerThread(Socket s,int portNumber,int hostKey,String ipAddr,Node node,Finger finger,Node successorNode,Node predecessorNode,List<Finger> fingerTable,int M,List<String> dataList,List<AntiFinger> antiFingerTable){
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
		this.antiFingerTable=antiFingerTable;
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
			//System.out.println("Message Recieved");
			if (modelObj != null) {
				System.out.println("Request received for command " + modelObj.command);
				
				if (modelObj.command.equals("add")) {
					int newNodeKey = Integer.parseInt(modelObj.addObject.get(0));
					String newNodeIp  = modelObj.addObject.get(1);
					int newNodePort = Integer.parseInt(modelObj.addObject.get(2));
					
					System.out.println("newNodeKey :"+newNodeKey);
					System.out.println("newNodeIp :"+newNodeIp);
					System.out.println("newNodePort :"+newNodePort);
					
					modelObj.response = addNodeToChord(modelObj);
				} else if (modelObj.command.equals("add_PassFingerTableAndData")){
					updateNewHostFingerTable(modelObj);
					System.out.println("updateNewHostFingerTable");
					modelObj.response= true;
				}
				else if (modelObj.command.equals("fixFinger_validateRange")) {
                    modelObj.response_message =  fixFinger_validateRange(modelObj);
                } else if (modelObj.command.equals("delete")) {
					Operation.deleteMethod(modelObj,node,fingerTable,dataList);
				} else if (modelObj.command.equals("update after delete")) {
					// updating the finger table after neighbour is deleted i.e., when the successor or the the predecssor is deleted
					updateAfterDelete(modelObj);
					output_disable = true;
				}else if(modelObj.command.equals("updateSuccessor")){
					
					if (modelObj.successor != null) {
						//System.out.println("updateSuccessor : "+modelObj.successor.getId());
						node.setSuccessor(modelObj.successor);
					}
					modelObj.response= true;
				} else if (modelObj.command.equals("out")) {
					Operation.outMethod(modelObj,M,node,fingerTable,dataList);
					modelObj.response=true;
					output_disable = true;
				} else if(modelObj.command.equals("in")) {
					Operation.printDataTable(dataList);
					Operation.inMethod(modelObj, M, node, fingerTable, dataList);
					modelObj.response=true;
					output_disable = true;
				} else if(modelObj.command.equals("successfully added")) {
					outSuccess(modelObj);
					output_disable = true;
				} else if (modelObj.command.equals("successfully found")) {
					inSuccess(modelObj);
					output_disable = true;
				}
				else{
					System.out.println("serverThread : else part");
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
		System.out.println("OLA 1");
		boolean returnFlag = true;
		int newNodeKey = Integer.parseInt(modelObj.addObject.get(0));
		String newNodeIp  = modelObj.addObject.get(1);
		int newNodePort = Integer.parseInt(modelObj.addObject.get(2));
		//check key to add is in self range
		int currentNodeKey = node.getId();
		int currentNodeScrKey = node.getSuccessor().getId();
		int currentNodePredKey = node.getPredecessor().getId();

		if (Operation.checkSpanRange1(currentNodePredKey,currentNodeKey,newNodeKey,true, M)) {
			System.out.println("OLA 2");
			try{
				
				System.out.println("OLA 3..");
				
				Node tempPred = node.getPredecessor();
				Node temp = new Node(newNodeKey,newNodeIp,newNodePort);
				node.setPredecessor(temp); // set new node as a predecessor
				updateFingerTable(modelObj,newNodeKey);
				
				System.out.println("OLA 4..");
				
				passFingerTableAndDataToNewNode(modelObj,tempPred, newNodeKey);

				//passDataToNewNode();
				
				//updateAntiFingerTable(modelObj,newNodeKey);
				System.out.println("OLA 5...");
				//SEND REQUEST TO PREVIOUS PREDE TO UPDATE ITS SUCCESSOR
				MyNetwork obj = new MyNetwork();
				obj.command = "updateSuccessor";
				obj.successor= temp;
				returnFlag = Operation.sendRequest(tempPred.getIp(), tempPred.getPortNo(), obj);
				System.out.println("OLA 6...");
				/*System.out.println("Successor "+node.getSuccessor().getId()+" "+node.getSuccessor().getIp()+" "+node.getSuccessor().getPortNo()+
						" predecessor "+node.getPredecessor().getId()+" "+node.getPredecessor().getIp()+" "+node.getPredecessor().getPortNo());
				Operation.printFingerTable(fingerTable);*/
			}
			catch(Exception e){
				returnFlag = false;
			}
			returnFlag = true;
		}
		else{	//else pass it to next Successor;
			System.out.println("OLA 3");
			String ip = null;
			int port = -1;

			if(Operation.checkSpanRange1(currentNodeKey,currentNodeScrKey,newNodeKey,true,M))
			{
				System.out.println("OLA 4");
				ip = node.getSuccessor().getIp();
				port = node.getSuccessor().getPortNo();
			}
			else{
				System.out.println("OLA 5");
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
			System.out.println("IP"+ip);
			System.out.println("port"+port);
			//reroute add node request 
			returnFlag= Operation.sendRequest(ip,port,modelObj);
		}
		
		return returnFlag;
	}


	private void updateAfterDelete(MyNetwork modelObj) {

		System.out.println("updateAfterDelete :Currsucc "+node.getSuccessor().getId()+" currPred"+node.getPredecessor().getId() +" modelObj.nodeToDeleteId "+modelObj.nodeToDeleteId);
		// updating the immediate predecessor of the  successor node which is going to be  deleted
		if(node.getSuccessor().getId() == modelObj.nodeToDeleteId) {
			
			//node.setSuccessor(modelObj.nodeToDelete.getSuccessor());
			node.setSuccessor(modelObj.successor);
			//node.setSuccessor(modelObj.nodeToDelete.getSuccessor());
		}
		// updating the immediate successor node of the predecessor node which is going to be deleted.
		if(node.getPredecessor().getId() == modelObj.nodeToDeleteId) {
			//node.setPredecessor(modelObj.nodeToDelete.getPredecessor());
			node.setPredecessor(modelObj.predecessor);
			//node.setPredecessor(modelObj.nodeToDelete.getPredecessor());
			// adding the data strings from the predecessor which is going to be deleted.
			dataList.addAll(modelObj.dataList);
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
		System.out.println("Successor "+node.getSuccessor().getId()+" "+node.getSuccessor().getIp()+" "+node.getSuccessor().getPortNo()+
				" predecessor "+node.getPredecessor().getId()+" "+node.getPredecessor().getIp()+" "+node.getPredecessor().getPortNo());
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

	public void passFingerTableAndDataToNewNode(MyNetwork modelObj,Node previousPred, int newNodeKey){
	
	
		String ip = modelObj.addObject.get(1);
		int port = Integer.parseInt(modelObj.addObject.get(2)); 
		
		System.out.println("ip :"+ip);
		System.out.println("newNodeIp :"+port);
		
		MyNetwork obj = new MyNetwork();
		obj.command = "add_PassFingerTableAndData";
		obj.fingerTable = fingerTable;
		obj.predecessor= previousPred;
		obj.successor=node;
		obj.dataList = passDataToNewNode(modelObj, newNodeKey);
		
		System.out.println("before request send");
		Operation.sendRequest(ip,port,obj);
		System.out.println("after request send");
	}
	
	// function to decide which data keys to transfer
	public List<String> passDataToNewNode(MyNetwork modelObj, int newNodeKey) {
		System.out.println("in passDataToNewNode 1");
		List<String> DataToTransfer = new ArrayList<String>();
		List<String> newDataList = new ArrayList<String>();
		for(String data : dataList) {
			int hashKey = Operation.getmd5Modulo(data, M); 
			if (hashKey <= newNodeKey) {
				DataToTransfer.add(data);
				//dataList.remove(data);
			}
			else{
				newDataList.add(data);
			}
		}
		
		dataList = newDataList;
		
		System.out.println("in passDataToNewNode 2");
		System.out.println("DataToTransfer size :"+DataToTransfer.size());
		return DataToTransfer;
		
	}

	public void updateNewHostFingerTable(MyNetwork modelObj){
		
		//get successor's finger table 
		List<Finger> succFingerTable = modelObj.fingerTable;

		//update pred
		node.setPredecessor(modelObj.predecessor);

		//update succ
		node.setSuccessor(modelObj.successor);
		
		// transferring the data from the successor when a new node is added
		//dataList.addAll(modelObj.dataList);
		dataList = modelObj.dataList;
		System.out.println("Size "+ dataList.size());
		System.out.println("Network Size "+ modelObj.dataList.size());
		Operation.printDataTable(dataList);
		
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
		
		/*System.out.println("Successor "+node.getSuccessor().getId()+" "+node.getSuccessor().getIp()+" "+node.getSuccessor().getPortNo()+
				" predecessor "+node.getPredecessor().getId()+" "+node.getPredecessor().getIp()+" "+node.getPredecessor().getPortNo());
		Operation.printFingerTable(fingerTable);*/
		System.out.println("added in Chord Network");
		System.out.print("chord >");
	}

	//public Node fixFinger_validateRange(MyNetwork modelObj)
	public String fixFinger_validateRange(MyNetwork modelObj){

		Node responsibleNode = null;
		int keyTobeValidate = modelObj.keyTobeValidate;
		boolean validate = false;
		if(node.getPredecessor().getId() == node.getId()){
			validate = true;
		}
		else{
			validate = Operation.checkSpanRange(node.getPredecessor().getId(),node.getId(),keyTobeValidate,true, M);
					
					//keyTobeValidate > node.getPredecessor().getId() &&
					//keyTobeValidate <= node.getId()) ? true : false;
		}

		System.out.println("fixFinger_validateRange "+modelObj.keyTobeValidate+ " validate "+validate);

		//if Key range to be validated doesn't fall into local host range then check the finger table
		if(!validate){
			
			System.out.println(" checkSpan "+Operation.checkSpanRange(node.getId(),node.getSuccessor().getId(),keyTobeValidate,true,M));

			if(Operation.checkSpanRange(node.getId(),node.getSuccessor().getId(),keyTobeValidate,false,M)){
					//keyTobeValidate > node.getId() && keyTobeValidate <= node.getSuccessor().getId())
				responsibleNode = new Node(node.getSuccessor().getId(),node.getSuccessor().getIp(),node.getSuccessor().getPortNo());
			}
			else{
			
			for(Finger finger : fingerTable){
				boolean check = Operation.checkSpanRange(finger.getKey(),finger.getSpan(),keyTobeValidate,false,M);
				if(check){
					responsibleNode = new Node(finger.getSuccessor(),finger.getIp(),finger.getPort());
					break;
				}
			}
		 }
		}
		else if(validate){
			responsibleNode = new Node(node.getId(),node.getIp(),node.getPortNo());
		}

		String response_string = responsibleNode.getId()+"/"+responsibleNode.getIp()+"/"+responsibleNode.getPortNo();
		//System.out.println("fixFinger_validateRange responsible node found : " +responsibleNode.getId());
		//System.out.println("response_string "+response_string);
        System.out.println("sendRequest : "+response_string+" keyTobeValidate "+keyTobeValidate);

		return response_string;

	}

	public void passDataToNewHost(MyNetwork modelObj){

	}

	/*public void passAntiFingerTableToNewNode(int newNodeKey){

    }*/

	public void outSuccess(MyNetwork modelObj) {
		System.out.println("The data "+modelObj.dataString +" is successfully added on node "+ modelObj.respondedNodeId);
		System.out.println("chord>");
	}
	
	public void inSuccess(MyNetwork modelObj) {
		System.out.println("The data "+modelObj.dataString +" is successfully found on node "+ modelObj.respondedNodeId);
		System.out.println("chord>");
	}


}

