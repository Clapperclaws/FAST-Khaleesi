import java.util.ArrayList;

public class Flow {
	
	
	private int source;
	private int destination;
	private int bw;
	private ArrayList<Integer> chain;
	
	public Flow(int src, int dst, int bw){
		this.source = src;
		this.destination = dst;
		this.bw = bw;
		chain = new ArrayList<Integer>();
	}

	public int getSource() {
		return source;
	}

	public void setSource(int source) {
		this.source = source;
	}

	public int getDestination() {
		return destination;
	}

	public void setDestination(int destination) {
		this.destination = destination;
	}

	public int getBw() {
		return bw;
	}

	public void setBw(int bw) {
		this.bw = bw;
	}

	public ArrayList<Integer> getChain() {
		return chain;
	}

	public void setChain(ArrayList<Integer> chain) {
		this.chain = chain;
	}
	
	public String toString(){
		String content = "Flow between "+source+" and "+destination+" with bw demand = "+bw+"; and chain:\n";
		for(int i=0;i<chain.size();i++){
			content += chain.get(i)+",";
		}
		content+="\n";
		return content;
	}
	
}
