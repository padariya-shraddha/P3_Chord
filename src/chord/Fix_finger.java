package chord;
//finx_finger
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

      for(Finger finger : local_fingertable)
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
              Node responsibleNode = sendRequest(other_node_ip,other_node_port, obj);
              System.out.println("Fix_finger class received response host Key "+responsibleNode.getId());
              if(responsibleNode.getId() != other_node_id){
                  finger.setSuccessorNode(responsibleNode.getId());
                  finger.setip(responsibleNode.getIp());
                  finger.setPort(responsibleNode.getPortNo());
              }

              System.out.println("Fix_finger : updated successfully");

          } catch (Exception e) {
              e.printStackTrace();
          }

      }
      
      System.out.println("updated finger table After running Fix_finger for "+local_hostKey);
		P3 p3 = new P3();
		p3.printFingerTable();
		System.out.println();


  }

  public Node sendRequest(String ip, int port,MyNetwork modelObj){
      Socket s1=null;
      boolean returnFlag;
      ObjectOutputStream out=null;
      ObjectInputStream in=null;
      Node response = null;
      try {
          s1 = new Socket(ip, port);
          out = new ObjectOutputStream(s1.getOutputStream());
          in = new ObjectInputStream(s1.getInputStream());
          out.writeObject(modelObj);
          response = (Node) in.readObject();
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

      return response;

  }
}

