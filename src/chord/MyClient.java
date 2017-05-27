package chord;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;


public class MyClient extends Thread{
	
	public MyClient(List<Finger> fingerTable){
		//it will have finger table, successor, predecessor as arguments
		//con
	}

	public void run(){
		try {
			BufferedReader br= new BufferedReader(new InputStreamReader(System.in));
			
			System.out.print("chord > ");
			String line = br.readLine();
			line = line.trim();
			
			while(!line.equals("quit")){
				
				//parse user command
				
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
}
