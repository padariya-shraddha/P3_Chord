package chord;
/**
 * @author Team-6
 * @description This Class has been used to keep other node information during cache 
 */

public class NodeInfo {
	NodeInfo pre; //for caching we are using to store node information
	NodeInfo next;
	String key;
    String ip;
    int port;
    int nodeId;
    
    public NodeInfo(String key, String ip,int port,int nodeId){
    	this.key = key;
        this.ip = ip;
        this.port = port;
        this.nodeId = nodeId;
    }

}
