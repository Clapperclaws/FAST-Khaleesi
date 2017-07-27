import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.StringTokenizer;

import ilog.concert.IloException;

public class Driver {
	public static String snFile = "";
	public static String flowsFile = "";
	public static String mbSpecFile = "";
	public static String rcmFile = "";

	private static HashMap<String, String> ParseArgs(String[] args) {
		HashMap<String, String> ret = new HashMap<String, String>();
		for (int i = 0; i < args.length; ++i) {
			StringTokenizer tokenizer = new StringTokenizer(args[i], "=");
			ret.put(tokenizer.nextToken(), tokenizer.nextToken());
		}
		return ret;
	}

	public static void main(String[] args) throws IOException, IloException {
		HashMap<String, String> parsedArgs = ParseArgs(args);
		// Read Substrate Network
		Graph substrateNetwork = ReadTopology(parsedArgs.get("--sn_topology_file"),
		    -1);
		// System.out.println("Substrate Network \n" + substrateNetwork);

		// Read List of Flows
		ArrayList<Flow> flowsList = ReadFlows(parsedArgs.get("--flows_file"));
		// System.out.println("List of Flows: \n" + flowsList);

		// Read Middlebox Specs
		int[] mbSpecs = ReadMBSpecs(parsedArgs.get("--mbox_spec_file"));
		// System.out.println("MBs Specs:" + Arrays.toString(mbSpecs));
		String logPrefix = parsedArgs.get("--log_prefix");

		// Read RCM
		int[][] rcm = ReadRCM(parsedArgs.get("--rcm_file"), mbSpecs.length);
		// System.out.print("ReadOrderCompatibilityMatrix \n");
		// for (int i = 0; i < rcm.length; i++) {
		// 	System.out.println(Arrays.toString(rcm[i]));
		// }

		BufferedWriter costWriter = new BufferedWriter(
		    new FileWriter(new File(logPrefix + ".cost")));
		BufferedWriter nodePlacementWriter = new BufferedWriter(
		    new FileWriter(new File(logPrefix + ".nmap")));
		BufferedWriter linkPlacementWriter = new BufferedWriter(
		    new FileWriter(new File(logPrefix + ".path")));
		BufferedWriter linkSelectionWriter = new BufferedWriter(
		    new FileWriter(new File(logPrefix + ".sequence")));
		BufferedWriter durationWriter = new BufferedWriter(
		    new FileWriter(new File(logPrefix + ".time")));
		costWriter.close();
		nodePlacementWriter.close();
		linkPlacementWriter.close();
		linkSelectionWriter.close();
		durationWriter.close();
		
		// int startFlowIndex = 0, endFlowIndex = flowsList.size() - 1;
		int startFlowIndex = 176, endFlowIndex = 176;
		for (int flowIdx = startFlowIndex; flowIdx <= endFlowIndex; ++flowIdx) {
			ArrayList<Tuple> vLinks = generateE(flowsList.get(flowIdx), rcm);
			System.out.println("List of Es \n" + vLinks);
			Heuristic hrst = new Heuristic();
			OverlayMapping oMapping = hrst.executeHeuristic(substrateNetwork, rcm,
					flowIdx, flowsList.get(flowIdx), vLinks, mbSpecs, logPrefix);
		}
	}

	public static ArrayList<Tuple> generateE(Flow f, int[][] M) {

		ArrayList<Tuple> vLinks = new ArrayList<Tuple>();

		// Add Links in the Original Chain
		for (int i = 0; i < f.getChain().size() - 1; i++) {
			Tuple t = new Tuple(f.getChain().get(i), f.getChain().get(i + 1));
			vLinks.add(t);
		}

		for (int i = 0; i < f.getChain().size() - 1; i++) {
			// Get Chain Head
			// System.out.println("Comparing NF "+i);
			for (int j = i + 1; j < f.getChain().size(); j++) {
				// System.out.println("with NF "+j);
				if (isNext(i, i + 1, j, j - 1, f, M)) {
					// System.out.println("NFs "+f.getChain().get(i)+" has next
					// "+f.getChain().get(j));
					if (!contains(vLinks, f.getChain().get(i), f.getChain().get(j)))
						vLinks.add(new Tuple(f.getChain().get(i), f.getChain().get(j)));

				}
				if (isPrev(i, i + 1, j, j - 1, f, M)) {
					// System.out.println("NFs "+f.getChain().get(i)+" has previous
					// "+f.getChain().get(j));
					if (!contains(vLinks, f.getChain().get(j), f.getChain().get(i)))
						vLinks.add(new Tuple(f.getChain().get(j), f.getChain().get(i)));
				}
			}
		}
		return vLinks;
	}

	public static boolean isNext(int i, int x, int j, int y, Flow f, int[][] M) {
		if (f.getChain().get(i) == f.getChain().get(y))
			return true;

		if (f.getChain().get(j) == f.getChain().get(x))
			return true;

		if (M[f.getChain().get(j)][f.getChain().get(y)] == 1)
			return isNext(i, x, j, y - 1, f, M);

		if (M[f.getChain().get(i)][f.getChain().get(x)] == 1)
			return isNext(i, x + 1, j, y, f, M);

		return false;
	}

	public static boolean isPrev(int i, int x, int j, int y, Flow f, int[][] M) {

		if (f.getChain().get(i) == f.getChain().get(y)) {
			if (M[f.getChain().get(i)][f.getChain().get(j)] == 1)
				return true;
			else
				return false;
		}
		if (f.getChain().get(j) == f.getChain().get(x)) {
			if (M[f.getChain().get(i)][f.getChain().get(j)] == 1)
				return true;
			else
				return false;
		}
		if (M[f.getChain().get(j)][f.getChain().get(y)] == 1)
			return isPrev(i, x, j, y - 1, f, M);
		if (M[f.getChain().get(i)][f.getChain().get(x)] == 1)
			return isPrev(i, x + 1, j, y, f, M);

		return false;
	}

	public static boolean contains(ArrayList<Tuple> vLinks, int source,
	    int destination) {
		for (int i = 0; i < vLinks.size(); i++) {
			if ((vLinks.get(i).getSource() == source)
			    && (vLinks.get(i).getDestination() == destination))
				return true;
		}
		return false;
	}

	// This function reads from file
	public static String ReadFromFile(String filename) throws IOException {
		String content = "";
		BufferedReader br = new BufferedReader(new FileReader(filename));
		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();
			while (line != null) {
				content += line + "\n";
				line = br.readLine();
			}
		} finally {
			if (br != null)
				br.close();
		}
		return content;
	}

	/* This function reads Middlebox Specs */
	public static int[] ReadMBSpecs(String filename) throws IOException {

		Scanner scanner = new Scanner(ReadFromFile(filename));
		String line = scanner.nextLine();
		String[] splitLine = line.split(",");

		int[] mbSpecs = new int[Integer.parseInt(splitLine[0])];

		while (scanner.hasNextLine()) {
			line = scanner.nextLine();
			if (line != null) {
				splitLine = line.split(",");
				mbSpecs[Integer.parseInt(splitLine[0])] = Integer
				    .parseInt(splitLine[1]);
			}
		}

		return mbSpecs;
	}

	/* This function reads a list of flows from file */
	public static ArrayList<Flow> ReadFlows(String filename) throws IOException {
		ArrayList<Flow> flowsList = new ArrayList<Flow>();
		Scanner scanner = new Scanner(ReadFromFile(filename));
		while (scanner.hasNextLine()) {

			String line = scanner.nextLine();
			String[] splitLine = line.split(",");

			Flow f = new Flow(Integer.parseInt(splitLine[1]),
			    Integer.parseInt(splitLine[2]), Integer.parseInt(splitLine[3]));
			for (int i = 4; i < splitLine.length; i++) {
				f.getChain().add(Integer.parseInt(splitLine[i]));
			}
			flowsList.add(f);
		}
		return flowsList;
	}

	/* This function reads a graph from file */
	public static Graph ReadTopology(String filename, int nodeType)
	    throws IOException {

		Scanner scanner = new Scanner(ReadFromFile(filename));
		String line = scanner.nextLine();
		String[] splitLine = line.split(",");
		int nodesCntr = Integer.parseInt(splitLine[0]);
		Graph g = new Graph(nodesCntr); // Initialize
		int cntr = 0;
		while (scanner.hasNextLine()) {
			line = scanner.nextLine();
			if (line != null) {
				splitLine = line.split(",");
				// Populate Nodes Specs
				if (cntr < nodesCntr) {
					g.getNodeCap()[Integer.parseInt(splitLine[0])] = Integer
					    .parseInt(splitLine[1]);
					g.getInterNodeSwitchingCap()[Integer.parseInt(splitLine[0])] = Integer
					    .parseInt(splitLine[2]);
					cntr++;
				} else {
					// Populate Links Specs
					EndPoint e1 = new EndPoint(Integer.parseInt(splitLine[2]),
					    Long.parseLong(splitLine[3]), nodeType);
					EndPoint e2 = new EndPoint(Integer.parseInt(splitLine[1]),
					    Long.parseLong(splitLine[3]), nodeType);
					g.getAllEndPoints(Integer.parseInt(splitLine[1])).add(e1);
					g.getAllEndPoints(Integer.parseInt(splitLine[2])).add(e2);
				}
			}
		}
		return g;
	}

	/* This function reads the Re-order Compatibility Matrix */
	public static int[][] ReadRCM(String filename, int numMB) throws IOException {

		int[][] RCM = new int[numMB][numMB];
		Scanner scanner = new Scanner(ReadFromFile(filename));
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			String[] splitLine = line.split(",");
			for (int i = 1; i < splitLine.length; i++) {
				RCM[Integer.parseInt(splitLine[0])][Integer.parseInt(splitLine[i])] = 1;
			}
		}
		return RCM;
	}
}
