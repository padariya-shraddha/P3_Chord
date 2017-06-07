package chord;
//finx_finger
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class Fix_finger extends Thread{

  private int local_host_key;
  private String local_ip;
  private int local_port;
  private List<Finger> local_fingerTable;
  private Node node;
  private String path;
  public Fix_finger(int local_host_key,String local_ip,int local_port,List<Finger> local_fingerTable,Node node,String path){
      this.local_host_key = local_host_key;
      this.local_ip = local_ip;
      this.local_port = local_port;
      this.local_fingerTable = local_fingerTable;
      this.node = node;
      this.path = path;

  }

  public void run(){
	  System.out.println();

	  while(true){
		  fix_finger_update();
		  fix_Antifinger_update();
		  try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
                  String[] parsedArray = response_message.split("/");
                  if(Integer.parseInt(parsedArray[0]) != other_node_id){
                      finger.setSuccessorNode(Integer.parseInt(parsedArray[0]));
                      finger.setip(parsedArray[1]);
                      finger.setPort(Integer.parseInt(parsedArray[2]));
                  }



              } catch (Exception e) {
                  e.printStackTrace();
              }

          }

          Operation.writeInLogFiles(local_fingerTable, path);

      }
  }

  public void fix_Antifinger_update(){

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
                  obj.command = "fixAntiFinger_validateRange";
                  obj.keyTobeValidate = finger_key;
                  //Node responsibleNode = sendRequest(other_node_ip,other_node_port, obj);
                  String response_message = sendRequest(other_node_ip,other_node_port, obj);
                  String[] parsedArray = response_message.split("/");
                  if(Integer.parseInt(parsedArray[0]) != other_node_id){
                      finger.setSuccessorNode(Integer.parseInt(parsedArray[0]));
                      finger.setip(parsedArray[1]);
                      finger.setPort(Integer.parseInt(parsedArray[2]));
                  }



              } catch (Exception e) {
                  e.printStackTrace();
              }

          }

          Operation.writeInLogFiles(local_fingerTable, path);

      }
  }
  public String sendRequest(String ip, int port,MyNetwork modelObj){
      Socket s1=null;
      boolean returnFlag;
      ObjectOutputStream out=null;
      ObjectInputStream in=null;
      //Node response = null;
      String response_message = null;
      try {
          s1 = new Socket(ip, port);
          out = new ObjectOutputStream(s1.getOutputStream());
          in = new ObjectInputStream(s1.getInputStream());
          out.writeObject(modelObj);
          MyNetwork response = new MyNetwork();
          //response = (Node) in.readObject();
          response = (MyNetwork) in.readObject();
          response_message = response.response_message;
          //returnFlag = response.response;



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

      //return response;
      return response_message;

  }
}

