import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Heuristic {

	
	ArrayList<ArrayList<Tuple>> validChains;
	ArrayList<Tuple>[] adjacencyList;
	Flow f;
	int[][] Omega;
	
	public Heuristic(){
		validChains = new ArrayList<ArrayList<Tuple>>();
	}
	
	public OverlayMapping executeHeuristic(Graph G, int[][] M, Flow f, ArrayList<Tuple> E, int[] mbSpecs){
		
		this.f = f;
		adjacencyList = new ArrayList[f.getChain().size()];
		
		//Step 0 - Generate Omega
		//Omega is populated by NF index and not NF type
		Omega = new int[f.getChain().size()][f.getChain().size()]; //Omega = C || !C ^ M ^ E
		for(int i=0;i<f.getChain().size()-1;i++){
			for(int j=i+1;j<f.getChain().size();j++)
				Omega[i][j] = 1;
		}
		for(int i=0;i<E.size();i++){
			//System.out.println(E.get(i).getSource()+","+E.get(i).getDestination());
			if((M[E.get(i).getSource()][E.get(i).getDestination()]) == 1) 
				Omega[getIndexNF(f, E.get(i).getSource())][getIndexNF(f, E.get(i).getDestination())] = 1;
		}
		
		//Step 1 - Generate Valid Chains
		 getValidChains(E, M, f);
				
		//Step 2 - Find shortest path to ingress & egress node
		 
		 int[][] capacity = new int[G.getNodeCount()][G.getNodeCount()];
		 for(int i=0;i<G.getAdjList().size();i++){
			 for(int j=0;j<G.getAllEndPoints(i).size();j++)
				 capacity[i][G.getAllEndPoints(i).get(j).getNodeId()] = G.getAllEndPoints(i).get(j).getBw();
		 }
		Dijkstra djst = new Dijkstra(G,capacity);
		
		//Get Min MB Demand in the chain
		int minDemand = Integer.MAX_VALUE;
		for(int i=0;i<f.getChain().size();i++){
			if(mbSpecs[f.getChain().get(i)] < minDemand)
				minDemand = mbSpecs[f.getChain().get(i)];
		}
		
		//Candidate Node Index, Cost
		ArrayList<Tuple> candidateNodes = new ArrayList<Tuple>();
		HashMap<Tuple, ArrayList<Tuple>> paths = new HashMap<Tuple, ArrayList<Tuple>>();
		int ingress= f.getSource();
		int egress = f.getDestination();
		for(int i=0;i<G.getNodeCount();i++){
		//	System.out.println("Node "+i);
			int di = 0;
			int de = 0;
			//Skip Node if it doesn't have enough node resources to support any NF
			if(G.getNodeCap()[i] < minDemand)
				continue;

			if(i != f.getSource()){
				ArrayList<Tuple> pathToIng = djst.getPath(ingress, i, f.getBw());
				di = pathToIng.size();
			//	System.out.println("di_"+i+"="+di);
			}
			
			if(i != f.getDestination()){
				ArrayList<Tuple> pathToEgr = djst.getPath(egress, i, f.getBw());
				de = pathToEgr.size();
				//System.out.println("de_"+i+"="+de);
			}		
			//System.out.println(" Value = "+G.getNodeCap()[i]/(di+de));
			double oc = (double)G.getNodeCap()[i]/(double)(di+de);
			candidateNodes.add(new Tuple(i, (int)(oc*10e4)));
		}
	
		
		//Step 3 - Sort Physical Servers
		Collections.sort(candidateNodes);
		System.out.println("Candidate Nodes");
		System.out.println(candidateNodes);

		int[] nodeCaps = new int[candidateNodes.size()];
		int[] internalSwitchingNodeCap = new int[candidateNodes.size()];
		
		ArrayList<OverlayMapping> omSol = new ArrayList<OverlayMapping>();
		for(int i=0;i<validChains.size();i++){
			//System.out.println("For Valid Chain "+validChains.get(i));
			
			//Initialize Nodes Cap & Internal Switching Cap
			for(int e=0;e<nodeCaps.length;e++){
				nodeCaps[e] = G.getNodeCap()[candidateNodes.get(e).getSource()];
				internalSwitchingNodeCap[e] = G.getInterNodeSwitchingCap()[candidateNodes.get(e).getSource()];
			}
		
			//Initialize a new Overlay Mapping Solution
			OverlayMapping om = new OverlayMapping(f.getChain().size());
			
			for(int j=0;j<candidateNodes.size();j++){
				int serverIndex = candidateNodes.get(j).getSource();
				//System.out.println("Candidate Server "+serverIndex);
				
				HashMap<Integer,ArrayList<Integer>> OCs = new HashMap<Integer,ArrayList<Integer>>();
				HashMap<Integer,ArrayList<Tuple>> intSwitchedLinks = new HashMap<Integer,ArrayList<Tuple>>();

				for(int t=0;t<=validChains.get(i).size();t++){ //For every NF in the chain
					
					int nodeCap = nodeCaps[j];
					int internalSwitchingCap = internalSwitchingNodeCap[j];
					int NFType = -1;
					if(t == validChains.get(i).size())
						NFType = validChains.get(i).get(t-1).getDestination();
					else
						NFType = validChains.get(i).get(t).getSource();
					
					if(om.nodeMapping[getIndexNF(f, NFType)] != -1)
						continue;				
								
					if(nodeCap < (mbSpecs[NFType]))
						continue;
					
					nodeCap -=	mbSpecs[NFType];
					OCs.put(NFType, new ArrayList<Integer>()); // Initialize a new tuple
					intSwitchedLinks.put(NFType, new ArrayList<Tuple>());
					OCs.get(NFType).add(NFType);
					
					for(int k=t;k<validChains.get(i).size();k++){ //Loop through the remaining tuples
						
						if(om.nodeMapping[getIndexNF(f,validChains.get(i).get(k).getSource())] != -1)
							continue;
						
						if(om.nodeMapping[getIndexNF(f,validChains.get(i).get(k).getDestination())] != -1)
							continue;
						
						if(!OCs.get(NFType).contains(validChains.get(i).get(k).getSource())){
							
							if(nodeCap >= (mbSpecs[validChains.get(i).get(k).getSource()]+
										  mbSpecs[validChains.get(i).get(k).getDestination()])){
								if(internalSwitchingCap >= f.getBw()){
									nodeCap-= (mbSpecs[validChains.get(i).get(k).getSource()]+
										  mbSpecs[validChains.get(i).get(k).getDestination()]);
									internalSwitchingCap -= f.getBw();
									//If sufficient cap to accommodate tuple; add tuple to the list
									OCs.get(NFType).add(validChains.get(i).get(k).getSource());	
									OCs.get(NFType).add(validChains.get(i).get(k).getDestination());	
									intSwitchedLinks.get(NFType).add(validChains.get(i).get(k));
								}
							}
							}else{
							if(nodeCap >=  mbSpecs[validChains.get(i).get(k).getDestination()]){
								if(internalSwitchingCap >= f.getBw()){
									nodeCap-= mbSpecs[validChains.get(i).get(k).getDestination()];
									internalSwitchingCap -= f.getBw();
									//If sufficient cap to accommodate tuple; add tuple to the list
									OCs.get(NFType).add(validChains.get(i).get(k).getDestination());	
									intSwitchedLinks.get(NFType).add(validChains.get(i).get(k));
								}
							}
						}
					}
				}
				//Find the best placement
				int index = -1;
				int maxInterLinks = -1;
				Iterator it = OCs.entrySet().iterator();
				 while (it.hasNext()) {
				      Map.Entry pair = (Map.Entry)it.next();
				  //    System.out.println("OC NF "+pair.getKey()+": "+pair.getValue());
				   		if(((ArrayList<Tuple>)pair.getValue()).size() > maxInterLinks){
				   			maxInterLinks = ((ArrayList<Tuple>)pair.getValue()).size();
				   			index = (int)pair.getKey();
					}
				}
				
				 //System.out.println("NF Type chosen = "+index);
				 
				//Place best NF set on this server node
				for(int k=0; k< OCs.get(index).size();k++){
					//System.out.println("Placed NF "+OCs.get(index).get(k)+" on "+serverIndex);
					om.nodeMapping[getIndexNF(f, OCs.get(index).get(k))] = serverIndex;
					nodeCaps[j] -= mbSpecs[OCs.get(index).get(k)];
				}
				
				for(int k=0; k< intSwitchedLinks.get(index).size();k++){
					//System.out.println("Internally Switched V. Links "+intSwitchedLinks.get(index).get(k));
					om.linkMapping.put(intSwitchedLinks.get(index).get(k), new ArrayList<Tuple>());
					internalSwitchingNodeCap[j] -= f.getBw();
				}
				
				if(om.numNodesSettled() == f.getChain().size()){
					om.setChainOrder(validChains.get(i));
					omSol.add(om);
					break;
				}
			}
			//System.out.println(om);			
		}
		
		//Step 4 - For each OverlayMapping Solution perform Link Embedding
		ArrayList<Integer> omCosts = new ArrayList<Integer>();
		for(int i=0; i<omSol.size(); i++){
			int cost = 0;
			
			for(int j=0;j<omSol.get(i).getChainOrder().size();j++){
				 djst = new Dijkstra(G,capacity);
				Tuple tup = omSol.get(i).getChainOrder().get(j);
				if(omSol.get(i).linkMapping.containsKey(tup))
					continue;
				else{
					ArrayList<Tuple> path = djst.getPath(omSol.get(i).getNodeMapping(getIndexNF(f, tup.getSource())),
							omSol.get(i).getNodeMapping(getIndexNF(f,tup.getDestination())), f.getBw());
					if(path == null || path.size() == 0){
						omCosts.add(i,Integer.MAX_VALUE);
						break;
					}
					else{
						capacity = updateNodeCap(path, capacity, f.getBw(), false); // Incerement Capacity Matrix
						omSol.get(i).linkMapping.put(tup, path);
						cost += path.size();
						omCosts.add(i,cost);
					}
				}
			}
			//Reset Node Capacity Matrix
			for(int j=0; j<omSol.get(i).getChainOrder().size();j++){
				if(omSol.get(i).linkMapping.containsKey(omSol.get(i).getChainOrder().get(j)))
					capacity = updateNodeCap(omSol.get(i).getLinkMapping(omSol.get(i).getChainOrder().get(j))
						, capacity, f.getBw(), true);
			}
		}
		
		//Step 5 - Return Lowest Cost Embedding Solution	
		int minIndex = -1;
		int minCost = Integer.MAX_VALUE;
		for(int i=0;i<omSol.size();i++){
			System.out.println("Overlay Mapping Solution "+i);
			System.out.println(omSol.get(i));
			System.out.println("Cost = "+omCosts.get(i));
			if(omCosts.get(i) < minCost){
				minCost = omCosts.get(i);
				minIndex = i;
			}
		}
		
		if(minIndex != -1){
			System.out.println("Lowest Cost Sol is Sol "+minIndex);
			return omSol.get(minIndex);
		}
		
		return null;
	}
	
	public static int[][] updateNodeCap(ArrayList<Tuple> path, int[][] capacity, int bw, boolean incerement){
		for(int i=0;i<path.size();i++){
			if(incerement){
				capacity[path.get(i).getSource()][path.get(i).getDestination()] += bw; 
				capacity[path.get(i).getDestination()][path.get(i).getSource()] += bw; 
			}
			else{
				capacity[path.get(i).getSource()][path.get(i).getDestination()] -= bw; 
				capacity[path.get(i).getDestination()][path.get(i).getSource()] -= bw; 
			}
		}
		return capacity;
	}
	
	
	//This function generates all valid chains
	public void getValidChains(ArrayList<Tuple> E, int[][] M, Flow f){
	
		//Create adjacency lists of Es
		for(int i=0;i<f.getChain().size();i++){
			adjacencyList[i] = new ArrayList<Tuple>();
			for(int j=0;j<E.size();j++){
				if(E.get(j).getSource() == f.getChain().get(i))
					adjacencyList[i].add(E.get(j));
			}
		}
		for(int i=0;i<f.getChain().size();i++)			
			createChain(new boolean[f.getChain().size()], i, new ArrayList<Tuple>());
				
		System.out.println("List of Valid Chains is: ");
		int cntr = 1;
		for(int i=0;i<validChains.size();i++){
			System.out.println("Chain "+cntr+"- "+(validChains.get(i)).toString());
			cntr++;
		}

	}

	//visited has mb Typles, currentNode is NF index, chain has NF types
	public void createChain(boolean[] visited, int currentNode, ArrayList<Tuple> chain){
		
		//System.out.println("Current NF Type = "+f.getChain().get(currentNode));
		if(chain.size() == adjacencyList.length-1){
			//System.out.println("Found a chain: "+chain.toString());
			validChains.add((ArrayList<Tuple>)chain.clone());
			return;
		}
		
		if(chain.size() == 0)
			visited[currentNode] = true; //This is type
		
	
		for(int i=0;i<adjacencyList[currentNode].size();i++){
			int destination = adjacencyList[currentNode].get(i).getDestination(); // This is type
			int dstIndex = getIndexNF(f,destination);
			//System.out.println("Destination Type "+destination);
			if(visited[dstIndex]){
				//System.out.println("Already Visited...skip");
				continue;
			}
			boolean skip = false;
			for(int j=0;j<chain.size();j++){
				if(Omega[getIndexNF(f, chain.get(j).getSource())][dstIndex]==0){
					skip = true;
					break;
				}
			}
			if(skip)
				continue;
			
			//System.out.println("Add to chain tuple ("+f.getChain().get(currentNode)+","+destination+")");
			chain.add(adjacencyList[currentNode].get(i));//This is type
			visited[dstIndex] = true; //This is type
			createChain(visited, dstIndex, chain);	
			chain.remove(chain.size()-1);
			
			visited[dstIndex] = false; 
		}
		
		return;
	}
	
	public int getIndexNF(Flow f , int type){
		int index = -1;
		for(int i=0;i<f.getChain().size();i++){
			if(f.getChain().get(i)== type)
				index = i;
		}
		return index;
	}
}

