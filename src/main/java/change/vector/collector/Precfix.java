package change.vector.collector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.text.similarity.JaccardSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.commoncrawl.util.shared.SimHash;
import org.eclipse.jgit.api.errors.GitAPIException;

public class Precfix {

	public static void runPrecfix(Input input, ArrayList<BeforeBIC> bbics) throws IOException, GitAPIException {
		ArrayList<DefectPatchPair> dps = new ArrayList<DefectPatchPair>();
		if(input.inFile.contains("combined")) {
			// combined part
			int igniteNum = 647;
			int luceneNum = 1041;
			int zookeeperNum = 294;
			int flinkNum = 1351;
			int isisNum = 396;
			int mahoutNum = 386;
			// int oozieNum = 514;
			Input inputIgnite = new Input("https://github.com/apache/ignite", input.inFile, input.outFile);
			Input inputLucene = new Input("https://github.com/apache/lucene-solr", input.inFile, input.outFile);
			Input inputZookeeper = new Input("https://github.com/apache/zookeeper", input.inFile, input.outFile);
			Input inputFlink = new Input("https://github.com/apache/flink", input.inFile, input.outFile);
			Input inputIsis = new Input("https://github.com/apache/isis", input.inFile, input.outFile);
			Input inputMahout = new Input("https://github.com/apache/mahout", input.inFile, input.outFile);
			Input inputOozie = new Input("https://github.com/apache/oozie", input.inFile, input.outFile);
			for (int i = 0; i < bbics.size(); i++) {
				DefectPatchPair dp;
				if (i < igniteNum) {
					dp = new DefectPatchPair(bbics.get(i), inputIgnite);
				} else if (i < (igniteNum + luceneNum)) {
					dp = new DefectPatchPair(bbics.get(i), inputLucene);
				} else if (i < igniteNum + luceneNum + zookeeperNum) {
					dp = new DefectPatchPair(bbics.get(i), inputZookeeper);
				} else if (i < igniteNum + luceneNum + zookeeperNum + flinkNum) {
					dp = new DefectPatchPair(bbics.get(i), inputFlink);
				} else if (i < igniteNum + luceneNum + zookeeperNum + flinkNum + isisNum) {
					dp = new DefectPatchPair(bbics.get(i), inputIsis);
				} else if (i < igniteNum + luceneNum + zookeeperNum + flinkNum + isisNum + mahoutNum) {
					dp = new DefectPatchPair(bbics.get(i), inputMahout);
				} else {
					dp = new DefectPatchPair(bbics.get(i), inputOozie);
				}
				dps.add(dp);
			}
			
		}
		else {
			for (BeforeBIC bbic : bbics) {
				DefectPatchPair dp = new DefectPatchPair(bbic, input);
				dps.add(dp);
			}
		}
		
//		DefectPatchPair dp = new DefectPatchPair(bbics.get(501), input);

		// get SimHash for defect-patch pairs;
		ArrayList<ArrayList<Long>> simHashes = new ArrayList<ArrayList<Long>>();
		simHashes = getSimHash(dps);

		double[][] reducer = new double[simHashes.size()][simHashes.size()];
		reducer = getReducers(simHashes);

		double[][] similarity = new double[simHashes.size()][simHashes.size()];
		similarity = calculateSimilarity(reducer, dps);

		if (input.inFile.contains("combined")) {
			writePrecfixMulti(input, similarity);
		} else {
			writePrecfix(input, similarity);
		}

	}

	public static ArrayList<ArrayList<Long>> getSimHash(ArrayList<DefectPatchPair> dps) {
		ArrayList<ArrayList<Long>> simHashes = new ArrayList<ArrayList<Long>>();
		for (DefectPatchPair dp : dps) {
			String defectString = "";
			String patchString = "";
			for (int i = 0; i < dp.codeDefect.size(); i++) {
				defectString += dp.codeDefect.get(i);
			}
			for (int i = 0; i < dp.codePatch.size(); i++) {
				patchString += dp.codePatch.get(i);
			}
			long defectSH = SimHash.computeOptimizedSimHashForString(defectString);
			long patchSH = SimHash.computeOptimizedSimHashForString(patchString);
			ArrayList<Long> dpPair = new ArrayList<Long>();
			dpPair.add(defectSH);
			dpPair.add(patchSH);
			simHashes.add(dpPair);
		}
		System.out.println("Calculating SimHashes complete!");
		return simHashes;
	}

	public static double[][] getReducers(ArrayList<ArrayList<Long>> simHashes) {
		double[][] reducer = new double[simHashes.size()][simHashes.size()];
		for (int i = 0; i < simHashes.size(); i++) {
			for (int j = 0; j < simHashes.size(); j++) {
				float defectHD = (float) SimHash.hammingDistance(simHashes.get(i).get(0), simHashes.get(j).get(0));
				float patchHD = (float) SimHash.hammingDistance(simHashes.get(i).get(1), simHashes.get(j).get(1));
				float hammingDistance = (defectHD + patchHD) / 2;
				reducer[i][j] = hammingDistance;
			}
		}
		System.out.println("Calculating reducers complete!");
		return reducer;
	}

	public static double[][] calculateSimilarity(double[][] reducer, ArrayList<DefectPatchPair> dps) {
		double[][] similarity = new double[dps.size()][dps.size()];
		String[] defectStrings = new String[dps.size()];
		String[] patchStrings = new String[dps.size()];
		int dpsIndex = 0;

		// concatenating ArrayList<String> of code to one String
		for (DefectPatchPair dp : dps) {
			String defectString = "";
			String patchString = "";
			for (int i = 0; i < dp.codeDefect.size(); i++) {
				defectString += dp.codeDefect.get(i);
			}
			for (int i = 0; i < dp.codePatch.size(); i++) {
				patchString += dp.codePatch.get(i);
			}
			defectStrings[dpsIndex] = defectString;
			patchStrings[dpsIndex] = patchString;
			dpsIndex++;
		}

		double levenDefectMax = 0;
		double levenPatchMax = 0;

		for (int i = 0; i < dps.size(); i++) {
			for (int j = 0; j < dps.size(); j++) {
				if (reducer[i][j] <= 17) {
					double levenDefect = new LevenshteinDistance().apply(defectStrings[i], defectStrings[j]);
					double levenPatch = new LevenshteinDistance().apply(patchStrings[i], patchStrings[j]);
					if (levenDefect > levenDefectMax)
						levenDefectMax = levenDefect;
					if (levenPatch > levenPatchMax)
						levenPatchMax = levenPatch;
				}
			}
//			if (i > 0) {
//				System.out.print(String.format("\033[%dA", 1)); // Move up
//			}
//			System.out.println("Getting Levenshtien Max " + i + "/" + dps.size());
		}
		for (int i = 0; i < dps.size(); i++) {
			for (int j = 0; j < dps.size(); j++) {
				if (reducer[i][j] > 17) {
					similarity[i][j] = 0.0;
				} else {
					if (defectStrings[i].equals("") || defectStrings[j].equals("") || patchStrings[i].equals("")
							|| patchStrings[j].equals("")) {
						similarity[i][j] = 0.0;
						continue;
					}
					if (i == j) {
						similarity[i][j] = 1.0;
						continue;
					}
					double jaccardDefect = new JaccardSimilarity().apply(defectStrings[i], defectStrings[j]);
					double levenshteinDefect = new LevenshteinDistance().apply(defectStrings[i], defectStrings[j]);
					levenshteinDefect /= levenDefectMax;
					levenshteinDefect = 1 - levenshteinDefect;
					double jaccardPatch = new JaccardSimilarity().apply(patchStrings[i], patchStrings[j]);
					double levenshteinPatch = 1 - (new LevenshteinDistance().apply(patchStrings[i], patchStrings[j]));
					levenshteinPatch /= levenPatchMax;
					levenshteinPatch = 1 - levenshteinPatch;
					double scoreDefect = jaccardDefect * 0.8 + levenshteinDefect * 0.2;
					double scorePatch = jaccardPatch * 0.8 + levenshteinPatch * 0.2;
					double score = (scoreDefect + scorePatch) / 2;

					similarity[i][j] = score;
				}
			}
//			if (i > 0) {
//				System.out.print(String.format("\033[%dA", 1)); // Move up
//			}
//			System.out.println("Getting Scores " + i + "/" + dps.size());
		}
		return similarity;
	}

	public static void writePrecfix(Input input, double[][] similarity) throws IOException {
		File outFile = new File(input.outFile + "test_" + input.projectName + ".csv");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT);

		csvprinter.print(input.projectName);
		for (int i = 0; i < similarity.length; i++) {
			csvprinter.print(i);
		}
		csvprinter.println();

		for (int i = 0; i < similarity.length; i++) {
			csvprinter.print(i);
			for (int j = 0; j < similarity.length; j++) {
				csvprinter.print(similarity[i][j]);
			}
			csvprinter.println();
		}
		csvprinter.close();
		System.out.println("writing precfix done!");
	}

	public static void writePrecfixMulti(Input input, double[][] similarity) throws IOException {
		File outFile = new File(input.outFile + "_prec_combined7" + ".csv");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT);

		// combined part
		int igniteNum = 647;
		int luceneNum = 1041;
		int zookeeperNum = 294;
		int flinkNum = 1351;
		int isisNum = 396;
		int mahoutNum = 386;
		int oozieNum = 514;

		// index of x-axis
		// writing index of x-axis
		csvprinter.print("combined7");
		for (int i = 0; i < igniteNum; i++) {
			csvprinter.print("ignite" + i);
		}
		for (int i = 0; i < luceneNum; i++) {
			csvprinter.print("lucene-solr" + i);
		}
		for (int i = 0; i < zookeeperNum; i++) {
			csvprinter.print("zookeeper" + i);
		}
		for (int i = 0; i < flinkNum; i++) {
			csvprinter.print("flink" + i);
		}
		for (int i = 0; i < isisNum; i++) {
			csvprinter.print("isis" + i);
		}
		for (int i = 0; i < mahoutNum; i++) {
			csvprinter.print("mahout" + i);
		}
		for (int i = 0; i < oozieNum; i++) {
			csvprinter.print("oozie" + i);
		}
		csvprinter.println();

		// writing data
		for (int i = 0, lucene = 0, zookeeper = 0, flink = 0, isis = 0, mahout = 0, oozie = 0; i < similarity.length; i++) {
			if (i < igniteNum) {
				csvprinter.print("ignite" + i);
			} else if (i < (igniteNum + luceneNum)) {
				csvprinter.print("lucene-solr" + (lucene++));
			} else if (i < igniteNum + luceneNum + zookeeperNum) {
				csvprinter.print("zookeeper" + (zookeeper++));
			} else if (i < igniteNum + luceneNum + zookeeperNum + flinkNum) {
				csvprinter.print("flink" + (flink++));
			} else if (i < igniteNum + luceneNum + zookeeperNum + flinkNum + isisNum) {
				csvprinter.print("isis" + (isis++));
			} else if (i < igniteNum + luceneNum + zookeeperNum + flinkNum + isisNum + mahoutNum) {
				csvprinter.print("mahout" + (mahout++));
			} else if (i < igniteNum + luceneNum + zookeeperNum + flinkNum + isisNum + mahoutNum + oozieNum) {
				csvprinter.print("oozie" + (oozie++));
			}
			for (int j = 0; j < similarity.length; j++) {
				csvprinter.print(similarity[i][j]);
			}
			csvprinter.println();
		}
		csvprinter.close();
	}
}