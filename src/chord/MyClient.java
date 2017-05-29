package chord;
//ghj
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class MyClient extends Thread{

	List<Finger> fingerTable;
	public MyClient(List<Finger> fingerTable){
		//it will have finger table, successor, predecessor as arguments
		this.fingerTable = fingerTable;
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
					} 
				}else{
					System.out.println("Please enter valid command");
				}
				
				line = br.readLine();
			}
		} catch (Exception e) {
			// TODO: handle exception
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
				temp.add(parts[0].trim());
				temp.add(parts[1].trim());
				temp.add(parts[1].trim());
				
				obj.addObject =temp;
			}
		}
		
		return obj;
	}

	public void addMethod(MyNetwork networkObj)
	{
		String keytoFind = networkObj.addObject.get(0);
		
		if (StrToIntCheck(keytoFind)) {
			int keytoFind_int= Integer.parseInt(keytoFind);
			for (Finger finger : fingerTable) {
				int tempKey = finger.getKey();
				int tempRange = finger.getSpan();
				
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

	public boolean StrToIntCheck(String str){
		try {
			int i = Integer.parseInt(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
		
	}

	
}
