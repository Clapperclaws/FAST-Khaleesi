import java.util.ArrayList;
import java.util.Arrays;

public class Heuristic {

	
	ArrayList<Tuple[]> validChains;
	
	public Heuristic(){
		validChains = new ArrayList<Tuple[]>();
	}
	
	public void executeHeuristic(Graph G, int[][] M, Flow f, ArrayList<Tuple> E, int[] mbSpecs){
		
		//Step 1 - Generate Valid Chains
		 getValidChains(E, M, f);
		
		
		
	}
	
	//This function generates all valid chains
	public void getValidChains(ArrayList<Tuple> E, int[][] M, Flow f){
	
		//Create adjacency lists of Es
		ArrayList<Tuple>[] adjacencyList = new ArrayList[f.getChain().size()];
		for(int i=0;i<f.getChain().size();i++){
			adjacencyList[i] = new ArrayList<Tuple>();
			for(int j=0;j<E.size();j++){
				if(E.get(j).getSource() == f.getChain().get(i))
					adjacencyList[i].add(E.get(j));
			}
		}
			for(int i=0;i<f.getChain().size();i++){
			
			createChain(new ArrayList<Integer>(), i,
					0, new Tuple[f.getChain().size()-1], f, adjacencyList);
			
		}	
			
		System.out.println("List of Valid Chains is: ");
		int cntr = 1;
		for(int i=0;i<validChains.size();i++){
			System.out.println("Chain "+cntr+"- "+Arrays.toString(validChains.get(i)));
			cntr++;
		}

	}

	//visited has mb Typles, currentNode is NF index, chain has NF types
	public void createChain(ArrayList<Integer> visited, int currentNode, int chainIndex, Tuple[] chain, Flow f, ArrayList<Tuple>[] adjacencyList){
		
		System.out.println("Curent NF Type = "+f.getChain().get(currentNode));
		if(chainIndex == adjacencyList.length-1){
			System.out.println("Found a chain: "+Arrays.toString(chain));
			validChains.add(chain.clone());
			return;
		}
		
		if(chainIndex == 0)
			visited.add(f.getChain().get(currentNode)); //This is type
		
	
		for(int i=0;i<adjacencyList[currentNode].size();i++){
			int destination = adjacencyList[currentNode].get(i).getDestination(); // This is type
			System.out.println("Destination Type "+destination);
			if(visited.contains(destination)){
				System.out.println("Already Visited...skip");
				continue;
			}
			else{
				System.out.println("Add to chain at index "+chainIndex+" tuple ("+f.getChain().get(currentNode)+","+destination+")");
				chain[chainIndex] = adjacencyList[currentNode].get(i);//This is type
				visited.add(destination); //This is type
				createChain(visited, getIndexNF(f,destination), chainIndex+1, chain, f, adjacencyList);	
			}
			visited.remove(visited.size()-1);
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

