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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @author Team-6
 * @description This Class has been used to continuously read the commands given by users
 */

public class MyClient extends Thread{

	List<Finger> fingerTable;
	private Node node;
	public int M;
	List<String> dataList;
	List<AntiFinger> antiFingerTable;
	LRUCache cache;

	public MyClient(List<Finger> fingerTable,Node node,int M,List<String> dataList,List<AntiFinger> antiFingerTable, LRUCache cache){
		//it will have finger table, successor, predecessor as arguments
		this.fingerTable = fingerTable;
		this.node = node;
		this.M = M;
		this.dataList = dataList;
		this.antiFingerTable=antiFingerTable;
		this.cache = cache;
	}

	public void run(){
		try {
			BufferedReader br= new BufferedReader(new InputStreamReader(System.in));

			System.out.print("chord> ");
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

						Operation.deleteMethod(networkObj, node, fingerTable,dataList);

					} else if(command.equals("out")) {
						Operation.outMethod(networkObj,M,node,fingerTable,antiFingerTable,dataList);
					} 
					else if(command.equals("in")) {
						DateFormat df = new SimpleDateFormat("yy/MM/dd HH:mm:ss");
						Calendar calobj = Calendar.getInstance();
						networkObj.date = df.format(calobj.getTime());
						networkObj.miliseconds = calobj.getTimeInMillis();
						Operation.inMethod(networkObj, M, node, fingerTable,antiFingerTable, dataList,cache,false);
					}
					else if(command.equals("printFinger")) {
						Operation.printFingerTable(fingerTable);
					} 
					else if(command.equals("printAntiFinger")) {
						Operation.printAntiFingerTable(antiFingerTable);
					} 
					else if(command.equals("printData")) {
						Operation.printDataTable(dataList);
					}
					else if(command.equals("nodeDetail")) {
						Operation.nodeDeatil(node);
					} 
					else if(command.equals("paChord")) {
						Operation.printAnalysis(networkObj,M,node,fingerTable,antiFingerTable,dataList,cache,false);					
					}
					else if(command.equals("paEnhanced")) {
						Operation.printAnalysis(networkObj,M,node,fingerTable,antiFingerTable,dataList,cache,true);					
					}
					else if(command.equals("printCache")) {
						LRUCache ca = new LRUCache(10);
						ca.print();
					}
					else if(command.startsWith("waWithCache")) {	//word analysis with cache
						String[] parsedInputfinal = command.split("\\s+");
						System.out.println("in readFile " +parsedInputfinal[1]);
						//set last parameter false because we  want to use cache for this analysis
						Operation.readWordsFromFile(parsedInputfinal[1],networkObj,M,node,fingerTable,antiFingerTable,dataList,cache,false);
					}
					else if(command.startsWith("waWithOutCache")) {	//word analysis without cache
						String[] parsedInputfinal = command.split("\\s+");
						System.out.println("in readFile " +parsedInputfinal[1]);
						//set last parameter true because we don't want to use cache for this analysis
						Operation.readWordsFromFile(parsedInputfinal[1],networkObj,M,node,fingerTable,antiFingerTable,dataList,cache,true);
					}
					else if(command.startsWith("storeWordFile")) {
						Operation.storeWordsFromFile(networkObj,M,node,fingerTable,antiFingerTable,dataList,cache);
					}
					else{
						System.out.println("Please enter valid command");
					}
					System.out.print("chord> ");
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

			line = line.substring(3,line.length());

			String[] parts = line.split(",");
			if (parts.length==3) {
				obj = new MyNetwork();
				obj.command = "add";

				List<String> temp = new ArrayList<>();
				temp.add(parts[0].trim()); //hostKey
				temp.add(parts[1].trim()); //host IP
				temp.add(parts[2].trim()); //host Port number
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
				obj.requestedNodeId = node.getId();
				obj.requestedNodeIp = node.getIp();
				obj.requestedNodeport = node.getPortNo();
			}
		}else if(line.contains("printFinger")) {
			obj = new MyNetwork();
			obj.command = "printFinger";
		} 
		else if(line.contains("printAntiFinger")) {
			obj = new MyNetwork();
			obj.command = "printAntiFinger";
		} 
		else if(line.contains("printData")) {
			obj = new MyNetwork();
			obj.command ="printData";
		} else if (line.contains("in") && line.length() > 2) {
			String[] parts = line.split(" ");
			if (parts.length == 2) {
				obj = new MyNetwork();
				obj.traversalList = new ArrayList<>();
				obj.command = "in";
				obj.dataString = parts[1];
				obj.requestedNodeId = node.getId();
				obj.requestedNodeIp = node.getIp();
				obj.requestedNodeport = node.getPortNo();
			}
		}else if(line.contains("nodeDetail")) {
			obj = new MyNetwork();
			obj.command ="nodeDetail";
		}
		else if(line.contains("paChord")) {
			obj = new MyNetwork();
			obj.command ="paChord";
		}
		else if(line.contains("paEnhanced")) {
			obj = new MyNetwork();
			obj.command ="paEnhanced";
		}
		else if(line.contains("waWithCache")) {
			obj = new MyNetwork();
			obj.command =line;
		}else if(line.contains("waWithOutCache")) {
			obj = new MyNetwork();
			obj.command =line;
			System.out.print(line);
		}
		else if(line.contains("storeWordFile")) {
			obj = new MyNetwork();
			obj.command =line;
		}else if(line.contains("printCache")) {
			obj = new MyNetwork();
			obj.command =line;
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
						//Operation.printFingerTable(fingerTable);
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
}
