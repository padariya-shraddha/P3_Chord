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


public class P3
{	
	public static final int M = 6;
	private static ServerSocket serversocket;
	private static int local_host_key;
	static String local_ip;
	static int local_port;
	static private Map<Integer,String> dataKeys = new HashMap<Integer,String>();
	static	private Finger finger;
	static	private List<Finger> fingerTable;
	static private int nodeId;
	static private Node successorNode;
	static private Node predecessorNode;

	private static void initialise (String ip, int portNo) {
		successorNode = new Node(nodeId,ip,portNo);
		predecessorNode = new Node(nodeId,ip,portNo);
		for(int i = 0; i < M; i++) {
			int gap = (int) (nodeId+ Math.pow(2,i));
			int range = (int) (gap + Math.pow(2, i));
			if (range > Math.pow(2, M)) {
				range = (int) (range % Math.pow(2, M));
			}
			Finger finger = new Finger(gap, range , nodeId, ip ,portNo );
			//finger[i] = new Finger(gap,nodeId+gap, nodeId, ip ,portNo );
			fingerTable.add(finger);
		}
	}

	public static void main(String args[]) throws IOException {
//		if (args.length != 1) {
//			System.out.println("Please enter host information format : host_key");
//			System.out.println("linda>");
//			return;
//		}
		System.out.print("Enter Id :");

		Scanner scan = new Scanner(System.in);
		local_host_key = scan.nextInt(); // assign unique indentifier to host
		nodeId = local_host_key;
		fingerTable = new ArrayList<Finger>();
		serversocket = new ServerSocket(0);
		
		local_ip = InetAddress.getLocalHost().getHostAddress();
		local_port = serversocket.getLocalPort();
		
		System.out.println(local_ip + " at port number: " + local_port);
		System.out.println();

		
		initialise(local_ip, local_port);
		
		for(int i = 0 ;i < M;i++) {
			fingerTable.get(i).print();
			System.out.println();
		}
		System.out.println();
		
		MyServer server = new MyServer(serversocket, local_host_key, local_ip,local_port,fingerTable);
		MyClient client = new MyClient(fingerTable);
		server.start();
		client.start();   

	}
}
