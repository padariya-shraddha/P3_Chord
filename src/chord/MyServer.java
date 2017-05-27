package chord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
		String line;
        BufferedReader in = null;
        PrintWriter out = null;
        try {
            in = new BufferedReader(new InputStreamReader(s.getInputStream())); //wait for Input message
            out = new PrintWriter(s.getOutputStream(), true);

        } catch (IOException e) {
            System.out.println("in or out failed");
            throw new RuntimeException();
        }

        try {
            String inputString = in.readLine();
            System.out.println("Request received " + inputString);

        } catch (Exception e) {
            e.printStackTrace();

        }
	}
}
