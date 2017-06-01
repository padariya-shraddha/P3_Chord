package chord;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class Fix_finger extends Thread{

	private int local_hostKey;
	private String local_ip;
	private int local_port;
	private List<Finger> local_fingertable;
	public Fix_finger(int local_host_key,String local_ip,int local_port,List<Finger> local_fingerTable){
		this.local_hostKey = local_hostKey;
		this.local_ip = local_ip;
		this.local_port = local_port;
		this.local_fingertable = local_fingertable;
	
	} 

	public void run(){
		
		for(Finger finger : local_fingertable){
			
			Socket s1;
			try {
				
				/*s1 = new Socket(ip, port);
				ObjectOutputStream out = new ObjectOutputStream(s1.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(s1.getInputStream());
				MyNetwork obj = new MyNetwork();
				
				obj.command = "check_span_range";
				obj.fingerTable = fingerTable;
				obj.predecessor= previousPred;
				obj.successor=node;
				out.writeObject(obj);
				
				out.writeObject(modelObj);
				MyNetwork response = (MyNetwork) in.readObject();
				returnFlag = response.response;
				in.close();
				out.close();
				s1.close();*/
				
				/*
			
			MyNetwork response = (MyNetwork) in.readObject();
			in.close();
			out.close();
			s1.close();*/

			} catch (Exception e) {
				e.printStackTrace();
			} 
			
		}
		
		
	}
}
