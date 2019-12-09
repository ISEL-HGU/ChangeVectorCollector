package change.vector.collector;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.text.similarity.JaccardSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.commoncrawl.util.shared.SimHash;
import org.eclipse.jgit.api.errors.GitAPIException;

public class Precfix {

	public static double[][] runPrecfix(Input input, ArrayList<BeforeBIC> bbics) throws IOException, GitAPIException {
		ArrayList<DefectPatchPair> dps = new ArrayList<DefectPatchPair>();
		for (BeforeBIC bbic : bbics) {
			DefectPatchPair dp = new DefectPatchPair(bbic, input);
			dps.add(dp);
		}
//		DefectPatchPair dp = new DefectPatchPair(bbics.get(501), input);

		// get SimHash for defect-patch pairs;
		ArrayList<ArrayList<Long>> simHashes = new ArrayList<ArrayList<Long>>();
		simHashes = getSimHash(dps);

		double[][] reducer = new double[simHashes.size()][simHashes.size()];
		reducer = getKDTree(simHashes);

		double[][] similarity = new double[simHashes.size()][simHashes.size()];
		similarity = calculateSimilarity(reducer, dps);

		return similarity;
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
		return simHashes;
	}

	public static double[][] getKDTree(ArrayList<ArrayList<Long>> simHashes) {
		double[][] reducer = new double[simHashes.size()][simHashes.size()];
		for (int i = 0; i < simHashes.size(); i++) {
			for (int j = 0; j < simHashes.size(); j++) {
				float defectHD = (float) SimHash.hammingDistance(simHashes.get(i).get(0), simHashes.get(j).get(0));
				float patchHD = (float) SimHash.hammingDistance(simHashes.get(i).get(1), simHashes.get(j).get(1));
				float hammingDistance = (defectHD + patchHD) / 2;
				reducer[i][j] = hammingDistance;
			}
		}
		return reducer;
	}

	public static double[][] calculateSimilarity(double[][] reducer, ArrayList<DefectPatchPair> dps) {
		double[][] similarity = new double[dps.size()][dps.size()];
		String[] defectStrings = new String[dps.size()];
		String[] patchStrings = new String[dps.size()];
		int dpsIndex = 0;
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
				double levenDefect =  new LevenshteinDistance().apply(defectStrings[i], defectStrings[j]);
				double levenPatch = new LevenshteinDistance().apply(patchStrings[i], patchStrings[j]);
				if(levenDefect > levenDefectMax) levenDefectMax = levenDefect;
				if(levenPatch > levenPatchMax) levenPatchMax = levenPatch;
			}
		}
		System.out.println(levenDefectMax);
		System.out.println(levenPatchMax);
		for (int i = 0; i < dps.size(); i++) {
			for (int j = 0; j < dps.size(); j++) {
				if (reducer[i][j] > 17) {
					similarity[i][j] = 0.0;
				} else {
					double jaccardDefect = new JaccardSimilarity().apply(defectStrings[i], defectStrings[j]);
					double levenshteinDefect =  new LevenshteinDistance().apply(defectStrings[i], defectStrings[j]);
					levenshteinDefect /= levenDefectMax;
					levenshteinDefect = 1-levenshteinDefect;
					double jaccardPatch = new JaccardSimilarity().apply(patchStrings[i], patchStrings[j]);
					double levenshteinPatch = 1 - (new LevenshteinDistance().apply(patchStrings[i], patchStrings[j]));
					levenshteinPatch /= levenPatchMax;
					levenshteinPatch = 1-levenshteinPatch;
					double scoreDefect = jaccardDefect * 0.8 + levenshteinDefect * 0.2;
					double scorePatch = jaccardPatch * 0.8 + levenshteinPatch * 0.2;
					double score = (scoreDefect + scorePatch) / 2;
					similarity[i][j] = score;
				}
			}
		}
		return similarity;
	}

}
