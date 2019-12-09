package change.vector.collector;

import java.io.IOException;
import java.util.ArrayList;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;

public class DefectPatchPair {
	String shaPatch = "";
	String pathPatch = "";
	String shaBeforePatch = "";
	String pathBeforePatch = "";
	ArrayList<String> codeDefect = new ArrayList<String>();
	ArrayList<String> codePatch = new ArrayList<String>();
	ArrayList<String> defectPatch = new ArrayList<String>();

	public DefectPatchPair(BeforeBIC bbic, Input input) throws IOException, GitAPIException {
		shaPatch = bbic.shaFix;

		pathPatch = bbic.pathFix;
		shaBeforePatch = shaPatch + "^";
		DiffEntry diff = Collector.runDiff(input.repo, shaBeforePatch, shaPatch, pathPatch);
		if (diff != null) {
			pathBeforePatch = diff.getOldPath();
		}
		defectPatch = getDefectPatch(input.repo, diff, shaPatch, pathPatch);
		codeDefect = getCodeDefect(defectPatch);
		codePatch = getCodePatch(defectPatch);

	}

	public static ArrayList<String> getDefectPatch(Repository repo, DiffEntry diff, String sha, String path)
			throws IOException {
		String code = Collector.getDiff(repo, diff);
		ArrayList<String> defectPatch = new ArrayList<String>();
		
		// Divide code by @@ for scoping
		String[] dpWithinScope = code.split("@@");
		for (int i = 1; i < dpWithinScope.length; i++) {
			// scope of interest
			if (i % 2 == 0) {
				boolean is_del = false;
				boolean is_ins = false;
				String[] lineByLine = dpWithinScope[i].split("\n");
				for (String oneLine : lineByLine) {
					if (oneLine.startsWith("-"))
						is_del = true;
					if (oneLine.startsWith("+"))
						is_ins = true;

				}
				if (is_del && is_ins) {
					defectPatch.add(dpWithinScope[i]);
				}
			}
		}
		return defectPatch;
	}

	public static ArrayList<String> getCodeDefect(ArrayList<String> defectPatch) {
		ArrayList<String> defects = new ArrayList<String>();

		for (String dp : defectPatch) {
			String[] codeLines = dp.split("\n");
			for (String codeLine : codeLines) {
				if (codeLine.startsWith("-")) {
					defects.add(codeLine.substring(1) + "\n");
				}
			}
		}

		return defects;
	}

	public static ArrayList<String> getCodePatch(ArrayList<String> defectPatch) {
		ArrayList<String> patches = new ArrayList<String>();

		for (String dp : defectPatch) {
			String[] codeLines = dp.split("\n");
			for (String codeLine : codeLines) {
				if (codeLine.startsWith("+")) {
					patches.add(codeLine.substring(1) + "\n");
				}
			}
		}

		return patches;
	}
	public String toString() {
		String string = "";
		for(String str: defectPatch) {
			string += str;
		}
		return string;
	}
}
