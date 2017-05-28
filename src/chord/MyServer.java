package chord;
//dfgh
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class MyServer extends Thread{
	
	int portNumber;
	int hostKey;
	String ipAddr;
	ServerSocket serverSocket;
	
	public MyServer(ServerSocket serverSocket,int hostKey,String ipAddr,int portNumber,List<Finger> fingerTable){
        //it will have finger table, successor, predecessor as arguments
        this.serverSocket = serverSocket;
        this.portNumber = portNumber;
        this.hostKey = hostKey;
        this.ipAddr = ipAddr;
    }
	
	public void run(){
		Socket s=null;
		ServerSocket ss=null;
		
		try{
			//ss = new ServerSocket(portNumber);

			while(true){
				s= serverSocket.accept();
				
				ServerThread st=new ServerThread(s,portNumber,hostKey,ipAddr);
				st.start();
			}

		}
		catch(IOException e){
			e.printStackTrace();
			System.out.println("Server error");

		}finally{
			try {
				ss.close();
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
	public ServerThread(Socket s,int portNumber,int hostKey,String ipAddr){
		this.s = s;
		this.portNumber = portNumber;
		this.hostKey = hostKey;
		this.ipAddr = ipAddr;
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
				
			}

			modelObj.response= true;
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
}
