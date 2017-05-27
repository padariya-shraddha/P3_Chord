package chord;
//shraddha
public class Node {
	private int id;
	private String ip;
	private int portNo;
	public Node(int id, String ip, int portNo) {
		// TODO Auto-generated constructor stub
		this.id =id;
		this.ip = ip;
		this.portNo = portNo;

	}

	public void setId(int id) {
		this.id = id;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public void setPortNo(int portNo) {
		this.portNo = portNo;
	}

	public int getId() {
		return this.id;
	}

	public String getIp() {
		return this.ip;
	}

	public int getPortNo() {
		return this.portNo;
	}



}
