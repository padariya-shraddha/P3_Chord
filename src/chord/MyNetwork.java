package chord;

import java.io.Serializable;
import java.util.List;

public class MyNetwork implements Serializable{

	public String command;
	public List<String> addObject;	//id, ip ,port
	public boolean response; 
	public String response_message; 
	public List<Finger> fingerTable;
	public List<AntiFinger> antiFingerTable;
	public Node responsibleNode; // used by fix_finger class
	public int nodeToDeleteId;
	public int keyTobeValidate; //key to be validate in case of fix finger
	public Node nodeToDelete;
	public Node predecessor;	//for passing predecessor in case of add
	public Node successor;	//for passing successor in case of add
	public String dataString;
	public int requestedNodeId;
	public String requestedNodeIp;
	public int requestedNodeport;
	public int respondedNodeId;
	public String respondedNodeIp;
	public int respondedNodeport;
	public List<String> dataList; // for passing set of data
	public int hopCount;
	public Node sendResponseToNode;
}
