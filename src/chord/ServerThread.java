package chord;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @author Team-6
 * @description This class serves as a server thread which continuously listen for input request
 */

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
	LRUCache cache;
	public static List<String> analysisStore = new ArrayList<>();

	public ServerThread(Socket s,int portNumber,int hostKey,String ipAddr,Node node,Finger finger,Node successorNode,Node predecessorNode,List<Finger> fingerTable,int M,List<String> dataList,List<AntiFinger> antiFingerTable, LRUCache cache){
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
		this.cache = cache;
	}

	public void run() {
		//process request
		ObjectInputStream in = null;
		ObjectOutputStream out = null;
		MyNetwork modelObj = null;
		try {
			out= new ObjectOutputStream(s.getOutputStream());
			in=new ObjectInputStream(s.getInputStream());
			if(in!=null){
			modelObj = (MyNetwork) in.readObject();
			if (modelObj != null) {
				//System.out.println("Request received for command " + modelObj.command);
				
				if (modelObj.command.equals("add")) {
					modelObj.response = addNodeToChord(modelObj);
				} else if (modelObj.command.equals("add_PassFingerTableAndData")){
					updateNewHostFingerTable(modelObj);
					modelObj.response= true;
				}
				else if (modelObj.command.equals("fixFinger_validateRange")) {
                    modelObj.response_message =  fixFinger_validateRange(modelObj);
                    
                }else if (modelObj.command.equals("fixAntiFinger_validateRange")) {
                    modelObj.response_message =  fixAntiFinger_validateRange(modelObj);
                    
                }else if (modelObj.command.equals("searchKeyForFixAntiFinger")) {
                    searchKeyForFixAntiFinger(modelObj);
                    output_disable = true;
                    
                }else if (modelObj.command.equals("searchKeyForFixFinger")) {
                    searchKeyForFixAntiFinger(modelObj);
                    output_disable = true;
                    
                } else if (modelObj.command.equals("FoundSearchKeyForFixFinger")) {
                	FoundSearchKeyForFixFinger(modelObj);
                    output_disable = true;
                    
                } else if (modelObj.command.equals("FoundSearchKeyForFixAntiFinger")) {
                	FoundSearchKeyForFixAntiFinger(modelObj);
                    output_disable = true;
                    
                }else if (modelObj.command.equals("delete")) {
					Operation.deleteMethod(modelObj,node,fingerTable,dataList);
					
				} else if (modelObj.command.equals("update after delete")) {
					// updating the finger table after neighbour is deleted i.e., when the successor or the the predecssor is deleted
					updateAfterDelete(modelObj);
					output_disable = true;
				}else if(modelObj.command.equals("updateSuccessor")){
					//System.out.println("updateSuccessor : "+modelObj.successor.getId());
					if (modelObj.successor != null) {
						node.setSuccessor(modelObj.successor);
					}
					modelObj.response= true;
				} else if (modelObj.command.equals("out")) {
					Operation.outMethod(modelObj,M,node,fingerTable,antiFingerTable,dataList);
					modelObj.response=true;
					output_disable = true;
				}else if(modelObj.command.equals("in")) {
					if (modelObj.dontUseCache) {
						Operation.inMethod_proto(modelObj, M, node, fingerTable, dataList, modelObj.analysisFlag);
					} else {
						Operation.inMethod(modelObj, M, node, fingerTable,antiFingerTable, dataList,cache,modelObj.analysisFlag);
					}
					//Operation.inMethod(modelObj, M, node, fingerTable,antiFingerTable, dataList,cache,modelObj.analysisFlag);
					modelObj.response=true;
					output_disable = true;
				} else if(modelObj.command.equals("successfully added")) {
					outSuccess(modelObj);
					output_disable = true;
				} else if (modelObj.command.equals("successfully found")) {
					//System.out.println("successfully found"+modelObj.analysisNodeId);
					inSuccess(modelObj);
					output_disable = true;
				}
				else if(modelObj.command.equals("cache")) {
					checkDataList(modelObj,out);
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

	private void checkDataList(MyNetwork modelObj, ObjectOutputStream out) throws IOException {
		// TODO Auto-generated method stub
		if(dataList.contains(modelObj.dataString)) {
			modelObj.dataFound = true;
			out.writeObject(modelObj);
			//System.out.println("The data string " + modelObj.dataString+ " is found on "+ node.getId());
		}
		
		else {
			modelObj.dataFound = false;
			//System.out.println("The data string " + modelObj.dataString+ " is not found");
			out.writeObject(modelObj);
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
				updateAntiFingerTable(modelObj,newNodeKey);
				
				passFingerTableAndDataToNewNode(modelObj,tempPred, newNodeKey); //this will pass both finger and anti finger
							
				MyNetwork obj = new MyNetwork();
				obj.command = "updateSuccessor";
				obj.successor= temp;
				//System.out.println("Key"+tempPred.getId()+"tempPred.getIp() "+tempPred.getIp()+"tempPred.getPortNo()"+tempPred.getPortNo());
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
			
			returnFlag= Operation.sendRequest(ip,port,modelObj);
		}
		
		return returnFlag;
	}

	private void updateAfterDelete(MyNetwork modelObj) {

		//System.out.println("updateAfterDelete :Currsucc "+node.getSuccessor().getId()+" currPred"+node.getPredecessor().getId() +" modelObj.nodeToDeleteId "+modelObj.nodeToDeleteId);
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
		
		for (AntiFinger antifinger : antiFingerTable) {
			int keyStart = antifinger.getKey();
			int keyEnd = antifinger.getSpan();

			if(antifinger.getSuccessor() == modelObj.nodeToDeleteId){
				antifinger.setSuccessorNode(modelObj.successor.getId());
				antifinger.setip(modelObj.successor.getIp());
				antifinger.setPort(modelObj.successor.getPortNo());
			}
		}
		
	}
	
	//need to delete it...just kept it for reference
	public void updateFingerTable_proto(MyNetwork modelObj,int newNodeKey){
		for (Finger finger : fingerTable) {
			int keyStart = node.getId();
			int keyEnd = node.getPredecessor().getId();
			
			if(Operation.checkSpanRange(keyStart,keyEnd,newNodeKey,true,M)){
				finger.setSuccessorNode(newNodeKey);
			}
		}
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
	}

	public void updateAntiFingerTable(MyNetwork modelObj,int newNodeKey){
		int selfId =node.getId();
		//Operation.printAntiFingerTable(antiFingerTable);
		for (AntiFinger antifinger : antiFingerTable) {
			int successorNodeId = antifinger.getSuccessor();
			int temp_newNodeKey = (newNodeKey+1)%((int)Math.pow(2, M));
			boolean rangeCheckFlag = Operation.checkSpanRange1(temp_newNodeKey, selfId, antifinger.getKey(), true, M);

			if (successorNodeId ==selfId && (rangeCheckFlag==false)) {
				antifinger.setSuccessorNode(newNodeKey);
				antifinger.setip(modelObj.addObject.get(1));
				antifinger.setPort(Integer.parseInt(modelObj.addObject.get(2)));
			}
		}
		//Operation.printAntiFingerTable(antiFingerTable);
    }

	public void passFingerTableAndDataToNewNode(MyNetwork modelObj,Node previousPred, int newNodeKey){
	
	
		String ip = modelObj.addObject.get(1);
		int port = Integer.parseInt(modelObj.addObject.get(2)); 
		
		MyNetwork obj = new MyNetwork();
		obj.command = "add_PassFingerTableAndData";
		obj.fingerTable = fingerTable;
		obj.antiFingerTable = antiFingerTable;
		obj.predecessor= previousPred;
		obj.successor=node;
		obj.dataList = passDataToNewNode(modelObj, newNodeKey);
		
		Operation.sendRequest(ip,port,obj);
		
	}
	
	// function to decide which data keys to transfer
	public List<String> passDataToNewNode(MyNetwork modelObj, int newNodeKey) {
		List<String> DataToTransfer = new ArrayList<String>();
		List<String> newDataList = new ArrayList<String>();
		
		int start =(newNodeKey+1)%(totalNodes);
		for(String data : dataList) {
			int hashKey = Operation.getmd5Modulo(data, M);
			if (Operation.checkSpanRange1(start, node.getId(), hashKey, true, M)) {
				//System.out.println("data "+data +" falls in range "+start +"-"+node.getId());
				newDataList.add(data);
			} else {
				DataToTransfer.add(data);
			}
		}
		for (String dt : DataToTransfer) {
			dataList.remove(dt);
		}
		
		return DataToTransfer;
	}

	public void printList(List<String> list){
		for (String string : list) {
			System.out.println(string);
		}
	}
	
	public void updateNewHostFingerTable(MyNetwork modelObj){
		
		//get successor's finger table 
		List<Finger> succFingerTable = modelObj.fingerTable;
		List<AntiFinger> succAntiFingerTable = modelObj.antiFingerTable;

		//update pred
		node.setPredecessor(modelObj.predecessor);

		//update succ
		node.setSuccessor(modelObj.successor);
		
		// transferring the data from the successor when a new node is added
		dataList.addAll(modelObj.dataList);
		//dataList = modelObj.dataList;
		
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
		
		for (AntiFinger finger : antiFingerTable) {
			int tempKey =finger.getKey();
			if (Operation.checkSpanRange1(updateRangeStart,updateRangeEnd,tempKey,true,M)) {	//if key falls between span
				finger.setSuccessorNode(updateRangeEnd);
				finger.setip(node.getSuccessor().getIp());
				finger.setPort(node.getSuccessor().getPortNo());
			}else{	//calculate it from successor's finger table
				for (AntiFinger finger2 : succAntiFingerTable) {
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
			validate = Operation.checkSpanRange1(node.getPredecessor().getId(),node.getId(),keyTobeValidate,true, M);
		}

		//System.out.println("fixFinger_validateRange "+modelObj.keyTobeValidate+ " validate "+validate);

		//if Key range to be validated doesn't fall into local host range then check the finger table
		if(!validate){
			
			//System.out.println(" checkSpan "+Operation.checkSpanRange(node.getId(),node.getSuccessor().getId(),keyTobeValidate,true,M));

			if(Operation.checkSpanRange1(node.getId(),node.getSuccessor().getId(),keyTobeValidate,true,M)){
					//keyTobeValidate > node.getId() && keyTobeValidate <= node.getSuccessor().getId())
				responsibleNode = new Node(node.getSuccessor().getId(),node.getSuccessor().getIp(),node.getSuccessor().getPortNo());
			}
			else{
			
			for(Finger finger : fingerTable){
				boolean check = Operation.checkSpanRange1(finger.getKey(),finger.getSpan(),keyTobeValidate,true,M);
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

		String response_string = null;
		if(responsibleNode != null){
			response_string = responsibleNode.getId()+"/"+responsibleNode.getIp()+"/"+responsibleNode.getPortNo();
		}
		
		//System.out.println("fixFinger_validateRange responsible node found : " +responsibleNode.getId());
		//System.out.println("response_string "+response_string);
        //System.out.println("sendRequest : "+response_string+" keyTobeValidate "+keyTobeValidate);

		return response_string;

	}

	public String fixAntiFinger_validateRange(MyNetwork modelObj){

		Node responsibleNode = null;
		int keyTobeValidate = modelObj.keyTobeValidate;
		boolean validate = false;
		if(node.getPredecessor().getId() == node.getId()){
			validate = true;
		}
		else{
			validate = Operation.checkSpanRange1(node.getPredecessor().getId(),node.getId(),keyTobeValidate,true, M);

		}

		//System.out.println("fixAntiFinger_validateRange "+modelObj.keyTobeValidate+ " validate "+validate);

		//if Key range to be validated doesn't fall into local host range then check the finger table
		if(!validate){
			
			//System.out.println(" checkSpan "+Operation.checkSpanRange1(node.getId(),node.getSuccessor().getId(),keyTobeValidate,true,M));

			if(Operation.checkSpanRange1(node.getId(),node.getSuccessor().getId(),keyTobeValidate,true,M)){
					//keyTobeValidate > node.getId() && keyTobeValidate <= node.getSuccessor().getId())
				responsibleNode = new Node(node.getSuccessor().getId(),node.getSuccessor().getIp(),node.getSuccessor().getPortNo());
			}
			else{
			
			for(AntiFinger finger : antiFingerTable){
				boolean check = Operation.checkSpanRange1(finger.getKey(),finger.getSpan(),keyTobeValidate,true,M);
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

		String response_string = null;
		if(responsibleNode != null){
			response_string = responsibleNode.getId()+"/"+responsibleNode.getIp()+"/"+responsibleNode.getPortNo();
		}
		
		//System.out.println("fixFinger_validateRange responsible node found : " +responsibleNode.getId());
		//System.out.println("response_string "+response_string);
        //System.out.println("sendRequest : "+response_string+" keyTobeValidate "+keyTobeValidate);

		return response_string;

	}
	
	public void searchKeyForFixAntiFinger(MyNetwork modelObj){
		//System.out.println("Inside searchKeyForFixAntiFinger");
		Node responsibleNode = null;
		if(Operation.checkSpanRange1(node.getId(),node.getSuccessor().getId(),modelObj.keyTobeValidate,true,M)){
			responsibleNode = new Node(node.getSuccessor().getId(),node.getSuccessor().getIp(),node.getSuccessor().getPortNo());
			MyNetwork newObj = new MyNetwork();
			newObj.command = "FoundSearchKeyForFixAntiFinger";
			newObj.keyTobeValidate = modelObj.keyTobeValidate;
			newObj.responsibleNode = responsibleNode;
			String modelObjIp = modelObj.sendResponseToNode.getIp();
			int modelObjPort = modelObj.sendResponseToNode.getPortNo();
			Operation.sendMessage(modelObjIp,modelObjPort,newObj);
		}
		else{
			
			MyNetwork newObj = new MyNetwork();
			String succIp = node.getSuccessor().getIp();
			int succPort = node.getSuccessor().getPortNo();
			Operation.sendMessage(succIp,succPort,modelObj);
		}
		
	}
	
	public void searchKeyForFixFinger(MyNetwork modelObj){
		//System.out.println("Inside searchKeyForFixFinger");
		
		Node responsibleNode = null;
		if(Operation.checkSpanRange1(node.getId(),node.getSuccessor().getId(),modelObj.keyTobeValidate,true,M)){
			responsibleNode = new Node(node.getSuccessor().getId(),node.getSuccessor().getIp(),node.getSuccessor().getPortNo());
			MyNetwork newObj = new MyNetwork();
			newObj.command = "FoundSearchKeyForFixFinger";
			newObj.keyTobeValidate = modelObj.keyTobeValidate;
			newObj.responsibleNode = responsibleNode;
			String modelObjIp = modelObj.sendResponseToNode.getIp();
			int modelObjPort = modelObj.sendResponseToNode.getPortNo();
			Operation.sendMessage(modelObjIp,modelObjPort,newObj);
		}
		else{
			
			MyNetwork newObj = new MyNetwork();
			String succIp = node.getSuccessor().getIp();
			int succPort = node.getSuccessor().getPortNo();
			Operation.sendMessage(succIp,succPort,modelObj);
		}
		
	}
	
	public void FoundSearchKeyForFixFinger(MyNetwork modelObj){
		//System.out.println("Inside FoundSearchKeyForFixFinger");
		
		for(Finger finger : fingerTable)
		{
			if(finger.getKey() == modelObj.keyTobeValidate){
				finger.setSuccessorNode(modelObj.responsibleNode.getId());
				finger.setip(modelObj.responsibleNode.getIp());
				finger.setPort(modelObj.responsibleNode.getPortNo());
				break;
			}
			
		}
		
	}
	
	public void FoundSearchKeyForFixAntiFinger(MyNetwork modelObj){
		//System.out.println("Inside FoundSearchKeyForFixAntiFinger");
		
		for(AntiFinger finger : antiFingerTable)
		{
			if(finger.getKey() == modelObj.keyTobeValidate){
				finger.setSuccessorNode(modelObj.responsibleNode.getId());
				finger.setip(modelObj.responsibleNode.getIp());
				finger.setPort(modelObj.responsibleNode.getPortNo());
				break;
			}
			
		}
		
	}

	public void outSuccess(MyNetwork modelObj) {
		System.out.println("The data "+modelObj.dataString +" is successfully added on node "+ modelObj.respondedNodeId+" , Hop count :"+modelObj.hopCount);
		System.out.print("chord>");
		System.out.println("Hop count :"+modelObj.hopCount);
		System.out.println("The data "+modelObj.dataString +" is successfully added on node "+ modelObj.respondedNodeId+" , Hop count :"+modelObj.hopCount);
		System.out.print("chord > ");
	}
	
	public void inSuccess(MyNetwork modelObj) {
		SimpleDateFormat format = new SimpleDateFormat("yy/MM/dd HH:mm:ss");  
		DateFormat df = new SimpleDateFormat("yy/MM/dd HH:mm:ss");
		Calendar calobj = Calendar.getInstance();
		String DateStop = df.format(calobj.getTime());
		long diff = 0;
				
		if(modelObj.date != null){
			try {
				long timeMilli2 = calobj.getTimeInMillis();
				diff = timeMilli2 - modelObj.miliseconds;
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("inSuccess : error in date");
			}   
		}
		
		if (modelObj.analysisFlag) {
			System.out.println("The data key "+modelObj.analysisNodeId +" is successfully found on node "+ modelObj.respondedNodeId+" ,Hop count :"+modelObj.hopCount+" , Traversal List :"+modelObj.traversalList);
			String temp = "data key: "+modelObj.analysisNodeId +" ,found on node: "+ modelObj.respondedNodeId+" ,Hop count :"+modelObj.hopCount+" ,Traversal List :"+modelObj.traversalList;
			//System.out.println("data: "+modelObj.dataString +" hope count "+modelObj.hopCount);
			
			//System.out.println(modelObj.hopCount+" ,"+modelObj.traversalList);
			System.out.println(modelObj.hopCount +" "+diff);
			//System.out.println(temp);
			analysisStore.add("data key: "+modelObj.analysisNodeId +" "+temp);
		} else {
			System.out.println(modelObj.hopCount +" "+diff);
			System.out.println("The data "+modelObj.dataString +" is successfully found on node "+ modelObj.respondedNodeId+" ,Hop count :"+modelObj.hopCount+" , Traversal List :"+modelObj.traversalList);
			//System.out.println("The data "+modelObj.dataString + "Hop count :"+modelObj.hopCount+" , Traversal List :"+modelObj.traversalList);
			//System.out.println("data: "+modelObj.dataString +" hope count "+modelObj.hopCount);
		}
		cache.set(modelObj.dataString, modelObj.respondedNodeIp, modelObj.respondedNodeport, modelObj.respondedNodeId );
		//System.out.print("chord> ");
		//System.out.println("Responded id :" +modelObj.respondedNodeId);
		//System.out.println("Responded id :" +modelObj.respondedNodeId);
		cache.print();
		//System.out.print("chord > ");
	}

}

