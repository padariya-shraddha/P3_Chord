package chord;

import java.io.Serializable;
import java.util.List;

public class MyNetwork implements Serializable{

	public String command;
	public List<String> addObject;	//id, ip ,port
	public boolean response; 
	public List<Finger> fingerTable;
}
