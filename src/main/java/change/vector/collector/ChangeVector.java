package change.vector.collector;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.NonSparseToSparse;

@SuppressWarnings("deprecation")
class MutableInt {
	public int value = 1;
	public void increment() { value++; }
	public int get() { return value; }
}

public class ChangeVector {
	
	public int deletesNum = 0;
	public int insertsNum = 0;
	public int updatesNum = 0;
	public int movesNum = 0;
		
	public Map<String, MutableInt> deletes = null;
	public Map<String, MutableInt> inserts = null;
	public Map<String, MutableInt> updates = null;
	public Map<String, MutableInt> moves = null;

	public static ArrayList<ChangeVector> runGumtreeDIST(Input input) throws Exception {
		ArrayList<ChangeVector> changeVectors = new ArrayList<ChangeVector>();
		
		String gumtree = "./gumtree/bin/gumtree";
		String com =  " jsondiff ";
		String gcom = gumtree + com;
//		String filetest = "./assets/test/El";				
		String filePath = "./assets/collectedFiles/";
		String file = filePath + input.projectName;
		
		

		for (int i = 0; i < Collector.instanceNumber; i++) {
			ChangeVector changeVector = new ChangeVector();
			
			Process p = Runtime.getRuntime().exec(gcom + file + i + "_before.java " + file + i + "_bic.java"); 
//			Process p = Runtime.getRuntime().exec(gcom + filetest+ "1.java " + filetest + "2.java"); 
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));  

// 			printing the console output.
//			String line = null;			
//			while((line = in.readLine()) != null) {
//				System.out.println(line);
//			}
			
			String str = IOUtils.toString(in);
			JSONObject json = new JSONObject(str);
			
			Map<String, MutableInt> deletes = new HashMap<String, MutableInt>();
			Map<String, MutableInt> inserts = new HashMap<String, MutableInt>();
			Map<String, MutableInt> updates = new HashMap<String, MutableInt>();
			Map<String, MutableInt> moves = new HashMap<String, MutableInt>();

			
			// counting instances of actions
			JSONArray actions = json.getJSONArray("actions");
			for (int j = 0; j < actions.length(); j++) {
				JSONObject tmp = actions.getJSONObject(j);

				// deleted nodes
				if(tmp.getString("action").equals("delete-node")) {
					changeVector.deletesNum++;
					
					String [] tmpSplit = tmp.getString("tree").split("\\s");
					if(tmpSplit[0].contains(":")){
						tmpSplit[0] = tmpSplit[0].substring(0, tmpSplit[0].length()-1);
					}
					MutableInt count = deletes.get(tmpSplit[0]);
					if(count == null) {
						deletes.put(tmpSplit[0], new MutableInt());
					} else {
						count.increment();
					}					
				} 
				// inserted nodes
				else if(tmp.getString("action").equals("insert-node")) {
					changeVector.insertsNum++;
					
					String [] tmpSplit = tmp.getString("tree").split("\\s");
					if(tmpSplit[0].contains(":")){
						tmpSplit[0] = tmpSplit[0].substring(0, tmpSplit[0].length()-1);
					}
					MutableInt count = inserts.get(tmpSplit[0]);
					if(count == null) {
						inserts.put(tmpSplit[0], new MutableInt());
					} else {
						count.increment();
					}		
				}
				// updated nodes
				else if(tmp.getString("action").equals("update-node")) {
					changeVector.updatesNum++;
					
					String [] tmpSplit = tmp.getString("tree").split("\\s");
					if(tmpSplit[0].contains(":")){
						tmpSplit[0] = tmpSplit[0].substring(0, tmpSplit[0].length()-1);
					}
					MutableInt count = updates.get(tmpSplit[0]);
					if(count == null) {
						updates.put(tmpSplit[0], new MutableInt());
					} else {
						count.increment();
					}		
				}
				// moved trees
				else if(tmp.getString("action").equals("move-tree")) {
					changeVector.movesNum++;
					
					String [] tmpSplit = tmp.getString("tree").split("\\s");
					if(tmpSplit[0].contains(":")){
						tmpSplit[0] = tmpSplit[0].substring(0, tmpSplit[0].length()-1);
					}
					MutableInt count = moves.get(tmpSplit[0]);
					if(count == null) {
						moves.put(tmpSplit[0], new MutableInt());
					} else {
						count.increment();
					}		
				} 
			}
			
			// console output of each node counts		
			// System.out.println("DELETES:");
			// for(String name: deletes.keySet()) {
			// 	System.out.println(name.toString() + " " + deletes.get(name).value);
			// }
			// System.out.println();
			// System.out.println("INSERTS:");
			// for(String name: inserts.keySet()) {
			// 	System.out.println(name.toString() + " " + inserts.get(name).value);
			// }
			// System.out.println();
			// System.out.println("UPDATES:");
			// for(String name: updates.keySet()) {
			// 	System.out.println(name.toString() + " " + updates.get(name).value);
			// }
			// System.out.println();
			// System.out.println("MOVES:");
			// for(String name: moves.keySet()) {
			// 	System.out.println(name.toString() + " " + moves.get(name).value);
			// }
			// System.out.println();

			
			System.out.println(i+"/"+Collector.instanceNumber);
			System.out.println("deletesNum: " + changeVector.deletesNum);
			System.out.println("insertsNum: " + changeVector.insertsNum);
			System.out.println("updateNum: " + changeVector.updatesNum);
			System.out.println("movesNum: " + changeVector.movesNum);
			System.out.println();
			
			// saving instance fields
			changeVector.deletes = deletes;
			changeVector.inserts = inserts;
			changeVector.updates = updates;
			changeVector.moves = moves;
			
			changeVectors.add(changeVector);
		}
	
		writeARFF(changeVectors, input);
		
		System.out.println("$$$$$$ Change vector extraction complete!!");
		return changeVectors;
	}
	
	@SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
	public static void writeARFF(ArrayList<ChangeVector> CVS, Input input) throws Exception {
		FastVector attributes = new FastVector();
		Instances dataSet;
        
		// writing all attributes
		int att;
	    for (att = 0; att < MyASTNode.nodes.length; att++) {
	    	attributes.addElement(new Attribute("del_" + MyASTNode.nodes[att])); 
	    } 
	    for (int att1 = 0; att < MyASTNode.nodes.length*2; att1++, att++) {
	    	attributes.addElement(new Attribute("ins_" + MyASTNode.nodes[att1]));
	    } 
	    for (int att2 = 0; att < MyASTNode.nodes.length*3; att2++, att++) {
	    	attributes.addElement(new Attribute("upd_" + MyASTNode.nodes[att2]));
	    } 
	    for (int att3 = 0; att < MyASTNode.nodes.length*4; att3++, att++) {
	    	attributes.addElement(new Attribute("mov_" + MyASTNode.nodes[att3]));
	    } 

	    dataSet = new Instances("CVS", attributes, 0);
	    List<String> astnodes = Arrays.asList(MyASTNode.nodes);
	    
	    // writing the data
	    for(ChangeVector cv : CVS) { // instances X
			double[] values = new double[dataSet.numAttributes()];
			// deletes
			for(String nodeName: cv.deletes.keySet()) {
				int nodeCount = cv.deletes.get(nodeName).value;
				int index = astnodes.indexOf(nodeName);
				for(int ast_i = 0; ast_i < astnodes.size(); ast_i++) {
					if(ast_i == index) values[ast_i] = nodeCount;
				}	
			}
			// inserts
			for(String nodeName: cv.inserts.keySet()) {
				int nodeCount = cv.inserts.get(nodeName).value;
				int index = astnodes.indexOf(nodeName);
				for(int ast_i = 0, del_i = astnodes.size(); ast_i < astnodes.size(); ast_i++, del_i++) {
					if(ast_i == index) values[del_i] = nodeCount;
				}
			}
			//updates
			for(String nodeName: cv.updates.keySet()) {
				int nodeCount = cv.updates.get(nodeName).value;
				int index = astnodes.indexOf(nodeName);
				for(int ast_i = 0, ins_i = astnodes.size()*2; ast_i < astnodes.size(); ast_i++, ins_i++) {
					if(ast_i == index) values[ins_i] = nodeCount;
				}
			}
			// moves
			for(String nodeName: cv.moves.keySet()) {
				int nodeCount = cv.moves.get(nodeName).value;
				int index = astnodes.indexOf(nodeName);
				for(int ast_i = 0, upd_i = astnodes.size()*3; ast_i < astnodes.size(); ast_i++, upd_i++) {
					if(ast_i == index) values[upd_i] = nodeCount;
				}
			}
			dataSet.add(new SparseInstance(1.0, values));
	    }
	    
	    NonSparseToSparse nonSparseToSparseInstance = new NonSparseToSparse();
	    nonSparseToSparseInstance.setInputFormat(dataSet);
	    Instances sparseDataset = Filter.useFilter(dataSet, nonSparseToSparseInstance);
	    
	    System.out.println(sparseDataset);
	    
	    ArffSaver arffSaverInstance = new ArffSaver();
	    arffSaverInstance.setInstances(sparseDataset);
	    String outFile = input.outFile + "CVC_" + input.projectName + ".arff";
	    arffSaverInstance.setFile(new File(outFile));
	    arffSaverInstance.writeBatch();
	}
	
}
	