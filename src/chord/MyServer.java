package chord;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class MyServer extends Thread{
	
	int portNumber;
	int hostKey;
	String ipAddr;
	ServerSocket serverSocket;
	Node node;
	
	public MyServer(ServerSocket serverSocket,int hostKey,String ipAddr,int portNumber,List<Finger> fingerTable,Node node){
        //it will have finger table, successor, predecessor as arguments
        this.serverSocket = serverSocket;
        this.portNumber = portNumber;
        this.hostKey = hostKey;
        this.ipAddr = ipAddr;
        this.node = node;
    }
	
	public void run(){
		Socket s=null;
		ServerSocket ss=null;
		
		try{
			while(true){
				s= serverSocket.accept();
				ServerThread st=new ServerThread(s,portNumber,hostKey,ipAddr,node);
				st.start();
			}
		}
		catch(IOException e){
			e.printStackTrace();
			System.out.println("Server error");

		}finally{
			try {
				if (ss != null) {
					ss.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

class ServerThread extends Thread{ 
	int portNumber;
	int hostKey;
	String ipAddr;
	Socket s=null;
	Node node;
	
	public ServerThread(Socket s,int portNumber,int hostKey,String ipAddr,Node node){
		this.s = s;
		this.portNumber = portNumber;
		this.hostKey = hostKey;
		this.ipAddr = ipAddr;
		this.node= node;
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
				if (modelObj.command=="add") {
					modelObj.response = addNodeToChord(modelObj);
				} else {

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

	public boolean addNodeToChord(MyNetwork modelObj){
		boolean retrunFlag = true;
		int newNodeKey = Integer.parseInt(modelObj.addObject.get(0));
		//check key to add is in self range
		if (newNodeKey<=node.getId() && newNodeKey>node.getPredecessor().getId()) {
			
			//NIDHI ADD YOUR CODE HERE
			
			//update self finger table(with added new node)
			//pass this finger table to new node(you can get ip, port og this node from modelObj.addObject index 1= ip, index 2 = port)
			//also pass data related to keys to new node
			
			//if everything above goes well retrunFlag =true
			//else retrunFlag =false
			
		}else{	//else pass it to next Successor;
			String ip = node.getSuccessor().getIp();
			int port = node.getSuccessor().getPortNo();
			
			Socket s1;
			try {
				s1 = new Socket(ip, port);
				ObjectOutputStream out = new ObjectOutputStream(s1.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(s1.getInputStream());
				out.writeObject(modelObj);
				MyNetwork response = (MyNetwork) in.readObject();
				retrunFlag = response.response;
				in.close();
				out.close();
				s1.close();
			
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			} 
		}
		
		return retrunFlag;
	}
}
