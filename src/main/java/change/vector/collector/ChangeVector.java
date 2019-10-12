package change.vector.collector;

import change.vector.collector.Collector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

class MutableInt {
	int value = 1;
	public void increment() { value++; }
	public int get() { return value; }
}

public class ChangeVector {
	public int matchesNum = 0;
	public int updatesNum = 0;
	public int insertsNum = 0;
	public int movesNum = 0;
	public int deletesNum = 0;
	public Map<String, MutableInt> matches = null;
	public Map<String, MutableInt> deletes = null;
	public Map<String, MutableInt> inserts = null;
	public Map<String, MutableInt> updates = null;
	public Map<String, MutableInt> moves = null;

	public static ArrayList<ChangeVector> runGumtreeDIST() throws IOException {
		ArrayList<ChangeVector> changeVectors = new ArrayList<ChangeVector>();
		
		String gumtree = "./gumtree/bin/gumtree";
		String com =  " jsondiff ";
		String gcom = gumtree + com;
//		String filetest = "./assets/test/El";				
		String file = "/Users/jihoshin/eclipse-workspace/change-vector-collector/assets/collectedFiles/";

		for (int i = 0; i < Collector.instanceNumber; i++) {
			ChangeVector changeVector = new ChangeVector();
			
			Process p = Runtime.getRuntime().exec(gcom + file + i + "_before.java " + file + i + "_bic.java"); 
//			Process p = Runtime.getRuntime().exec(gcom + filetest+ "1.java " + filetest + "2.java"); 
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));  

			// printing the console output.
//			String line = null;			
//			while((line = in.readLine()) != null) {
//				System.out.println(line);
//			}
			
			String str = IOUtils.toString(in);
			JSONObject json = new JSONObject(str);
			JSONArray matchesJSON = json.getJSONArray("matches");
			Map<String, MutableInt> matches = new HashMap<String, MutableInt>();
			changeVector.matchesNum = json.getJSONArray("matches").length();
			
			
			// counting instances of matches
			for (int j = 0; j < matchesJSON.length(); j++){
				JSONObject tmp = matchesJSON.getJSONObject(j);
				String src = tmp.getString("src");
				String tmpSplit[] = src.split("\\s");
				
				MutableInt count = matches.get(tmpSplit[0]);
				if(count == null) {
					matches.put(tmpSplit[0], new MutableInt());
				} else {
					count.increment();
				}
			}
			System.out.println("MATCHES:");
			for(String name: matches.keySet()) {
				System.out.println(name.toString() + " " + matches.get(name).value);
			}
			System.out.println();
			
			
			Map<String, MutableInt> deletes = new HashMap<String, MutableInt>();
			Map<String, MutableInt> inserts = new HashMap<String, MutableInt>();
			Map<String, MutableInt> moves = new HashMap<String, MutableInt>();
			Map<String, MutableInt> updates = new HashMap<String, MutableInt>();
			
			
			// counting instances of actions
			JSONArray actions = json.getJSONArray("actions");
			for (int j = 0; j < actions.length(); j++) {
				JSONObject tmp = actions.getJSONObject(j);

				// deleted nodes
				if(tmp.getString("action").equals("delete-node")) {
					changeVector.deletesNum++;
					String [] tmpSplit = tmp.getString("tree").split("\\s");
			
					MutableInt count = deletes.get(tmpSplit[0]);
					if(count == null) {
						deletes.put(tmpSplit[0], new MutableInt());
					} else {
						count.increment();
					}					
				} 
				// inseted nodes
				else if(tmp.getString("action").equals("insert-node")) {
					changeVector.insertsNum++;
					String [] tmpSplit = tmp.getString("tree").split("\\s");
					
					MutableInt count = inserts.get(tmpSplit[0]);
					if(count == null) {
						inserts.put(tmpSplit[0], new MutableInt());
					} else {
						count.increment();
					}		
				} 
				// moved trees
				else if(tmp.getString("action").equals("move-tree")) {
					changeVector.movesNum++;
					String [] tmpSplit = tmp.getString("tree").split("\\s");
					
					MutableInt count = moves.get(tmpSplit[0]);
					if(count == null) {
						moves.put(tmpSplit[0], new MutableInt());
					} else {
						count.increment();
					}		
				} 
				// updated nodes
				else if(tmp.getString("action").equals("update-node")) {
					changeVector.updatesNum++;
					String [] tmpSplit = tmp.getString("tree").split("\\s");
					
					MutableInt count = updates.get(tmpSplit[0]);
					if(count == null) {
						updates.put(tmpSplit[0], new MutableInt());
					} else {
						count.increment();
					}		
				}
			}
			
			System.out.println("DELETES:");
			for(String name: deletes.keySet()) {
				System.out.println(name.toString() + " " + deletes.get(name).value);
			}
			System.out.println();
			System.out.println("INSERTS:");
			for(String name: inserts.keySet()) {
				System.out.println(name.toString() + " " + inserts.get(name).value);
			}
			System.out.println();
			System.out.println("MOVES:");
			for(String name: moves.keySet()) {
				System.out.println(name.toString() + " " + moves.get(name).value);
			}
			System.out.println();
			System.out.println("UPDATES:");
			for(String name: updates.keySet()) {
				System.out.println(name.toString() + " " + updates.get(name).value);
			}
			System.out.println();
			
			System.out.println(i+"/"+Collector.instanceNumber);
			System.out.println("matchesNum: " + changeVector.matchesNum);
			System.out.println("insertsNum: " + changeVector.insertsNum);
			System.out.println("deletesNum: " + changeVector.deletesNum);
			System.out.println("updateNum: " + changeVector.updatesNum);
			System.out.println("movesNum: " + changeVector.movesNum);
			System.out.println();
			
			changeVector.matches = matches;
			changeVector.deletes = deletes;
			changeVector.inserts = inserts;
			changeVector.updates = updates;
			changeVector.moves = moves;
		
			changeVectors.add(changeVector);
		}
	
		
		
		
		
		// saving the results in CSV format
		final String[] headers = {"matches", "deletes", "inserts", "moves", "updates"};
		File fileP = new File("/Users/jihoshin/eclipse-workspace/change-vector-collector/assets/changeVectors.csv");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileP.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers));
		
		for(ChangeVector cv : changeVectors) {
			csvprinter.printRecord(cv.matchesNum, cv.deletesNum, cv.insertsNum, cv.movesNum, cv.updatesNum);
			csvprinter.flush();
		}
		System.out.println("$$$$$$ Change vector extraction complete!!");
		csvprinter.close();
		return changeVectors;
	}
	
	
}
	