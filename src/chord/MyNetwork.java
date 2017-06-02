package chord;

import java.io.Serializable;
import java.util.List;

public class MyNetwork implements Serializable{

	public String command;
	public List<String> addObject;	//id, ip ,port
	public boolean response; 
	public String response_message; 
	public List<Finger> fingerTable;
	public Node responsibleNode; // used by fix_finger class
	public int nodeToDeleteId;
	public int keyTobeValidate; //key to be validate in case of fix finger
	public Node nodeToDelete;
	public Node predecessor;	//for passing predecessor in case of add
	public Node successor;	//for passing successor in case of add
	public String dataString;
}
