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
	
	public void executeHeuristic(Graph G, int[][] M, Flow f, ArrayList<Tuple> E, int[] mbSpecs){
		
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
			System.out.println(E.get(i).getSource()+","+E.get(i).getDestination());
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
			System.out.println("Node "+i);
			int di = 0;
			int de = 0;
			if(G.getNodeCap()[i] < minDemand)
				continue;
			
			if(i != f.getSource()){
				ArrayList<Tuple> pathToIng = djst.getPath(ingress, i, f.getBw());
				di = pathToIng.size();
				System.out.println("di_"+i+"="+di);
			}
			
			if(i != f.getDestination()){
				ArrayList<Tuple> pathToEgr = djst.getPath(egress, i, f.getBw());
				de = pathToEgr.size();
				System.out.println("de_"+i+"="+de);
			}		
			System.out.println(" Value = "+G.getNodeCap()[i]/(di+de));
			candidateNodes.add(new Tuple(i, G.getNodeCap()[i]/(di+de)));
		}
	
		
		//Step 3 - Sort Physical Servers
		Collections.sort(candidateNodes);
		System.out.println("Candidate Nodes");
		System.out.println(candidateNodes);
			
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
		
		System.out.println("Curent NF Type = "+f.getChain().get(currentNode));
		if(chain.size() == adjacencyList.length-1){
			System.out.println("Found a chain: "+chain.toString());
			validChains.add((ArrayList<Tuple>)chain.clone());
			return;
		}
		
		if(chain.size() == 0)
			visited[currentNode] = true; //This is type
		
	
		a: for(int i=0;i<adjacencyList[currentNode].size();i++){
			int destination = adjacencyList[currentNode].get(i).getDestination(); // This is type
			int dstIndex = getIndexNF(f,destination);
			System.out.println("Destination Type "+destination);
			if(visited[dstIndex]){
				System.out.println("Already Visited...skip");
				continue;
			}
			for(int j=0;j<chain.size();j++){
				if(Omega[getIndexNF(f, chain.get(j).getSource())][dstIndex]==0)
					continue a;
			}
			
			System.out.println("Add to chain tuple ("+f.getChain().get(currentNode)+","+destination+")");
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

