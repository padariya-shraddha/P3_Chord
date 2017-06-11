package chord;
//finx_finger
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * @author Team-6
 * @description This thread would run in a background which would eventually make the finger table and anti-finger table 
 * consistent for all nodes.
 */

public class Fix_finger extends Thread{

	private int local_host_key;
	private String local_ip;
	private int local_port;
	private List<Finger> local_fingerTable;
	private List<AntiFinger> local_antiFingerTable;
	private Node node;
	private String finger_path;
	private String AntiFinger_path;
	public Fix_finger(int local_host_key,String local_ip,int local_port,List<Finger> local_fingerTable,Node node,String finger_path,List<AntiFinger> local_antiFingerTable,String AntiFinger_path){
		this.local_host_key = local_host_key;
		this.local_ip = local_ip;
		this.local_port = local_port;
		this.local_fingerTable = local_fingerTable;
		this.node = node;
		this.finger_path = finger_path;
		this.local_antiFingerTable = local_antiFingerTable;
		this.AntiFinger_path = AntiFinger_path;
	}

	public void run(){
		//System.out.println();
		boolean msgPrinted = false;

		while(true){
			if(node.getId() == node.getSuccessor().getId() && node.getId() == node.getPredecessor().getId()){
				if(!msgPrinted){
					Operation.writeInLogFilesFinger(local_fingerTable, finger_path);
					msgPrinted = true;
				}
			}
			else if(node.getId() != node.getSuccessor().getId() && node.getId() != node.getPredecessor().getId()){
				msgPrinted = false;
			}

			fix_finger_update();
			fix_Antifinger_update();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				//e.printStackTrace();
				//System.out.println("fix_finger : connection error --1");	//commented by shraddha
			}
		}
	}

	public void fix_finger_update(){
		
		if(node.getId() != node.getSuccessor().getId() && node.getId() != node.getPredecessor().getId()){
			for(Finger finger : local_fingerTable)
			{
				try
				{
					int other_node_id = finger.getSuccessor();
					String other_node_ip = finger.getIp();
					int other_node_port = finger.getPort();
					int finger_key = finger.getKey(); //this key range we need to confirm

					MyNetwork obj = new MyNetwork();
					obj.command = "fixFinger_validateRange";
					obj.keyTobeValidate = finger_key;
					//Node responsibleNode = sendRequest(other_node_ip,other_node_port, obj);
					String response_message = sendRequest(other_node_ip,other_node_port, obj);

					if(response_message!= null)
					{
						String[] parsedArray = response_message.split("/");
						if(Integer.parseInt(parsedArray[0]) != other_node_id){
							finger.setSuccessorNode(Integer.parseInt(parsedArray[0]));
							finger.setip(parsedArray[1]);
							finger.setPort(Integer.parseInt(parsedArray[2]));
						}
					}
					else{
						MyNetwork newObj = new MyNetwork();
						newObj.command = "searchKeyForFixFinger";
						newObj.keyTobeValidate = finger_key;
						newObj.sendResponseToNode = new Node(node.getId(),node.getIp(),node.getPortNo());
						String succIp = node.getSuccessor().getIp();
						int succPort = node.getSuccessor().getPortNo();
						Operation.sendMessage(succIp,succPort,newObj);
						continue;
					}
				} catch (Exception e) {
					//System.out.println("fix_finger_update : error");	//commented by shraddha
					continue;
				}
			}
			Operation.writeInLogFilesFinger(local_fingerTable, finger_path);
		}
	}

	public void fix_Antifinger_update(){

		if(node.getId() != node.getSuccessor().getId() && node.getId() != node.getPredecessor().getId()){
			for(AntiFinger antifinger : local_antiFingerTable)
			{
				try
				{
					int other_node_id = antifinger.getSuccessor();
					String other_node_ip = antifinger.getIp();
					int other_node_port = antifinger.getPort();
					int finger_key = antifinger.getKey(); //this key range we need to confirm

					MyNetwork obj = new MyNetwork();
					obj.command = "fixAntiFinger_validateRange";
					obj.keyTobeValidate = finger_key;
					//Node responsibleNode = sendRequest(other_node_ip,other_node_port, obj);
					String response_message = sendRequest(other_node_ip,other_node_port, obj);

					if(response_message!= null)
					{
						String[] parsedArray = response_message.split("/");
						if(Integer.parseInt(parsedArray[0]) != other_node_id){
							antifinger.setSuccessorNode(Integer.parseInt(parsedArray[0]));
							antifinger.setip(parsedArray[1]);
							antifinger.setPort(Integer.parseInt(parsedArray[2]));
						}
					}
					else{
						MyNetwork newObj = new MyNetwork();
						newObj.command = "searchKeyForFixAntiFinger";
						newObj.keyTobeValidate = finger_key;
						newObj.sendResponseToNode = new Node(node.getId(),node.getIp(),node.getPortNo());
						String succIp = node.getSuccessor().getIp();
						int succPort = node.getSuccessor().getPortNo();
						Operation.sendMessage(succIp,succPort,newObj);
						continue;
					}
				} catch (Exception e) {
					//System.out.println("fix_Antifinger_update : error");
					continue;
				}
			}
			Operation.writeInLogFilesAntiFinger(local_antiFingerTable, AntiFinger_path);
		}
	}
	public String sendRequest(String ip, int port,MyNetwork modelObj){
		Socket s1=null;
		boolean returnFlag;
		ObjectOutputStream out=null;
		ObjectInputStream in=null;
		String response_message = null;
		try {
			s1 = new Socket(ip, port);
			out = new ObjectOutputStream(s1.getOutputStream());
			in = new ObjectInputStream(s1.getInputStream());
			out.writeObject(modelObj);
			MyNetwork response = new MyNetwork();
			response = (MyNetwork) in.readObject();
			response_message = response.response_message;
		} catch (IOException | ClassNotFoundException e) {
			//System.out.println("fix_finger : connection error");
			returnFlag= false;
			response_message = null;
			return response_message;
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
		return response_message;
	}
}

