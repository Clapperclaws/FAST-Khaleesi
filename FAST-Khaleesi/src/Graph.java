import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/*Generic Graph that will be used to represent virtual, IP, and OTN graphs*/
public class Graph {
    // Every node in the graph is associated with a list of endpoints
    private ArrayList<ArrayList<EndPoint>> adjList;
    
    private int[] nodeCap;
    private int[] interNodeSwitchingCap;

    // Default constructor
    public Graph(int N) {
        adjList = new ArrayList<ArrayList<EndPoint>>();
        for (int i = 0; i < N; i++) {
            adjList.add(i, new ArrayList<EndPoint>());
        }
        nodeCap = new int[N];
        interNodeSwitchingCap = new int[N];
    }
    
    //Copy Constructor
    public Graph (Graph g){
        adjList = new ArrayList<ArrayList<EndPoint>>();
        for (int i = 0; i < g.adjList.size(); i++) {
            adjList.add(i, new ArrayList<EndPoint>());
            for (int j = 0; j < g.adjList.get(i).size(); j++) {
                EndPoint p = new EndPoint(g.adjList.get(i).get(j));
                adjList.get(i).add(p);
            }
        }
        nodeCap = new int[g.getAdjList().size()];
        interNodeSwitchingCap = new int[g.getAdjList().size()];
        for(int i=0;i<nodeCap.length;i++){
        	nodeCap[i] = g.getNodeCap()[i];
        	interNodeSwitchingCap[i] = g.getInterNodeSwitchingCap()[i];
        }
    }

    public ArrayList<ArrayList<EndPoint>> getAdjList() {
		return adjList;
	}

	public void setAdjList(ArrayList<ArrayList<EndPoint>> adjList) {
		this.adjList = adjList;
	}

	// Add a single end point to the list of end points for a given node.
    public void addEndPoint(int nodeId, EndPoint endPnt) {
        adjList.get(nodeId).add(endPnt);
    }

    // Get all end points of a given node
    public ArrayList<EndPoint> getAllEndPoints(int nodeId) {
        return adjList.get(nodeId);
    }

    // Get the bandwidth of an incident link
    public int getBW(int source, int destination) {

        ArrayList<EndPoint> endPoints = adjList.get(source);
        for (int i = 0; i < endPoints.size(); i++) {
            if ((endPoints.get(i).getNodeId() == destination))
                return endPoints.get(i).getBw();
        }
        return -1;
    }
    
    public int getNodeCount(){
    	return this.getAdjList().size();
    }

    public int[] getNodeCap() {
		return nodeCap;
	}

	public void setNodesCap(int[] nodesCap) {
		this.nodeCap = nodesCap;
	}

	public int[] getInterNodeSwitchingCap() {
		return interNodeSwitchingCap;
	}

	public void setInterNodeSwitchingCap(int[] interNodeSwitchingCap) {
		this.interNodeSwitchingCap = interNodeSwitchingCap;
	}

	// Print the complete Adjacency List
    public String toString() {
    	String content = "Nodes Spec:\n";
        for (int i = 0; i < nodeCap.length; i++)
            content += "- Node " + i + ", Cap = "+ nodeCap[i] +
            ", Internal Switchin Cap = "+ interNodeSwitchingCap[i]+"\n";

        content += "Adjacency List:\n";
        for (int i = 0; i < adjList.size(); i++)
            content += "\n- Node " + i + " is attached to: \n,"
                    + adjList.get(i).toString();

        return content;
    }

}
