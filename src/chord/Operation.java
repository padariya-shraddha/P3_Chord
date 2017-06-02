package chord;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Operation {

	public static void deleteMethod(MyNetwork networkObj, Node node,List<Finger> fingerTable  ){
		int nodeToFind = networkObj.nodeToDeleteId;
		if (node.getId() == nodeToFind) {
			if (node.getId() == node.getSuccessor().getId() && node.getId() == node.getPredecessor().getId()) {
				System.exit(0);
			}
			else {
				networkObj.command = "update after delete";
				// notifying successor the deletion of the current node
				if(node.getId() != node.getSuccessor().getId()) {
					sendMessage(node.getSuccessor().getIp(), node.getSuccessor().getPortNo(), networkObj);
				}
				// notifying predecessor the deletion of the current node
				if(node.getId() != node.getPredecessor().getId()) {
					sendMessage(node.getPredecessor().getIp(), node.getPredecessor().getPortNo(), networkObj);
				}
				// deleting the node
				System.exit(0);
			}
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
						Socket s = new Socket(ip, port);
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
	
	public static void sendMessage(String ip,int portNo, MyNetwork networkObj) {
		try {
			Socket s = new Socket(ip, portNo);
			ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
			out.writeObject(networkObj);
			out.close();
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean checkSpanRange(int start,int end,int searchKey,boolean flag,int M) {
		
		if (start==end) {
			return true;
		}
		
		boolean result = false;
		int keyStart;
		int keyEnd;

		keyEnd = (start<end) ? end : (int) (end + Math.pow(2, M));

		if(flag && (searchKey >= start && searchKey <= end)) {result = true;}
		if(!flag && (searchKey >= start && searchKey < end)) {result = true;}

		return result;
	}

	public static  boolean sendRequest(String ip, int port,MyNetwork modelObj){
		Socket s1=null;
		boolean returnFlag;
		ObjectOutputStream out=null;
		ObjectInputStream in=null;
		try {
			System.out.println("ip :"+ip +", port :"+port);
			s1 = new Socket(ip, port);
			out = new ObjectOutputStream(s1.getOutputStream());
			in = new ObjectInputStream(s1.getInputStream());
			out.writeObject(modelObj);
			MyNetwork response = (MyNetwork) in.readObject();
			returnFlag = response.response;

		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			returnFlag= false;
		}finally{
			try {
				if (in!=null) {
					in.close();
				}
				if (out!=null) {
					out.close();
				}
				if (s1!=null) {
					s1.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return returnFlag;
	}

	public static boolean StrToIntCheck(String str){
		try {
			int i = Integer.parseInt(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
		
	}
	
	public static int getmd5Modulo(String lineNoSpace,int M){
		String s=lineNoSpace;
	    MessageDigest m;
		try {
			m = MessageDigest.getInstance("MD5");
			m.update(s.getBytes(),0,s.length());
			BigInteger bi = new BigInteger(1,m.digest());
			int count = (int) Math.pow(2, M);
			
			if (count>0) {
				BigInteger modulo = new BigInteger(""+count+"");
				bi = bi.remainder(modulo);
				return bi.intValue();
				
			} else {
				return -1;
			}
			
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Error in getmd5Modulo");
			return -1;
		} 
	}
	
	public static void outMethod(MyNetwork networkObj,int M,Node node,List<Finger> fingerTable,List<String> dataList){
		
		if (networkObj != null && (!networkObj.dataString.equals(""))) {
			String line = networkObj.dataString.trim();
			int NodeId = Operation.getmd5Modulo(line,M);
			
			if (NodeId>=0) {
				
				//************ we can also use anti- finger table
				
				String ip ;
				int port;
				
				//check if NodeId is in range of predecessorID and self
				int selfId = node.getId();
				int predecessorID = node.getPredecessor().getId();
				predecessorID = (predecessorID+1)%((int) Math.pow(2, M));
				
				if (checkSpanRange(predecessorID, selfId, NodeId, true, M)) {
					
					//add to self
					dataList.add(networkObj.dataString);
					System.out.println("The data "+ networkObj.dataString +"is added in "+node.getId());
					if(networkObj.requestedNodeId != node.getId()) {
						networkObj.command ="successfully added";
						networkObj.respondedNodeId= node.getId();
						networkObj.respondedNodeIp = node.getIp();
						networkObj.respondedNodeport = node.getPortNo();
						sendMessage(networkObj.requestedNodeIp, networkObj.requestedNodeport, networkObj);
					}
					return;
				}
				
				//check if NodeId resides between self and successor *****
				selfId= (selfId+1)%((int) Math.pow(2, M));
				int successorID = node.getSuccessor().getId();
				
				if (Operation.checkSpanRange(selfId,successorID,NodeId,true,M)) {
					//send request to successor
					ip = node.getSuccessor().getIp();
					port = node.getSuccessor().getPortNo();
					
					sendMessage(ip, port, networkObj);
					
					return;
				}
				
				for (Finger finger : fingerTable) {
					int start = finger.getKey();
					int end = finger.getSpan();
					
					if (Operation.checkSpanRange(start,end,NodeId,false,M)) {
						//send request to the node to add data
						
						ip = finger.getIp();
						port= finger.getPort();
						
						sendMessage(ip, port, networkObj);
						
						return;
					}
				}
			}
		}
	}
	
public static void inMethod(MyNetwork networkObj,int M,Node node,List<Finger> fingerTable,List<String> dataList){
		
		if (networkObj != null && (!networkObj.dataString.equals(""))) {
			String line = networkObj.dataString.trim();
			int NodeId = Operation.getmd5Modulo(line,M);
			
			if (NodeId>=0) {
				
				//************ we can also use anti- finger table
				
				String ip ;
				int port;
				
				//check if NodeId is in range of predecessorID and self
				int selfId = node.getId();
				int predecessorID = node.getPredecessor().getId();
				predecessorID = (predecessorID+1)%((int) Math.pow(2, M));
				
				if (checkSpanRange(predecessorID, selfId, NodeId, true, M)) {
					
					//checking in self
					if (dataList.contains(networkObj.dataString)) {
						System.out.println("Data key" + networkObj.dataString+ " is found in" + node.getId()); 
						if(networkObj.requestedNodeId != node.getId()) {
							networkObj.command ="successfully found";
							networkObj.respondedNodeId= node.getId();
							networkObj.respondedNodeIp = node.getIp();
							networkObj.respondedNodeport = node.getPortNo();
							sendMessage(networkObj.requestedNodeIp, networkObj.requestedNodeport, networkObj);
						}
					}
					return;
				}
				
				//check if NodeId resides between self and successor *****
				selfId= (selfId+1)%((int) Math.pow(2, M));
				int successorID = node.getSuccessor().getId();
				
				if (checkSpanRange(selfId,successorID,NodeId,true,M)) {
					//send request to successor
					ip = node.getSuccessor().getIp();
					port = node.getSuccessor().getPortNo();
					
					sendMessage(ip, port, networkObj);
					
					return;
				}
				
				for (Finger finger : fingerTable) {
					int start = finger.getKey();
					int end = finger.getSpan();
					
					if (checkSpanRange(start,end,NodeId,false,M)) {
						//send request to the node which has the data
						
						ip = finger.getIp();
						port= finger.getPort();
						
						sendMessage(ip, port, networkObj);
						
						return;
					}
				}
			}
		}
	}
	
	

	public static void printFingerTable(List<Finger> fingerTable){
		for (Finger finger : fingerTable) {
			finger.print();
			System.out.println();
		}
	}
	
	public static void printDataTable(List<String> dataList){
		System.out.println("In print dataList");
		for (String data : dataList) {
			System.out.println(data);
		}
	}
}
