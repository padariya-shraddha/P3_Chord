package chord;
//ghj
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;


public class MyClient extends Thread{

	List<Finger> fingerTable;
	private Node node;
	public int M;
	
	public MyClient(List<Finger> fingerTable,Node node,int M){
		//it will have finger table, successor, predecessor as arguments
		this.fingerTable = fingerTable;
		this.node = node;
		this.M = M;
	}

	public void run(){
		try {
			BufferedReader br= new BufferedReader(new InputStreamReader(System.in));

			System.out.print("chord > ");
			String line = br.readLine();
			line = line.trim();

			while(!line.equals("quit")){
				
				//parse user command
				MyNetwork networkObj = getCommand(line);
				
				if (networkObj != null) {
					String command = networkObj.command;
					
					if (command.equals("add")) {
						//find immediate successor
						addMethod(networkObj);
					} else if (command.equals("delete")) {

						Operation.deleteMethod(networkObj, node, fingerTable);

					} else if(command.equals("out")) {
						outMethod(networkObj);
					} 
					else{
						System.out.println("Please enter valid command");
					}
				
					line = br.readLine();
				}
			}
		} catch (Exception e) {
		}
	}
	
	public MyNetwork getCommand(String line){

		// parsing code
		MyNetwork obj = null;
		
		if (line.contains("add") && line.length()>3) {
			line = line.substring(3,line.length()-1);
			String[] parts = line.split(",");
			if (parts.length==3) {
				obj = new MyNetwork();
				obj.command = "add";
				
				List<String> temp = new ArrayList<>();
				temp.add(parts[0].trim()); //hostKey
				temp.add(parts[1].trim()); //host IP
				temp.add(parts[1].trim()); //host Port number
				
				obj.addObject =temp;
			}
		}
		else if (line.contains("delete") && line.length() > 6) {
			String[] parts = line.split(" ");
				obj = new MyNetwork();
				obj.command = "delete";
				if (Operation.StrToIntCheck(parts[1])) {
					obj.nodeToDeleteId = Integer.parseInt(parts[1]);
				}
		}
		else if (line.contains("out") && line.length() > 3) {
			String[] parts = line.split(" ");
			
				if (parts.length==2) {
					obj = new MyNetwork();
					obj.command = "out";
					obj.dataString = parts[1];
				}
		}
		
		return obj;
	}
	
	public void addMethod(MyNetwork networkObj)
	{
		String keytoFind = networkObj.addObject.get(0);
		
		if (Operation.StrToIntCheck(keytoFind)) {
			int keytoFind_int= Integer.parseInt(keytoFind);
			for (Finger finger : fingerTable) {
				int tempKey = finger.getKey();
				int tempRange = finger.getSpan();
				
				//We have two scenerios here : (1)we get the current node (2) other node
				if (keytoFind_int>=tempKey || keytoFind_int<tempRange) {
					//send request to this node
					String ip = finger.getIp();
					int port = finger.getPort();
					
					Socket s;
					try {
						s = new Socket(ip, port);
						ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
						ObjectInputStream in = new ObjectInputStream(s.getInputStream());
						out.writeObject(networkObj);
						MyNetwork response = (MyNetwork) in.readObject();
						System.out.println("added node "+ keytoFind_int +" in ring");
						in.close();
						out.close();
						s.close();
						
						break;
					} catch (IOException | ClassNotFoundException e) {
						
					} 
				}
			}
		}
	}

	public void outMethod(MyNetwork networkObj){
		
		if (networkObj != null && (!networkObj.dataString.equals(""))) {
			String line = networkObj.dataString.trim();
			int NodeId = Operation.getmd5Modulo(line,M);
			
			if (NodeId>=0) {
				
				//************ we can also use anti- finger table
				
				
				String ip ;
				int port;
				
				//check if NodeId is same as self
				int selfId = node.getId();
				if (selfId == NodeId) {
					//send request to self
					ip= node.getIp(); 
					port= node.getPortNo();
					
					Operation.sendRequest(ip, port, networkObj);
					
					return;
				}
				
				//check if NodeId resides between self and successor *****
				selfId= (selfId+1)%((int) Math.pow(2, M));
				int successorID = node.getSuccessor().getId();
				
				if (Operation.checkSpanRange(selfId,successorID,NodeId,true,M)) {
					//send request to successor
					ip = node.getSuccessor().getIp();
					port = node.getSuccessor().getPortNo();
					
					Operation.sendRequest(ip, port, networkObj);
					
					return;
				}
				
				for (Finger finger : fingerTable) {
					int start = finger.getKey();
					int end = finger.getSpan();
					
					if (Operation.checkSpanRange(start,end,NodeId,false,M)) {
						//send request to found node to add data
						
						ip = finger.getIp();
						port= finger.getPort();
						
						Operation.sendRequest(ip, port, networkObj);
						
						return;
					}
				}
			}
		}
	}
	
}
