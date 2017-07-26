import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * 
 * @author saraa This class runs Dijkstra to find the shortest path between a
 *         pair of nodes
 */
public class Dijkstra {
    private Graph g;
    private boolean[] finished;
    private int[] predecessors;
    private int[] distance;
    private long[][] capacity;
    // private Map<Integer, Integer> distance;

    public Dijkstra(Graph graph, long[][] capacity) {
        this.g = graph;
        this.capacity = capacity;
        // g = new Graph(graph); // Create a copy of the graph
    }

    public Dijkstra(Graph graph) {
        this.g = graph;
        this.capacity = null;
        // g = new Graph(graph); // Create a copy of the graph
    }

    protected class PQEntry implements Comparable<PQEntry> {
        private int nodeId;
        private int distanceFromSource;

        public PQEntry() {
            nodeId = distanceFromSource = -1;
        }

        public PQEntry(int nodeId, int distanceFromSource) {
            this.nodeId = nodeId;
            this.distanceFromSource = distanceFromSource;
        }

        public void setNodeId(int nodeId) {
            this.nodeId = nodeId;
        }

        public int getNodeId() {
            return this.nodeId;
        }

        public void setDistanceFromSource(int distanceFromSource) {
            this.distanceFromSource = distanceFromSource;
        }

        public int getDistanceFromSource() {
            return distanceFromSource;
        }

        @Override
        public int compareTo(PQEntry o) {
            return distanceFromSource - o.distanceFromSource;
        }
    }

    // Gets source, destination, and a threshold capacity. Returns the shortest
    // path from source to destination with at least that capacity.
    public ArrayList<Tuple> getPath(int source, int destination, int requiredCap) {
        int numNodes = g.getNodeCount();
        finished = new boolean[numNodes];
        predecessors = new int[numNodes];
        distance = new int[numNodes];
        for (int i = 0; i < g.getNodeCount(); ++i) {
            distance[i] = Integer.MAX_VALUE;
            predecessors[i] = -1;
            finished[i] = false;
        }
        distance[source] = 0;
        PriorityQueue<PQEntry> pQueue = new PriorityQueue<PQEntry>();
        pQueue.add(new PQEntry(source, 0));
        while (!pQueue.isEmpty()) {
            PQEntry entry = pQueue.poll();
            int u = entry.getNodeId();
            finished[u] = true;
            if (u == destination)
                break;
            for (int i = 0; i < g.getAdjList().get(u).size(); ++i) {
                EndPoint e = g.getAdjList().get(u).get(i);
                int v = e.getNodeId();
                
                if (capacity != null && capacity[u][v] < requiredCap)
                    continue;
                else if (e.getBw() < requiredCap)
                    continue;
                if (finished[v])
                    continue;
                if (distance[v] > distance[u]) {
                    distance[v] = distance[u];
                    predecessors[v] = u;
                    pQueue.add(new PQEntry(v, distance[v]));
                }
            }
        }
        if (distance[destination] == Integer.MAX_VALUE) {
            return null;
        }
        ArrayList<Tuple> path = new ArrayList<Tuple>();
        int step = destination;
        
        while (predecessors[step] != -1) {
            path.add(new Tuple(step, predecessors[step]));
            step = predecessors[step];
        }    
        
        return path;
    }
}