package chord;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Operation {

	public static void deleteMethod(MyNetwork networkObj, Node node,List<Finger> fingerTable  ){
		int nodeToFind = networkObj.nodeToDeleteId;
		if (node.getId() == nodeToFind) {
			if (node.getId() == node.getSuccessor().getId() && node.getId() == node.getPredecessor().getId()) {
				System.exit(0);
			}
			else {
				networkObj.command = "update after delete";
				// notifying successor the deletion of the current node
				if(node.getId() != node.getSuccessor().getId()) {
					sendMessage(node.getSuccessor().getIp(), node.getSuccessor().getPortNo(), networkObj);
				}
				// notifying predecessor the deletion of the current node
				if(node.getId() != node.getPredecessor().getId()) {
					sendMessage(node.getPredecessor().getIp(), node.getPredecessor().getPortNo(), networkObj);
				}
				// deleting the node
				System.exit(0);
			}
		}
		else {
			for (Finger finger : fingerTable) {
				int tempKey = finger.getKey();
				int tempRange = finger.getSpan();

				// 
				if (nodeToFind >= tempKey || nodeToFind < tempRange) {
					//send request to this node
					String ip = finger.getIp();
					int port = finger.getPort();
					try {
						Socket s = new Socket(ip, port);
						ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
						out.writeObject(networkObj);
						out.close();
						s.close();
						break;
					} catch (IOException e) {

					} 
				}
			}
		}
	}
	public static void sendMessage(String ip,int portNo, MyNetwork networkObj) {
		try {
			Socket s = new Socket(ip, portNo);
			ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
			out.writeObject(networkObj);
			out.close();
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean checkSpanRange(int start,int end,int searchKey,boolean flag,int M) {
		boolean result = false;
		int keyStart;
		int keyEnd;

		keyEnd = (start<end) ? end : (int) (end + Math.pow(2, M));

		if(flag && (searchKey >= start && searchKey <= end)) {result = true;}
		if(!flag && (searchKey >= start && searchKey < end)) {result = true;}

		return result;
	}

	public static  boolean sendRequest(String ip, int port,MyNetwork modelObj){
		Socket s1=null;
		boolean returnFlag;
		ObjectOutputStream out=null;
		ObjectInputStream in=null;
		try {
			s1 = new Socket(ip, port);
			out = new ObjectOutputStream(s1.getOutputStream());
			in = new ObjectInputStream(s1.getInputStream());
			out.writeObject(modelObj);
			MyNetwork response = (MyNetwork) in.readObject();
			returnFlag = response.response;

		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			returnFlag= false;
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
		return returnFlag;
	}


}
