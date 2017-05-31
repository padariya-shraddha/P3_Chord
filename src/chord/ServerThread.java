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

	public ServerThread(Socket s,int portNumber,int hostKey,String ipAddr,Node node,Finger finger,Node successorNode,Node predecessorNode,List<Finger> fingerTable){
		this.s = s;
		this.portNumber = portNumber;
		this.hostKey = hostKey;
		this.ipAddr = ipAddr;
		this.node= node;
		this.finger = finger;
		this.predecessorNode = predecessorNode;
		this.successorNode = successorNode;
		this.fingerTable = fingerTable;
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
				if (modelObj.command.equals("add")) {
					modelObj.response = addNodeToChord(modelObj);
				} else if (modelObj.command.equals("transferFingerTable")) {
					updateNewHostFingerTable(modelObj);
				} else if (modelObj.command.equals("delete")) {
					deleteMethod(modelObj);
				}
				else if (modelObj.command.equals("update finger table after neighbour is deleted")) {
					// updating the finger table after neighbour is deleted i.e., when the successor or the the predecssor is deleted
					updateAfterDelete(modelObj);
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

	
	private void deleteMethod(MyNetwork networkObj) {
		int nodeToFind = networkObj.nodeToDeleteId;
		if (node.getId() == nodeToFind) {
			networkObj.command = "update finger table after neighbour is deleted";
			// notifying successor the deletion of the current node
			sendMessage(node.getSuccessor().getIp(), node.getSuccessor().getPortNo(), networkObj);
			// notifying predecessor the deletion of the current node
			sendMessage(node.getPredecessor().getIp(), node.getPredecessor().getPortNo(), networkObj);
			// deleting the node
			System.exit(0);
		}
		else {
			for (Finger finger : fingerTable) {
				int tempKey = finger.getKey();
				int tempRange = finger.getSpan();

				// 
				if (nodeToFind >= tempKey || nodeToFind < tempRange) {
					//send request to this node
					String ip = finger.getIp();
					int port = finger.getPort();
					try {
						s = new Socket(ip, port);
						ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
						out.writeObject(networkObj);
						out.close();
						s.close();
						break;
					} catch (IOException e) {

					} 
				}
			}
		}
	}
	private void sendMessage(String ip,int portNo, MyNetwork networkObj) {
		try {
			s = new Socket(ip, portNo);
			ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
			out.writeObject(networkObj);
			out.close();
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public boolean addNodeToChord(MyNetwork modelObj){
		boolean returnFlag = true;
		int newNodeKey = Integer.parseInt(modelObj.addObject.get(0));
		//check key to add is in self range
		int currentNodeKey = node.getId();
		int currentNodeScrKey = node.getSuccessor().getId();
		int currentNodePredKey = node.getPredecessor().getId();

		//if we have only 1 node in system then new node would become both successor and predecessor of current node
		//*******************We need to make this part generic instead of having if else
		if((currentNodeKey == currentNodeScrKey) && (currentNodeKey == currentNodePredKey)){
			node.getPredecessor().setId(newNodeKey);
			node.getSuccessor().setId(newNodeKey);
			updateFingerTable(modelObj,newNodeKey);
			passFingerTableToNewNode(modelObj);
			//passDataToNewNode();  //To-DO

		}
		else if (newNodeKey <= currentNodeKey && newNodeKey > currentNodePredKey) {

			try{
				node.getPredecessor().setId(newNodeKey);
				updateFingerTable(modelObj,newNodeKey);
				//updateAntiFingerTable(modelObj,newNodeKey);
			}
			catch(Exception e){
				returnFlag = false;
			}

			returnFlag = true;

		}
		//*******************
		else{	//else pass it to next Successor;
			String ip = node.getSuccessor().getIp();
			int port = node.getSuccessor().getPortNo();
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
	
	private void updateAfterDelete(MyNetwork modelObj) {
		// TODO Auto-generated method stub
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

	public void passFingerTableToNewNode(MyNetwork modelObj){
		String ip = modelObj.addObject.get(1);
		int port = Integer.parseInt(modelObj.addObject.get(2)); 
		Socket s1;
		try {
			s1 = new Socket(ip, port);
			ObjectOutputStream out = new ObjectOutputStream(s1.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(s1.getInputStream());
			MyNetwork obj = new MyNetwork();
			obj.command = "transferFingerTable";
			obj.fingerTable = fingerTable;
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

	}

	/*public void passAntiFingerTableToNewNode(int newNodeKey){

    }*/


}

