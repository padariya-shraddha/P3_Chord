package chord;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * @author Team-6
 * @description Main class which will invoke client thread and server thread
 */

public class P3
{	

	public static final int M = 6;
	public static final int CACHESIZE = 2;

	private static ServerSocket serversocket;
	private static int local_host_key;
	static String local_ip;
	static int local_port;
	static private Map<Integer,String> dataKeys = new HashMap<Integer,String>();
	static public Finger finger;
	static public List<Finger> fingerTable;
	static public List<AntiFinger> antiFingerTable;
	static private int nodeId;
	static public Node successorNode;
	static public Node predecessorNode;
	static public LRUCache cache;

	private static void initialise (String ip, int portNo) {
		successorNode = new Node(nodeId,ip,portNo);
		predecessorNode = new Node(nodeId,ip,portNo);
		int previousGap = -1;
		for(int i = 0; i < M; i++) {
			// for finger table
			int gap = (int) (nodeId+ Math.pow(2,i));
			
			if (gap >= Math.pow(2, M)) {
				gap = (int) (gap % (Math.pow(2, M) ));
			}
			
			int range = (int) (gap + Math.pow(2, i));
			if (range >= Math.pow(2, M)) {
				range = (int) (range % (Math.pow(2, M)));
			}
			Finger finger = new Finger(gap, range , nodeId, ip ,portNo );
			fingerTable.add(finger);
			
			// for anti finger table
			
			gap = (int) (nodeId - Math.pow(2, i));
			
			if (gap < 0) {
				gap = (int) Math.pow(2, M) + gap;
			}
			// storing only the upper limit
			if(previousGap == -1) {
				range = (int) (gap + Math.pow(2, i));
			 
				if (range >= Math.pow(2, M)) {
					range = (int) (range % Math.pow(2, M));

				}
			} else {
				range = previousGap;
			}
			 
			 AntiFinger antiFinger = new AntiFinger(gap, range, nodeId,ip,portNo);
			 antiFingerTable.add(antiFinger);
			 previousGap = gap;

		}
	}

	public static void main(String args[]) throws IOException {
		System.out.print("Enter Id :");
		Scanner scan = new Scanner(System.in);
		local_host_key = scan.nextInt(); // assign unique indentifier to host
		nodeId = local_host_key;
		fingerTable = new ArrayList<Finger>();
		antiFingerTable = new ArrayList<AntiFinger>();
		serversocket = new ServerSocket(0);
		
		local_ip = InetAddress.getLocalHost().getHostAddress();
		local_port = serversocket.getLocalPort();
		
		System.out.println(local_ip + " at port number: " + local_port);
		System.out.println();

		initialise(local_ip, local_port);
		//Operation.printFingerTable(fingerTable);
		//Operation.printAntiFingerTable(antiFingerTable);
		
		Node node = new Node(local_host_key, local_ip, local_port);
		String finger_path = Operation.createLogFileFinger(local_host_key);
		String AntiFinger_path = Operation.createLogFileAntiFinger(local_host_key);
		
		List<String> dataList = new ArrayList<>();
		cache = new LRUCache(CACHESIZE);

		MyServer server = new MyServer(serversocket, local_host_key, local_ip,local_port,fingerTable,node,finger,successorNode,predecessorNode,M,dataList,antiFingerTable,cache);
		MyClient client = new MyClient(fingerTable,node,M,dataList,antiFingerTable, cache);
		server.start();
		client.start();   
		
		
		/*System.out.println("Initialize finger table");
		Operation.printFingerTable(fingerTable);
		System.out.println();*/		
		Operation.writeInLogFilesFinger(fingerTable, finger_path);
		Operation.writeInLogFilesAntiFinger(antiFingerTable, AntiFinger_path);
		Fix_finger fixFinger = new Fix_finger(local_host_key, local_ip,local_port,fingerTable,node,finger_path,antiFingerTable,AntiFinger_path);
		fixFinger.start();
	}
	
	public void printFingerTable(){
		for(int i = 0 ;i < M;i++) {
			fingerTable.get(i).print();
			System.out.println();
		}
		System.out.println();
	}
	
}
