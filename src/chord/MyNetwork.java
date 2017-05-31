package chord;

import java.io.Serializable;
import java.util.List;

public class MyNetwork implements Serializable{

	public String command;
	public List<String> addObject;	//id, ip ,port
	public boolean response; 
	public List<Finger> fingerTable;
	public Node predecessor;	//for passing predecessor in case of add
	public Node successor;	//for passing successor in case of add
}
