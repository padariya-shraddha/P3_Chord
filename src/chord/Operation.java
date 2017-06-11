package chord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Timestamp;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * @author Team-6
 * @description This class contains all the common methods which has been used to perform data operations and
 * communication among all participating nodes in a distributed enviornment
 */

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
				//System.out.println("tempKey "+tempKey+"tempRange"+tempRange+"nodeToFind"+nodeToFind+"result"+checkSpanRange(tempKey,tempRange,nodeToFind,true,M));

				if(checkSpanRange(tempKey,tempRange,nodeToFind,true,M))
				{
					//System.out.println(finger.getIp()+" "+finger.getPort());

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
			//e.printStackTrace();	//commented by shraddha
		}
	}

	public static boolean checkSpanRange(int start,int end,int searchKey,boolean flag,int M) {

		int mytemp = (end+1)%((int)Math.pow(2, M));

		if (mytemp==start) {
			return true;
		}

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

				if(keyEnd >= 64 ){
					if((searchKey >= keyStart && searchKey <=63) || (searchKey >=0 && searchKey <= end ) ){
						result = true;
					}
				}
				else{

					result = false;

				}

			}

		}
		else{
			if(searchKey >= keyStart && searchKey < keyEnd) {
				result = true;
			}
			else{
				if(keyEnd >= 64 ){
					if((searchKey >= keyStart && searchKey <=63) || (searchKey >=0 && searchKey < end ) ){
						result = true;
					}
				}
				else{
					result = false;
				}
			}
		}

		keyEnd = (start<end) ? end : (int) (end + Math.pow(2, M));

		//System.out.println("KeyStart"+keyStart+" KeyEnd " +keyEnd+ "searchKey "+searchKey);
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
					System.out.println("The data '"+ networkObj.dataString +"' is added on "+node.getId());
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
			//System.out.println("NodeId :"+NodeId);
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


				if (checkSpanRange(predecessorID, selfId, NodeId, true, M)) {
					//add to self
					dataList.add(networkObj.dataString);
					System.out.println("The data "+ networkObj.dataString +" is added on "+node.getId());
					if(networkObj.requestedNodeId != node.getId()) {
						networkObj.command ="successfully added";
						networkObj.respondedNodeId= node.getId();
						networkObj.respondedNodeIp = node.getIp();
						networkObj.respondedNodeport = node.getPortNo();
						sendMessage(networkObj.requestedNodeIp, networkObj.requestedNodeport, networkObj);
					}
					//return;
				}else if (Operation.checkSpanRange(tempselfId,successorID,NodeId,true,M)) {
					//send request to successor
					ip = node.getSuccessor().getIp();
					port = node.getSuccessor().getPortNo();

					networkObj.hopCount =networkObj.hopCount+1;

					sendMessage(ip, port, networkObj);
					//return;
				}else{

					/*for (Finger finger : fingerTable) {
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
					}*/

					//find exact opposite node
					int oppoNode= (selfId + (totalNodes/2))%totalNodes;

					boolean clockwise = checkSpanRange(selfId, oppoNode, NodeId, true, M);

					if (clockwise) {
						for (Finger finger : fingerTable) {
							int start = finger.getKey();
							int end = finger.getSpan();

							if (Operation.checkSpanRange(start,end,NodeId,false,M)) {
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

							if (Operation.checkSpanRange(start,end,NodeId,false,M)) {
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

	public static void inMethod(MyNetwork networkObj,int M,Node node,List<Finger> fingerTable,List<AntiFinger> antiFingerTable,List<String> dataList, LRUCache cache,boolean analysisFlag){

		//****************
		//use networkObj.dontUseCache to enable/disable cache
		//dontUseCache = true => disable cache
		//dontUseCache= false => enable cache
		//****************
		if ((networkObj != null && (!networkObj.dataString.equals(""))) ||analysisFlag) {
			boolean catch1 = true;
			String line;
			int NodeId;
			if (analysisFlag) {
				line = "";
				NodeId = networkObj.analysisNodeId;
			} else {
				line = networkObj.dataString.trim();
				NodeId = Operation.getmd5Modulo(line,M);
			}

			int totalNodes = (int) Math.pow(2, M);
			int selfId = node.getId();
			if (NodeId>=0) {

				String ip ;
				int port;

				networkObj.traversalList.add(""+selfId);

				//check if NodeId is in range of predecessorID and self
				int predecessorID = node.getPredecessor().getId();
				predecessorID = (predecessorID+1)%((int) Math.pow(2, M));

				//check if NodeId resides between self and successor
				int temp = (selfId+1)%((int) Math.pow(2, M));
				int successorID = node.getSuccessor().getId();
				NodeInfo nodeInfo;



				//if (node.getId() == networkObj.requestedNodeId) {
				if ( (nodeInfo = cache.get(networkObj.dataString)) != null && analysisFlag == false) {

					// contact node Indo
					cache.print();
					//System.out.println(" The data string " +networkObj.dataString + " is found in cache and it's present on node id" +nodeInfo.nodeId);
					try {

						networkObj.command = "cache";
						Socket s = new Socket(nodeInfo.ip, nodeInfo.port);
						ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
						out.writeObject(networkObj);

						ObjectInputStream in = new ObjectInputStream(s.getInputStream());
						MyNetwork response = (MyNetwork) in.readObject();
						out.close();
						s.close();

						if(response.dataFound) {
							//System.out.println("The data" +networkObj.dataString+" is found");

							networkObj.command ="successfully found";

							networkObj.respondedNodeId= node.getId();
							networkObj.respondedNodeIp = node.getIp();
							networkObj.respondedNodeport = node.getPortNo();
							
							//System.out.println("node.getId() :"+ node.getId());
							//System.out.println("networkObj.requestedNodeI :"+networkObj.requestedNodeId);
							
							if(node.getId() != nodeInfo.nodeId) {
								//System.out.println("in if");
								networkObj.hopCount =networkObj.hopCount+1;
								sendMessage(networkObj.requestedNodeIp, networkObj.requestedNodeport, networkObj);
							}
							else {
								//System.out.println("in else");
								networkObj.hopCount = 0;
								sendMessage(networkObj.requestedNodeIp, networkObj.requestedNodeport, networkObj);
							}
							return;
						}
						else {
							cache.print();
							cache.remove(nodeInfo,0);
							cache.print();
							networkObj.command = "in";
							//System.out.println("The data must have been transferrred during add or delete operation");
							//System.out.println("using normal chord lookup to find data");
						}

					} catch (IOException | ClassNotFoundException e) {
						cache.print();
						cache.remove(nodeInfo,0);
						// resetting command
						cache.print();
						networkObj.command = "in";
						//System.out.println("Node deleted. Using normal chord lookup to find data");
						//System.out.println("using normal chord lookup to find data");
					} 				
				}
				//}
				if (checkSpanRange(predecessorID, selfId, NodeId, true, M)) {
					//checking in self
					if (dataList.contains(networkObj.dataString) || analysisFlag) {
						//System.out.println("Data key" + networkObj.dataString+ " is found in" + node.getId()); 
						//if(networkObj.requestedNodeId != node.getId()) {
						networkObj.command ="successfully found";
						networkObj.respondedNodeId= node.getId();
						networkObj.respondedNodeIp = node.getIp();
						networkObj.respondedNodeport = node.getPortNo();
						sendMessage(networkObj.requestedNodeIp, networkObj.requestedNodeport, networkObj);
						//}
					}
					return;
				}
				else if (checkSpanRange(temp,successorID,NodeId,true,M)) {
					//send request to successor
					//System.out.println("in successor for node "+networkObj.analysisNodeId);
					ip = node.getSuccessor().getIp();
					port = node.getSuccessor().getPortNo();
					networkObj.hopCount =networkObj.hopCount+1;
					sendMessage(ip, port, networkObj);
					return;
				}
				else{

					/*for (Finger finger : fingerTable) {
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
					}*/

					//find exact opposite node
					int oppoNode= (selfId + (totalNodes/2))%totalNodes;
					boolean clockwise = checkSpanRange(selfId, oppoNode, NodeId, true, M);

					if (clockwise) {
						for (Finger finger : fingerTable) {
							int start = finger.getKey();
							int end = finger.getSpan();

							if (checkSpanRange(start,end,NodeId,false,M)) {
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

							if (Operation.checkSpanRange(start,end,NodeId,false,M)) {
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

	public static void inMethod_proto(MyNetwork networkObj,int M,Node node,List<Finger> fingerTable,List<String> dataList,boolean analysisFlag){

		if (networkObj != null && (!networkObj.dataString.equals("")) ||analysisFlag) {
			String line;
			int NodeId;

			if (analysisFlag) {
				line = "";
				NodeId = networkObj.analysisNodeId;
			} else {
				line = networkObj.dataString.trim();
				NodeId = Operation.getmd5Modulo(line,M);
			}

			int totalNodes = (int) Math.pow(2, M);
			int selfId = node.getId();
			if (NodeId>=0) {

				String ip ;
				int port;

				networkObj.traversalList.add(""+selfId);

				//check if NodeId is in range of predecessorID and self
				int predecessorID = node.getPredecessor().getId();
				predecessorID = (predecessorID+1)%((int) Math.pow(2, M));

				//check if NodeId resides between self and successor
				int temp = (selfId+1)%((int) Math.pow(2, M));
				int successorID = node.getSuccessor().getId();

				if (checkSpanRange(predecessorID, selfId, NodeId, true, M)) {
					//System.out.println("in self for :"+NodeId);
					//checking in self
					if (dataList.contains(networkObj.dataString) || analysisFlag) {

						networkObj.command ="successfully found";
						networkObj.respondedNodeId= node.getId();
						networkObj.respondedNodeIp = node.getIp();
						networkObj.respondedNodeport = node.getPortNo();
						sendMessage(networkObj.requestedNodeIp, networkObj.requestedNodeport, networkObj);

					}
					return;
				}
				else if (checkSpanRange(temp,successorID,NodeId,true,M)) {
					//send request to successor
					ip = node.getSuccessor().getIp();
					port = node.getSuccessor().getPortNo();
					networkObj.hopCount =networkObj.hopCount+1;
					sendMessage(ip, port, networkObj);
					return;
				}
				else{

					for (Finger finger : fingerTable) {
						int start = finger.getKey();
						int end = finger.getSpan();

						if (checkSpanRange(start,end,NodeId,false,M)) {
							//send request to the node which has the data
							ip = finger.getIp();
							port= finger.getPort();
							networkObj.hopCount =networkObj.hopCount+1;
							sendMessage(ip, port, networkObj);
							return;	//break
						}
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
		System.out.println("Data :");
		for (String data : dataList) {
			System.out.println(data + "Key "+getmd5Modulo(data, M));
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

	public static String createLogFileFinger(int hostName) {
		try {
			File f = new File("/tmp/chord/" + hostName);
			String flag = null;
			String flag1 = null;
			if (f.mkdirs()) {
				flag = "Success";
			} else {
				flag = "notsuccess";
			}

			if (flag == "Success") {
				File file = new File(f.getAbsolutePath() + "/Finger_host_"+hostName+".txt");
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

	public static String createLogFileAntiFinger(int hostName) {
		try {
			File f = new File("/tmp/chord/" + hostName);
			String flag = null;
			String flag1 = null;
			if (f.mkdirs()) {
				flag = "Success";
			}

			File file = new File(f.getAbsolutePath() + "/AntiFinger_host_"+hostName+".txt");
			if (file.createNewFile()) {
				flag1 = "Success";
			} else {
				flag1 = "notsuccess";
			}
			return file.getAbsolutePath();

		} catch (Exception e) {
			System.out.println("createDirectory : failed");
		}
		return "NotSuccess";
	}

	public static void writeInLogFilesFinger(List<Finger> fingerTable, String filePath) {
		try {
			//Date date = new Date();
			DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
			Calendar calobj = Calendar.getInstance();
			//System.out.println(df.format(calobj.getTime()));

			String data = null;
			FileWriter writer = new FileWriter(filePath, true);
			writer.write("---------------------------"+df.format(calobj.getTime())+"----------------------------------------------");
			writer.write("\r\n");
			//writer.write(df.format(calobj.getTime()));
			//writer.write("\r\n");
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

	public static void writeInLogFilesAntiFinger(List<AntiFinger> fingerTable, String filePath) {
		try {

			DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
			Calendar calobj = Calendar.getInstance();
			//System.out.println(df.format(calobj.getTime()));

			String data = null;
			FileWriter writer = new FileWriter(filePath, true);
			writer.write("---------------------------"+df.format(calobj.getTime())+"----------------------------------------------");
			writer.write("\r\n");
			//writer.write(df.format(calobj.getTime()));
			//writer.write("\r\n");
			for (AntiFinger finger : fingerTable) {
				data = finger.getKey()+" "+finger.getSpan()+" "+finger.getSuccessor();
				writer.write(data);
				writer.write("\r\n");			
			}

			writer.close();
		} catch (IOException e) {
			System.out.println("log file : failed");
		}
	}

	public static void printAnalysis(MyNetwork networkObj,int M,Node node,List<Finger> fingerTable,List<AntiFinger> antiFingerTable,List<String> dataList,LRUCache cache,boolean enhanced){
		int totalNodes= (int) Math.pow(2, M);
		DateFormat df = new SimpleDateFormat("yy/MM/dd HH:mm:ss");
		Calendar calobj = Calendar.getInstance();

		//String date = df.format(calobj.getTime());

		for (int i = 0; i < totalNodes; i++) {
			MyNetwork temp = new MyNetwork();

			temp.traversalList = new ArrayList<>();
			temp.requestedNodeId= node.getId();
			temp.requestedNodeIp= node.getIp();
			temp.requestedNodeport = node.getPortNo();
			temp.analysisNodeId= i;
			temp.dataString= "";
			temp.command= "in";
			temp.analysisFlag = true;
			temp.date = df.format(calobj.getTime());
			temp.miliseconds = calobj.getTimeInMillis();

			if (enhanced) {

				inMethod(temp, M, node, fingerTable, antiFingerTable, dataList, cache,true);
			} else {
				temp.dontUseCache =true;
				inMethod_proto(temp, M, node, fingerTable, dataList, true);
			}

		}
	}

	/**
	 * @param fileID
	 * @param networkObj
	 * @param M
	 * @param node
	 * @param fingerTable
	 * @param antiFingerTable
	 * @param dataList
	 * @param cache
	 * @param dontUseCache (set this para 'true' if you don't want to use cache else set it 'false')
	 */
	public static void readWordsFromFile(String fileID,MyNetwork networkObj,int M,Node node,List<Finger> fingerTable,List<AntiFinger> antiFingerTable,List<String> dataList,LRUCache cache,boolean dontUseCache){
		try{
			FileReader fr = new FileReader (fileID+".txt");        
			BufferedReader br = new BufferedReader (fr);   
			DateFormat df = new SimpleDateFormat("yy/MM/dd HH:mm:ss");
			Calendar calobj = Calendar.getInstance();
			String line = br.readLine();
			int count = 0;
			while (line != null) {

				String []parts = line.split(" ");
				for( String word : parts)
				{	int dataKey = getmd5Modulo(word,M); 
				MyNetwork temp = new MyNetwork();
				temp.traversalList = new ArrayList<>();
				temp.requestedNodeId= node.getId();
				temp.requestedNodeIp= node.getIp();
				temp.requestedNodeport = node.getPortNo();
				temp.analysisNodeId= dataKey;
				temp.dataString= word;
				temp.command= "in";
				temp.analysisFlag = true;
				temp.dontUseCache = dontUseCache;
				temp.date = df.format(calobj.getTime());
				temp.miliseconds = calobj.getTimeInMillis();

				if (dontUseCache) {
					inMethod_proto(temp, M, node, fingerTable, dataList, false);
				} else {
					inMethod(temp, M, node, fingerTable, antiFingerTable, dataList, cache,false);
				}
				}
				line = br.readLine();
			}         
		}catch (Exception e) {
			System.out.println("readWordsFromFile : error");
		}
	}

	public static void storeWordsFromFile(MyNetwork networkObj,int M,Node node,List<Finger> fingerTable,List<AntiFinger> antiFingerTable,List<String> dataList,LRUCache cache){

		try{
			FileReader fr = new FileReader ("WordFile.txt"); 
			BufferedReader br = new BufferedReader (fr);     
			String line = br.readLine();
			List<String> wordList = new ArrayList<String>();

			while (line != null) {
				String []parts = line.split(" ");
				for( String word : parts){
					if(!wordList.contains(word)){
						wordList.add(word);
					} 
				}
				line = br.readLine();
			}  
			//System.out.println(wordList.size());
			for( String word : wordList)
			{
				int dataKey = getmd5Modulo(word,M); 
				MyNetwork temp = new MyNetwork();
				temp.traversalList = new ArrayList<>();
				temp.requestedNodeId= node.getId();
				temp.requestedNodeIp= node.getIp();
				temp.requestedNodeport = node.getPortNo();
				temp.analysisNodeId= dataKey;
				temp.dataString= word;
				temp.command= "out";
				temp.analysisFlag = true;
				outMethod(temp,M,node,fingerTable,antiFingerTable,dataList);
			}
		}catch (Exception e) {
			System.out.println("readWordsFromFile : error");
		}

		System.out.println("chord>");
	}


}
