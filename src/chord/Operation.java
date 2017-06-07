package chord;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Operation {

	private static Logger logger = Logger.getLogger(Operation.class.getSimpleName());  
	private static String filename = "/tmp/log.log";
	private static FileHandler fh ; 
	private static int M = 6;
   




	public static void deleteMethod(MyNetwork networkObj, Node node,List<Finger> fingerTable,List<String> dataList  ){
		int nodeToFind = networkObj.nodeToDeleteId;
		if (node.getId() == nodeToFind) {
			 
			
			if (node.getId() == node.getSuccessor().getId() && node.getId() == node.getPredecessor().getId()) {
				System.exit(0);
			}
			else {
				networkObj.command = "update after delete";
				Node pred = new Node(node.getPredecessor().getId(),node.getPredecessor().getIp(),node.getPredecessor().getPortNo());
				Node succ = new Node(node.getSuccessor().getId(),node.getSuccessor().getIp(),node.getSuccessor().getPortNo());
				
				networkObj.predecessor = pred;
				networkObj.successor = succ;
				
				// notifying successor the deletion of the current node
				if(node.getId() != node.getSuccessor().getId()) {
					// for transferring of data when deleting to successor node
					networkObj.dataList = dataList;
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

				// checkSpanRange
				//if (nodeToFind >= tempKey || nodeToFind < tempRange) {
				System.out.println("tempKey "+tempKey+"tempRange"+tempRange+"nodeToFind"+nodeToFind+"result"+checkSpanRange(tempKey,tempRange,nodeToFind,true,M));

				if(checkSpanRange(tempKey,tempRange,nodeToFind,true,M))
				{
					System.out.println(finger.getIp()+" "+finger.getPort());

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
						System.out.println("Error : deleteMethod");
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
		int keyStart = -1;
		int keyEnd = -1;
		if(start>end){
			keyStart = start;
			keyEnd = (int) (end + Math.pow(2, M));
		}
		else{
			keyStart = start;
			keyEnd = end;
		}
		
		if(flag)
			{
			 if(searchKey >= keyStart && searchKey <= keyEnd) {
				 result = true;
			 }
			 else{
					result = false;
				}
			
			}
		else{
			
			if(searchKey >= keyStart && searchKey < keyEnd) {
				 result = true;
			 }
			 else{
					result = false;
				}
		}
		
		keyEnd = (start<end) ? end : (int) (end + Math.pow(2, M));

		System.out.println("KeyStart"+keyStart+" KeyEnd " +keyEnd+ "searchKey "+searchKey);
		if(flag && (searchKey >= start && searchKey <= end)) {result = true;}
		if(!flag && (searchKey >= start && searchKey < end)) {result = true;}

		return result;
	}

	public static boolean checkSpanRange1(int start,int end,int searchKey,boolean flag,int M) {

		if (start==end) {
			return true;
		}

		int finalEndNode = ((int) Math.pow(2, M))-1;

		if (start>end) {
			boolean b1 = checkSpanRange1(start,finalEndNode,searchKey,flag,M);
			boolean b2 = checkSpanRange1(0,end,searchKey,flag,M);

			if (b1 || b2) {
				return true;
			}
			else 
				return false;
		}
		else{
			if (searchKey >= start && searchKey <= end) {
				return true;
			} else {
				return false;
			}
		}
	}

	public static  boolean sendRequest(String ip, int port,MyNetwork modelObj){
	
		Socket s1=null;
		boolean returnFlag;
		ObjectOutputStream out=null;
		ObjectInputStream in=null;
		try {

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
			/*try {
				
				if (in!=null) {
					in.close();
				}
				if (out!=null) {
					out.close();
				}
				if (s1!=null) {
					s1.close();
				
			} catch (IOException e) {
				e.printStackTrace();
			}}*/
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

	public static void outMethod_proto(MyNetwork networkObj,int M,Node node,List<Finger> fingerTable,List<String> dataList){

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
				
				//check if NodeId resides between self and successor *****
				selfId= (selfId+1)%((int) Math.pow(2, M));
				int successorID = node.getSuccessor().getId();


				if (checkSpanRange1(predecessorID, selfId, NodeId, true, M)) {

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
					//return;
				}
				else if (Operation.checkSpanRange1(selfId,successorID,NodeId,true,M)) {
					//send request to successor
					ip = node.getSuccessor().getIp();
					port = node.getSuccessor().getPortNo();

					sendMessage(ip, port, networkObj);

					//return;
				}

				else {
					for (Finger finger : fingerTable) {
					int start = finger.getKey();
					int end = finger.getSpan();

					if (Operation.checkSpanRange1(start,end,NodeId,false,M)) {
						//send request to the node to add data

						ip = finger.getIp();
						port= finger.getPort();

						sendMessage(ip, port, networkObj);

						//return;
					}
				}
				}
			}
		}
	}

	public static void outMethod(MyNetwork networkObj,int M,Node node,List<Finger> fingerTable,List<AntiFinger> antiFingerTable,List<String> dataList){

		if (networkObj != null && (!networkObj.dataString.equals(""))) {
			String line = networkObj.dataString.trim();
			int NodeId = Operation.getmd5Modulo(line,M);
			int totalNodes = (int) Math.pow(2, M);
			int selfId = node.getId();
			if (NodeId>=0) {

				String ip ;
				int port;
				
				//check if NodeId is in range of predecessorID and self
				int predecessorID = node.getPredecessor().getId();
				predecessorID = (predecessorID+1)%totalNodes;
				
				//check if NodeId resides between self and successor
				int tempselfId= (selfId+1)%totalNodes;
				int successorID = node.getSuccessor().getId();
				
				if (checkSpanRange1(predecessorID, selfId, NodeId, true, M)) {
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
					//return;
				}else if (Operation.checkSpanRange1(tempselfId,successorID,NodeId,true,M)) {
					//send request to successor
					ip = node.getSuccessor().getIp();
					port = node.getSuccessor().getPortNo();

					networkObj.hopCount =networkObj.hopCount+1;
					
					sendMessage(ip, port, networkObj);
					//return;
				}else{
					//find exact opposite node
					int oppoNode= (selfId + (totalNodes/2))%totalNodes;
					boolean clockwise = checkSpanRange1(selfId, oppoNode, NodeId, true, M);
					
					if (clockwise) {
						for (Finger finger : fingerTable) {
							int start = finger.getKey();
							int end = finger.getSpan();

							if (Operation.checkSpanRange1(start,end,NodeId,false,M)) {
								//send request to the node to add data
								ip = finger.getIp();
								port= finger.getPort();
								networkObj.hopCount =networkObj.hopCount+1;
								sendMessage(ip, port, networkObj);
								return;	//break
							}
						}
					} else {
						//explore anti-clock wise
						for (AntiFinger antiFinger : antiFingerTable) {
							int start = antiFinger.getKey();
							int end = antiFinger.getSpan();

							if (Operation.checkSpanRange1(start,end,NodeId,false,M)) {
								//send request to the node to add data

								ip = antiFinger.getIp();
								port= antiFinger.getPort();
								networkObj.hopCount =networkObj.hopCount+1;
								sendMessage(ip, port, networkObj);
								return;	//break
							}
						}
					}
				}
			}
		}
	}
	
	public static void inMethod(MyNetwork networkObj,int M,Node node,List<Finger> fingerTable,List<AntiFinger> antiFingerTable,List<String> dataList){

		if (networkObj != null && (!networkObj.dataString.equals(""))) {
			String line = networkObj.dataString.trim();
			int NodeId = Operation.getmd5Modulo(line,M);
			int totalNodes = (int) Math.pow(2, M);
			int selfId = node.getId();
			if (NodeId>=0) {

				String ip ;
				int port;

				//check if NodeId is in range of predecessorID and self
				int predecessorID = node.getPredecessor().getId();
				predecessorID = (predecessorID+1)%((int) Math.pow(2, M));

				//check if NodeId resides between self and successor
				selfId= (selfId+1)%((int) Math.pow(2, M));
				int successorID = node.getSuccessor().getId();
				
				if (checkSpanRange1(predecessorID, selfId, NodeId, true, M)) {
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
				else if (checkSpanRange1(selfId,successorID,NodeId,true,M)) {
					//send request to successor
					ip = node.getSuccessor().getIp();
					port = node.getSuccessor().getPortNo();
					networkObj.hopCount =networkObj.hopCount+1;
					sendMessage(ip, port, networkObj);
					return;
				}
				else{
					//find exact opposite node
					int oppoNode= (selfId + (totalNodes/2))%totalNodes;
					boolean clockwise = checkSpanRange1(selfId, oppoNode, NodeId, true, M);
					
					if (clockwise) {
						for (Finger finger : fingerTable) {
							int start = finger.getKey();
							int end = finger.getSpan();

							if (checkSpanRange1(start,end,NodeId,false,M)) {
								//send request to the node which has the data
								ip = finger.getIp();
								port= finger.getPort();
								networkObj.hopCount =networkObj.hopCount+1;
								sendMessage(ip, port, networkObj);
								return;	//break
							}
						}
					} else {
						//explore anti-clock wise
						for (AntiFinger antiFinger : antiFingerTable) {
							int start = antiFinger.getKey();
							int end = antiFinger.getSpan();

							if (Operation.checkSpanRange1(start,end,NodeId,false,M)) {
								//send request to the node to add data

								ip = antiFinger.getIp();
								port= antiFinger.getPort();
								networkObj.hopCount =networkObj.hopCount+1;
								sendMessage(ip, port, networkObj);
								return;	//break
							}
						}
					}
				}
			}
		}
	}

	public static void inMethod_proto(MyNetwork networkObj,int M,Node node,List<Finger> fingerTable,List<String> dataList){

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

				if (checkSpanRange1(predecessorID, selfId, NodeId, true, M)) {

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

				if (checkSpanRange1(selfId,successorID,NodeId,true,M)) {
					//send request to successor
					ip = node.getSuccessor().getIp();
					port = node.getSuccessor().getPortNo();

					sendMessage(ip, port, networkObj);

					return;
				}

				for (Finger finger : fingerTable) {
					int start = finger.getKey();
					int end = finger.getSpan();

					if (checkSpanRange1(start,end,NodeId,false,M)) {
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

	public static void printAntiFingerTable(List<AntiFinger> fingerTable){
		for (AntiFinger antiFinger : fingerTable) {
			antiFinger.print();
			System.out.println();
		}
	}
	
	public static void printDataTable(List<String> dataList){
		System.out.println("In print dataList");
		for (String data : dataList) {
			System.out.println(data);
		}
	}
	
	//need to delete
	public static void printDataInLogFile(List<Finger> fingerTable) {  

		try {  
			//File fh = new File(filename);
			fh = new FileHandler(filename);  
			logger.addHandler(fh);
			for (Finger finger : fingerTable) {
				String data = finger.getKey()+" "+finger.getSpan()+" "+finger.getSuccessor();
				//logger.info(data); 
			}



	    } catch (Exception e) {  
	        e.printStackTrace();  
	    }  
	}
	    
	public static void nodeDeatil(Node node){
		if (node != null) {
			System.out.println("Node id: "+node.getId() + ", ip: "+node.getIp()+", port: "+node.getPortNo());
			Node tempSuccessor = node.getSuccessor();
			Node tempPredecessor = node.getPredecessor();
			
			if (tempSuccessor != null) {
				System.out.println("Node Successor:"+tempSuccessor.getId()+", ip: "+tempSuccessor.getIp()+", port: "+tempSuccessor.getPortNo());
			}
			
			if (tempPredecessor != null) {
				System.out.println("Node Predecessor:"+tempPredecessor.getId() + ", ip: "+tempPredecessor.getIp()+", port: "+tempPredecessor.getPortNo());
			}
		}
	}
	
	public static String createLogFile(int hostName) {
        try {
            File f = new File("/tmp/chord/" + hostName);
            String flag = null;
            String flag1 = null;
            if (f.mkdirs()) {
                System.out.println("nets file successfully created");
                flag = "Success";
            } else {
                flag = "notsuccess";
            }

            if (flag == "Success") {
                File file = new File(f.getAbsolutePath() + "/host_"+hostName+".txt");
                if (file.createNewFile()) {
                    flag1 = "Success";
                } else {
                    flag1 = "notsuccess";
                }
                return file.getAbsolutePath();
            }
        } catch (Exception e) {
            System.out.println("createDirectory : failed");
        }
        return "NotSuccess";
    }
	
	public static void writeInLogFiles(List<Finger> fingerTable, String filePath) {
        try {
        	
            String data = null;
        	FileWriter writer = new FileWriter(filePath, true);
        	writer.write("-------------------------------------------------------------------------");
        	writer.write("\r\n");
        	for (Finger finger : fingerTable) {
				data = finger.getKey()+" "+finger.getSpan()+" "+finger.getSuccessor();
				writer.write(data);
	            writer.write("\r\n");			
	        }
        			
            writer.close();
        } catch (IOException e) {
            System.out.println("log file : failed");
        }
    }

}
