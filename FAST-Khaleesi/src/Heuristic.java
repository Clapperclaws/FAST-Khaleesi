import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

public class Heuristic {

	ArrayList<ArrayList<Tuple>> validChains;
	ArrayList<Tuple>[] adjacencyList;
	Flow f;
	int[][] Omega;

	public Heuristic() {
		validChains = new ArrayList<ArrayList<Tuple>>();
	}

	public void generateOmega(Flow f, ArrayList<Tuple> E, int[][] M) {
		// Omega is populated by NF index and not NF type
		Omega = new int[f.getChain().size()][f.getChain().size()]; // Omega = C ||
		                                                           // !C ^ M ^ E
		for (int i = 0; i < f.getChain().size() - 1; i++) {
			for (int j = i + 1; j < f.getChain().size(); j++)
				Omega[i][j] = 1;
		}
		for (int i = 0; i < E.size(); i++) {
			// System.out.println(E.get(i).getSource()+","+E.get(i).getDestination());
			if ((M[E.get(i).getSource()][E.get(i).getDestination()]) == 1)
				Omega[getIndexNF(f, E.get(i).getSource())][getIndexNF(f,
				    E.get(i).getDestination())] = 1;
		}
		for (int i = 0; i < f.getChain().size(); i++) {
			for (int j = 0; j < f.getChain().size(); j++)
				System.out.print(Omega[i][j] + ",");
			System.out.print("\n");
		}
		System.out.print("\n");
	}

	public ArrayList<Tuple> getCandidateServers(Graph G, int minDemand,
	    long[][] capacity) {

		ArrayList<Tuple> candidateNodes = new ArrayList<Tuple>();
		Dijkstra djst = new Dijkstra(G, capacity);

		HashMap<Tuple, ArrayList<Tuple>> paths = new HashMap<Tuple, ArrayList<Tuple>>();
		int ingress = f.getSource();
		int egress = f.getDestination();
		for (int i = 0; i < G.getNodeCount(); i++) {
			// System.out.println("Node " + i);
			int di = 0;
			int de = 0;
			// Skip Node if it doesn't have enough node resources to support any NF
			if (G.getNodeCap()[i] < minDemand)
				continue;

			if (i != f.getSource()) {
				ArrayList<Tuple> pathToIng = djst.getPath(ingress, i, f.getBw());
				di = pathToIng.size();
				// System.out.println("di_" + i + "=" + di);
			}

			if (i != f.getDestination()) {
				ArrayList<Tuple> pathToEgr = djst.getPath(egress, i, f.getBw());
				de = pathToEgr.size();
				// System.out.println("de_" + i + "=" + de);
			}
			// System.out.println(" Value = " + G.getNodeCap()[i] / (di + de));
			double oc = (double) G.getNodeCap()[i] / (double) (di + de);
			candidateNodes.add(new Tuple(i, (int) (oc * 10e4)));
		}

		return candidateNodes;
	}

	public OverlayMapping executeHeuristic(Graph G, int[][] M, int flowIdx,
	    Flow f, ArrayList<Tuple> E, int[] mbSpecs, String logPrefix)
	    throws IOException {
		long startTime = System.currentTimeMillis();
		this.f = f;
		adjacencyList = new ArrayList[f.getChain().size()];

		// Step 0 - Generate Omega
		generateOmega(f, E, M);

		// Step 1 - Generate Valid Chains
		getValidChains(E, M, f);

		// Step 2 - Find shortest path to ingress & egress node
		long[][] capacity = new long[G.getNodeCount()][G.getNodeCount()];
		for (int i = 0; i < G.getAdjList().size(); i++) {
			for (int j = 0; j < G.getAllEndPoints(i).size(); j++)
				capacity[i][G.getAllEndPoints(i).get(j).getNodeId()] = G
				    .getAllEndPoints(i).get(j).getBw();
		}

		// Get Min MB Demand in the chain
		int minDemand = Integer.MAX_VALUE;
		for (int i = 0; i < f.getChain().size(); i++) {
			if (mbSpecs[f.getChain().get(i)] < minDemand)
				minDemand = mbSpecs[f.getChain().get(i)];
		}

		// Candidate Node Index, Cost
		ArrayList<Tuple> candidateNodes = getCandidateServers(G, minDemand,
		    capacity);// new ArrayList<Tuple>();

		// Step 3 - Sort Physical Servers
		Collections.sort(candidateNodes);
		// System.out.println("Candidate Nodes");
		// System.out.println(candidateNodes);

		ArrayList<OverlayMapping> omSol = new ArrayList<OverlayMapping>();
		for (int i = 0; i < validChains.size(); i++) {
			// System.out.println("For Valid Chain "+validChains.get(i));
			OverlayMapping om = getOverlaySolution(G, validChains.get(i), mbSpecs,
			    candidateNodes, capacity);
			if (om != null)
				omSol.add(om);
		}

		// Step 4 - For each OverlayMapping Solution perform Link Embedding
		ArrayList<Integer> omCosts = new ArrayList<Integer>();
		for (int i = 0; i < omSol.size(); i++) {
			int cost = 0;
			Iterator it = omSol.get(i).linkMapping.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry) it.next();
				// if(omSol.get(i).linkMapping.containsKey(pair.getKey()))
				cost += ((ArrayList<Tuple>) pair.getValue()).size();
			}
			omCosts.add(cost);
			omSol.get(i).setMappingCost(cost);
		}
		/*
		 * for(int i=0; i<omSol.size(); i++){ int cost = 0;
		 * 
		 * for(int j=0;j<omSol.get(i).getChainOrder().size()+2;j++){
		 * System.out.println(j+"-"+omSol.get(i).getChainOrder().size()); Dijkstra
		 * djst = new Dijkstra(G,capacity); Tuple tup; int sourceIndex; int
		 * destinationIndex; //Route from Ingress to Starting Node if(j ==
		 * omSol.get(i).getChainOrder().size()){
		 * System.out.println("Is Ingress Path"); sourceIndex = f.getSource();
		 * destinationIndex = omSol.get(i).getChainOrder().get(0).getSource(); tup =
		 * new Tuple(sourceIndex,destinationIndex); }else{ if(j ==
		 * omSol.get(i).getChainOrder().size()+1){ sourceIndex =
		 * omSol.get(i).getChainOrder().get(omSol.get(i).getChainOrder().size()-1).
		 * getDestination(); destinationIndex = f.getDestination(); tup = new
		 * Tuple(sourceIndex,destinationIndex); } else{ tup =
		 * omSol.get(i).getChainOrder().get(j); sourceIndex =
		 * omSol.get(i).getNodeMapping(getIndexNF(f, tup.getSource()));
		 * destinationIndex =
		 * omSol.get(i).getNodeMapping(getIndexNF(f,tup.getDestination())); }}
		 * if(omSol.get(i).linkMapping.containsKey(tup)) continue; else{
		 * ArrayList<Tuple> path = djst.getPath(sourceIndex, destinationIndex,
		 * f.getBw()); if(path == null || path.size() == 0){
		 * omCosts.add(i,Integer.MAX_VALUE); break; } else{ capacity =
		 * updateNodeCap(path, capacity, f.getBw(), false); // Increment Capacity
		 * Matrix omSol.get(i).linkMapping.put(tup, path); cost += path.size();
		 * omCosts.add(i,cost); } } } //Reset Node Capacity Matrix Iterator it =
		 * omSol.get(i).linkMapping.entrySet().iterator(); while (it.hasNext()) {
		 * Map.Entry pair = (Map.Entry)it.next();
		 * //if(omSol.get(i).linkMapping.containsKey(pair.getKey())) capacity =
		 * updateNodeCap((ArrayList<Tuple>)pair.getValue(), capacity, f.getBw(),
		 * true); } }
		 */

		// Step 5 - Return Lowest Cost Embedding Solution
		int minIndex = -1;
		int minCost = Integer.MAX_VALUE;
		for (int i = 0; i < omSol.size(); i++) {
			// System.out.println("Overlay Mapping Solution " + i);
			// System.out.println(omSol.get(i));
			// System.out.println("Cost = " + omCosts.get(i));
			if (omCosts.get(i) < minCost) {
				minCost = omCosts.get(i);
				minIndex = i;
			}
		}

		if (minIndex != -1) {
			long endTime = System.currentTimeMillis();
			System.out.println("Lowest Cost Sol is Sol " + minIndex);
			System.out.println(omSol.get(minIndex));
			BufferedWriter costWriter = new BufferedWriter(
			    new FileWriter(new File(logPrefix + ".cost"), true));
			BufferedWriter nodePlacementWriter = new BufferedWriter(
			    new FileWriter(new File(logPrefix + ".nmap"), true));
			BufferedWriter linkPlacementWriter = new BufferedWriter(
			    new FileWriter(new File(logPrefix + ".path"), true));
			BufferedWriter linkSelectionWriter = new BufferedWriter(
			    new FileWriter(new File(logPrefix + ".sequence"), true));
			BufferedWriter durationWriter = new BufferedWriter(
			    new FileWriter(new File(logPrefix + ".time"), true));

			// Write cost.
			costWriter
			    .append(flowIdx + "," + omSol.get(minIndex).getMappingCost() + "\n");
			costWriter.flush();
			costWriter.close();

			// Write duration.
			durationWriter.append(flowIdx + "," + (endTime - startTime) + "\n");
			durationWriter.flush();
			durationWriter.close();

			// Write the sequence that has been embedded and the node mapping in that
			// sequence.
			ArrayList<Tuple> selectedLinks = new ArrayList<Tuple>();
			for (Tuple tLink : omSol.get(minIndex).getChainOrder()) {
				int u = getIndexNF(f, tLink.getSource());
				int v = getIndexNF(f, tLink.getDestination());
				selectedLinks.add(new Tuple(u, v));
			}
			ArrayList<Integer> embeddedSequence = ComputePath(selectedLinks,
			    f.getChain().size());
			linkSelectionWriter.append(Integer.toString(flowIdx));
			nodePlacementWriter.append(Integer.toString(flowIdx));
			for (int i = 0; i < embeddedSequence.size(); ++i) {
				linkSelectionWriter.append("," + embeddedSequence.get(i));
				nodePlacementWriter.append(
				    "," + omSol.get(minIndex).getNodeMapping(embeddedSequence.get(i)));
			}
			linkSelectionWriter.append("\n");
			linkSelectionWriter.flush();
			linkSelectionWriter.close();
			nodePlacementWriter.append("\n");
			nodePlacementWriter.flush();
			nodePlacementWriter.close();
			return omSol.get(minIndex);
		} else
			System.out.println("No Solution Found!");
		return null;
	}

	// This function computes the eulerian path from a set of links. This function
	// can be leveraged to figure out what chain has been embedded and also what
	// is the embedding path of a chain.
	ArrayList<Integer> ComputePath(ArrayList<Tuple> links, int numTotalNodes) {
		ArrayList<ArrayList<Integer>> adj = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> path = new ArrayList<Integer>();
		HashMap<Integer, Integer> indegree = new HashMap<Integer, Integer>();
		if (links == null || links.size() <= 0)
			return path;
		// System.out.println("numTotalNodes = " + numTotalNodes);
		// System.out.println(links);
		for (int i = 0; i < numTotalNodes; ++i)
			adj.add(new ArrayList<Integer>());
		for (int i = 0; i < links.size(); ++i) {
			Tuple link = links.get(i);
			adj.get(link.getSource()).add(link.getDestination());
			if (!indegree.containsKey(link.getDestination()))
				indegree.put(link.getDestination(), 0);
			if (!indegree.containsKey(link.getSource()))
				indegree.put(link.getSource(), 0);
			int prevInDeg = indegree.get(link.getDestination());
			indegree.put(link.getDestination(), prevInDeg + 1);
		}
		int source = -1;
		for (Integer key : indegree.keySet()) {
			if (indegree.get(key) == 0) {
				source = key;
				break;
			}
		}

		if (source == -1)
			return path;

		Stack<Integer> s = new Stack<Integer>();
		int currentNode = source;
		while (true) {
			if (adj.get(currentNode).isEmpty()) {
				path.add(currentNode);
				if (!s.empty()) {
					currentNode = s.pop();
				} else
					break;
			} else {
				s.push(currentNode);
				int neighbor = adj.get(currentNode)
				    .get(adj.get(currentNode).size() - 1);
				adj.get(currentNode).remove(adj.get(currentNode).size() - 1);
				currentNode = neighbor;
			}
		}
		Collections.reverse(path);
		return path;
	}

	public OverlayMapping getOverlaySolution(Graph G, ArrayList<Tuple> chain,
	    int[] mbSpecs, ArrayList<Tuple> candidateNodes, long[][] capacity) {
		OverlayMapping om = performNodeEmbedding(G, chain, mbSpecs, candidateNodes);
		if (om != null) {
			return performLinkEmbedding(om, G, capacity);
		}
		return null;
	}

	public OverlayMapping performNodeEmbedding(Graph G, ArrayList<Tuple> chain,
	    int[] mbSpecs, ArrayList<Tuple> candidateNodes) {

		// Initialize Nodes Cap & Internal Switching Cap
		int[] nodeCaps = new int[candidateNodes.size()];
		int[] internalSwitchingNodeCap = new int[candidateNodes.size()];

		for (int e = 0; e < nodeCaps.length; e++) {
			nodeCaps[e] = G.getNodeCap()[candidateNodes.get(e).getSource()];
			internalSwitchingNodeCap[e] = G.getInterNodeSwitchingCap()[candidateNodes
			    .get(e).getSource()];
		}

		// Initialize a new Overlay Mapping Solution
		OverlayMapping om = new OverlayMapping(f.getChain().size());

		for (int j = 0; j < candidateNodes.size(); j++) {
			int serverIndex = candidateNodes.get(j).getSource();
			System.out.println("Candidate Server " + serverIndex);

			HashMap<Integer, ArrayList<Integer>> OCs = new HashMap<Integer, ArrayList<Integer>>();
			HashMap<Integer, ArrayList<Tuple>> intSwitchedLinks = new HashMap<Integer, ArrayList<Tuple>>();

			for (int t = 0; t <= chain.size(); t++) { // For every NF in the chain

				int nodeCap = nodeCaps[j];
				int internalSwitchingCap = internalSwitchingNodeCap[j];
				int NFType = -1;
				if (t == chain.size())
					NFType = chain.get(t - 1).getDestination();
				else
					NFType = chain.get(t).getSource();

				if (om.nodeMapping[getIndexNF(f, NFType)] != -1)
					continue;

				if (nodeCap < (mbSpecs[NFType]))
					continue;

				nodeCap -= mbSpecs[NFType];
				OCs.put(NFType, new ArrayList<Integer>()); // Initialize a new tuple
				intSwitchedLinks.put(NFType, new ArrayList<Tuple>());
				OCs.get(NFType).add(NFType);

				for (int k = t; k < chain.size(); k++) { // Loop through the remaining
				                                         // tuples

					if (om.nodeMapping[getIndexNF(f, chain.get(k).getSource())] != -1)
						continue;

					if (om.nodeMapping[getIndexNF(f,
					    chain.get(k).getDestination())] != -1)
						continue;

					if (!OCs.get(NFType).contains(chain.get(k).getSource())) {

						if (nodeCap >= (mbSpecs[chain.get(k).getSource()]
						    + mbSpecs[chain.get(k).getDestination()])) {
							if (internalSwitchingCap >= f.getBw()) {
								nodeCap -= (mbSpecs[chain.get(k).getSource()]
								    + mbSpecs[chain.get(k).getDestination()]);
								internalSwitchingCap -= f.getBw();
								// If sufficient cap to accommodate tuple; add tuple to the list
								OCs.get(NFType).add(chain.get(k).getSource());
								OCs.get(NFType).add(chain.get(k).getDestination());
								intSwitchedLinks.get(NFType).add(chain.get(k));
							}
						}
					} else {
						if (nodeCap >= mbSpecs[chain.get(k).getDestination()]) {
							if (internalSwitchingCap >= f.getBw()) {
								nodeCap -= mbSpecs[chain.get(k).getDestination()];
								internalSwitchingCap -= f.getBw();
								// If sufficient cap to accommodate tuple; add tuple to the list
								OCs.get(NFType).add(chain.get(k).getDestination());
								intSwitchedLinks.get(NFType).add(chain.get(k));
							}
						}
					}
				}
			}
			// Find the best placement
			int index = -1;
			int maxInterLinks = -1;
			Iterator it = OCs.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry) it.next();
				System.out.println("OC NF " + pair.getKey() + ": " + pair.getValue());
				if (((ArrayList<Tuple>) pair.getValue()).size() > maxInterLinks) {
					maxInterLinks = ((ArrayList<Tuple>) pair.getValue()).size();
					index = (int) pair.getKey();
				}
			}

			System.out.println("NF Type chosen = " + index);

			// Place best NF set on this server node
			for (int k = 0; k < OCs.get(index).size(); k++) {
				System.out.println(
				    "Placed NF " + OCs.get(index).get(k) + " on " + serverIndex);
				om.nodeMapping[getIndexNF(f, OCs.get(index).get(k))] = serverIndex;
				nodeCaps[j] -= mbSpecs[OCs.get(index).get(k)];
			}

			for (int k = 0; k < intSwitchedLinks.get(index).size(); k++) {
				System.out.println("Internally Switched V. Links "
				    + intSwitchedLinks.get(index).get(k));
				om.linkMapping.put(intSwitchedLinks.get(index).get(k),
				    new ArrayList<Tuple>());
				internalSwitchingNodeCap[j] -= f.getBw();
			}

			if (om.numNodesSettled() == f.getChain().size()) {
				om.setChainOrder(chain);
				System.out.println("Node Embedding for Chain " + chain + "\n" + om);
				return om;
			}
		}
		return null;
	}

	public OverlayMapping performLinkEmbedding(OverlayMapping omSol, Graph G,
	    long[][] capacity) {

		System.out.println("New Execution");
		boolean infeasibleLinkEmbedding = false;
		for (int j = 0; j < omSol.getChainOrder().size() + 2; j++) {

			// For Testing
			// System.out.println("Network Capacity");
			// for (int i = 0; i < capacity.length; i++) {
			// for (int k = 0; k < capacity[i].length; k++) {
			// System.out.print(capacity[i][k] + ",");
			// }
			// System.out.println();
			// }
			// System.out.println();

			// System.out.println(j + "-" + omSol.getChainOrder().size());
			Dijkstra djst = new Dijkstra(G, capacity);
			Tuple tup;
			int sourceIndex;
			int destinationIndex;
			// Route from Ingress to Starting Node
			if (j == omSol.getChainOrder().size()) {
				// System.out.println("Routing Ingress Path...." + "Starting NF Type ="
				//     + omSol.getChainOrder().get(0).getSource());
				sourceIndex = f.getSource();
				destinationIndex = omSol.getNodeMapping(
				    getIndexNF(f, omSol.getChainOrder().get(0).getSource()));
				tup = new Tuple(sourceIndex, destinationIndex);
			} else {
				if (j == omSol.getChainOrder().size() + 1) {
					// System.out.println("Routing Egress Path...." + " Last NF Type ="
					//     + omSol.getChainOrder().get(omSol.getChainOrder().size() - 1)
					//         .getDestination());
					sourceIndex = omSol.getNodeMapping(getIndexNF(f, omSol.getChainOrder()
					    .get(omSol.getChainOrder().size() - 1).getDestination()));
					destinationIndex = f.getDestination();
					tup = new Tuple(sourceIndex, destinationIndex);
				} else {
					tup = omSol.getChainOrder().get(j);
					sourceIndex = omSol.getNodeMapping(getIndexNF(f, tup.getSource()));
					destinationIndex = omSol
					    .getNodeMapping(getIndexNF(f, tup.getDestination()));
				}
			}
			if (omSol.linkMapping.containsKey(tup))
				continue;
			else {
				ArrayList<Tuple> path = djst.getPath(sourceIndex, destinationIndex,
				    f.getBw());
				if ((path == null)
				    || (path.isEmpty() && sourceIndex != destinationIndex)) {
					System.out.println("Could not route tuple " + tup);
					infeasibleLinkEmbedding = true;
					break;
				} else {
					// System.out.println(tup + " routed via " + path);
					capacity = updateNodeCap(path, capacity, f.getBw(), false); // Increment
					                                                            // Capacity
					                                                            // Matrix
					omSol.linkMapping.put(tup, path);
				}
			}
		}

		// Reset Node Capacity Matrix
		Iterator it = omSol.linkMapping.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			// if(omSol.get(i).linkMapping.containsKey(pair.getKey()))
			capacity = updateNodeCap((ArrayList<Tuple>) pair.getValue(), capacity,
			    f.getBw(), true);
		}
		if (infeasibleLinkEmbedding)
			return null;
		// System.out.println("After Link Embedding " + omSol);
		return omSol;
	}

	public static long[][] updateNodeCap(ArrayList<Tuple> path, long[][] capacity,
	    int bw, boolean incerement) {
		for (int i = 0; i < path.size(); i++) {
			if (incerement) {
				capacity[path.get(i).getSource()][path.get(i).getDestination()] += bw;
				capacity[path.get(i).getDestination()][path.get(i).getSource()] += bw;
			} else {
				capacity[path.get(i).getSource()][path.get(i).getDestination()] -= bw;
				capacity[path.get(i).getDestination()][path.get(i).getSource()] -= bw;
			}
		}
		return capacity;
	}

	// This function generates all valid chains
	public void getValidChains(ArrayList<Tuple> E, int[][] M, Flow f) {

		// Create adjacency lists of Es
		for (int i = 0; i < f.getChain().size(); i++) {
			adjacencyList[i] = new ArrayList<Tuple>();
			for (int j = 0; j < E.size(); j++) {
				if (E.get(j).getSource() == f.getChain().get(i))
					adjacencyList[i].add(E.get(j));
			}
		}
		for (int i = 0; i < f.getChain().size(); i++)
			createChain(new boolean[f.getChain().size()], i, new ArrayList<Tuple>());
		//System.out.println("List of Valid Chains is: ");
//		int cntr = 1;
//		for (int i = 0; i < validChains.size(); i++) {
//			System.out
//			    .println("Chain " + cntr + "- " + (validChains.get(i)).toString());
//			cntr++;
//		}
	}

	// visited has mb Typles, currentNode is NF index, chain has NF types
	public void createChain(boolean[] visited, int currentNode,
	    ArrayList<Tuple> chain) {

		// System.out.println("Current NF Type = "+f.getChain().get(currentNode));
		if (chain.size() == adjacencyList.length - 1) {
			// System.out.println("Found a chain: "+chain.toString());
			validChains.add((ArrayList<Tuple>) chain.clone());
			return;
		}

		if (chain.size() == 0)
			visited[currentNode] = true; // This is type

		for (int i = 0; i < adjacencyList[currentNode].size(); i++) {
			int destination = adjacencyList[currentNode].get(i).getDestination(); // This
			                                                                      // is
			                                                                      // type
			int dstIndex = getIndexNF(f, destination);
			// System.out.println("Destination Type "+destination);
			if (visited[dstIndex]) {
				// System.out.println("Already Visited...skip");
				continue;
			}
			boolean skip = false;
			for (int j = 0; j < chain.size(); j++) {
				if (Omega[getIndexNF(f, chain.get(j).getSource())][dstIndex] == 0) {
					skip = true;
					break;
				}
			}
			if (skip)
				continue;

			// System.out.println("Add to chain tuple
			// ("+f.getChain().get(currentNode)+","+destination+")");
			chain.add(adjacencyList[currentNode].get(i));// This is type
			visited[dstIndex] = true; // This is type
			createChain(visited, dstIndex, chain);
			chain.remove(chain.size() - 1);

			visited[dstIndex] = false;
		}

		return;
	}

	public int getIndexNF(Flow f, int type) {
		int index = -1;
		for (int i = 0; i < f.getChain().size(); i++) {
			if (f.getChain().get(i) == type)
				index = i;
		}
		return index;
	}

	public int getVLinkIndex(int srcType, int dstType, ArrayList<Tuple> E) {
		for (int i = 0; i < E.size(); i++) {
			if (E.get(i).getSource() == srcType
			    && E.get(i).getDestination() == dstType)
				return i;
		}
		return -1;
	}
}
