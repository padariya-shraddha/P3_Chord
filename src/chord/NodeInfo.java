package chord;

public class NodeInfo {
	NodeInfo pre; //for caching we are using to store node information
	NodeInfo next;//for caching we are using to store node information
    String ip;
    int port;
    int nodeId;
    
    public NodeInfo(String ip,int port,int nodeId){
        this.ip = ip;
        this.port = port;
        this.nodeId = nodeId;
    }

}
