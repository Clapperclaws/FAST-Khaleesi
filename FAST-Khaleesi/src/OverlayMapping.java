import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 
 * This class represents an Overlay Mapping Solution
 *
 */
public class OverlayMapping {

    int[] nodeMapping; // This array represents the node mapping; where the
                       // index of the array is the virtual node index and the
                       // value is the physical node index
    HashMap<Tuple, ArrayList<Tuple>> linkMapping; // This array represents the
                                                  // link mapping.

    // Initialization Constructor
    public OverlayMapping(int N) {
        nodeMapping = new int[N];

        // Initialize Node Mapping Solution to -1
        for (int i = 0; i < nodeMapping.length; i++)
            nodeMapping[i] = -1;

        linkMapping = new HashMap<Tuple, ArrayList<Tuple>>();
    }

    // Get the node mapping
    public int getNodeMapping(int virtualNode) {
        return nodeMapping[virtualNode];
    }

    // Set the node mapping
    public void setNodeMappingSolution(int virtualNode, int physicalNode) {
        nodeMapping[virtualNode] = physicalNode;
    }

    // Get link mapping.
    public ArrayList<Tuple> getLinkMapping(Tuple t) {
        return linkMapping.get(t);
    }

    public void setLinkMapping(Tuple t, Tuple p) {
        if (!linkMapping.containsKey(t))
            linkMapping.put(t, new ArrayList<Tuple>());
        linkMapping.get(t).add(p);
    }

    // Set link Mapping
    public void setLinkMappingPath(Tuple t, ArrayList<Tuple> path) {
        if (!linkMapping.containsKey(t))
            linkMapping.put(t, new ArrayList<Tuple>());
        linkMapping.get(t).addAll(path);
    }

    // This Function Aggregates Node Placement
    public void incrementNodeEmbed(OverlayMapping sol) {
        for (int i = 0; i < sol.nodeMapping.length; i++) {
            if (sol.nodeMapping[i] != -1) {
                this.nodeMapping[i] = sol.nodeMapping[i];
            }
        }
    }

    // This function returns true if a given IP node (indicated by nodeIndex) is
    // occupied
    public boolean isOccupied(int nodeIndex) {
        for (int i = 0; i < this.nodeMapping.length; i++) {
            if (nodeMapping[i] == nodeIndex)
                return true;
        }
        return false;
    }

    public int numNodesSettled() {
    	int counter = 0;
    	for(int i=0;i<nodeMapping.length;i++){
    		if (nodeMapping[i] != -1)
    			counter++;
    	}
    	return counter;
    }
    
    
    public boolean isNodeSettled(int nodeId, int requiredLinks) {
        if (nodeMapping[nodeId] == -1)
            return false;
        int settledLinks = 0;
        Iterator<Tuple> it = linkMapping.keySet().iterator();
        while (it.hasNext()) {
            Tuple vLink = it.next();
            if (vLink.getSource() == nodeId)
                ++settledLinks;
            if (vLink.getDestination() == nodeId)
                ++settledLinks;
        }
        
        System.out.println("Number of Settled Links for VN "+nodeId+" is = "+settledLinks);
        return settledLinks == requiredLinks;
    }

    // Print the content of the overlay mapping
    public String toString() {
        String content = "";
        for (int i = 0; i < nodeMapping.length; i++) {
            if (nodeMapping[i] == -1)
                continue;
            content += " Node at index " + i
                    + " is mapped to Physical Node Index " + nodeMapping[i]
                    + "\n";
        }

        Iterator it = linkMapping.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            content += " Link " + pair.getKey() + " is routed via Path "
                    + pair.getValue() + "\n";
        }
        return content;
    }

}
