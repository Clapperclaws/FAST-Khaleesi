

/*Each node in the graph has an array of end points. Each element in the array represents an incident link;
 *  i.e., the index and type of the node at the other end of the link and the bandwidth (demand or capacity) of that link. */
public class EndPoint {

	private int nodeId; // Index of the node at the other end of the link
	private long bw; //Bandwidth (demand/capacity) of the link
	private int type; //Type of the node at the other end of the link

	//Default Constructor
	public EndPoint(){
		nodeId = -1;
		bw = 0;
		type = -1;
	}
	
	//Initializing Constructor
	public EndPoint(int nodeId, long bw, int type){
		this.nodeId = nodeId;
		this.bw     = bw;
		this.type   = type;
	}
	
	//Copy Constructor
	public EndPoint(EndPoint p){
		this.nodeId = p.nodeId;
		this.bw     = p.bw;
		this.type   = p.type;
	}
	
	//Return the index of the node at the other end of the link
	public int getNodeId() {
		return nodeId;
	}
	
	//Set the index of the node at the other end of the link
	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}
	
	//Get the bandwidth (demand/capacity) of the incident link
	public long getBw() {
		return bw;
	}

	//Set the bandwidth (demand/capacity) of the incident link
	public void setBw(long bw) {
		this.bw = bw;
	}
	
	//Get the type of the node at the other end of the link
	public int getType() {
		return type;
	}
	
	//Set the type of the node at the other end of the link
	public void setType(int t) {
		this.type = t;
	}
	
	//Print the attributes of the incident link
	public String toString(){
		return "Node "+nodeId+" of type "+type+", BW = "+bw+"\n\n";
	}
}
