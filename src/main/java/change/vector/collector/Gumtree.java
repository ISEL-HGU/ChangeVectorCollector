package change.vector.collector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;

public class Gumtree {

	public static void runGumtree(Input input, ArrayList<BeforeBIC> bbics)
			throws MissingObjectException, IncorrectObjectTypeException, IOException {

		int MAX_SIZE = 500;
		Repository repo = input.repo;
		RevWalk walk = new RevWalk(repo);
		ArrayList<ArrayList<Integer>> gumtree_vectors = new ArrayList<ArrayList<Integer>>();
		ArrayList<BeforeBIC> new_bbics = new ArrayList<BeforeBIC>();
		final String[] header_Y = { "index", "path_before", "path_BIC", "sha_before", "sha_BIC", "path_fix", "sha_fix",
		"key" };
		File file_Y = new File(input.outFile + "Y_" + input.projectName + ".csv");
		File file_GV = new File(input.outFile + "GV_" + input.projectName + ".csv");
		BufferedWriter writer_Y = Files.newBufferedWriter(Paths.get(file_Y.getAbsolutePath()));
		BufferedWriter writer_GV = Files.newBufferedWriter(Paths.get(file_GV.getAbsolutePath()));
		CSVPrinter csvprinter_Y = new CSVPrinter(writer_Y, CSVFormat.DEFAULT.withHeader(header_Y));
		CSVPrinter csvprinter_GV = new CSVPrinter(writer_GV, CSVFormat.DEFAULT);

		try {
			int cnt = 0;
			for (BeforeBIC bbic : bbics) {

				RevCommit commitBIC = walk.parseCommit(repo.resolve(bbic.shaBIC));
				RevCommit commitBBIC = walk.parseCommit(repo.resolve(bbic.shaBefore));
				RevCommit commitFIX = walk.parseCommit(repo.resolve(bbic.shaFix));
				RevCommit commitBFIX = walk.parseCommit(repo.resolve(bbic.shaFix + "^"));
				

				String pathBIC = bbic.pathBIC;
				String pathBBIC = bbic.pathBefore;
				String pathFIX = bbic.pathFix;
				String pathBFIX = bbic.pathBFix;
				

				String srcBlobBIC = Utils.fetchBlob(repo, commitBBIC.getName(), pathBBIC);
				String dstBlobBIC = Utils.fetchBlob(repo, commitBIC.getName(), pathBIC);
				String srcBlobFIX = Utils.fetchBlob(repo, commitBFIX.getName(), pathBFIX);
				String dstBlobFIX = Utils.fetchBlob(repo, commitFIX.getName(), pathFIX);
				
				Run.initGenerators();

				// for BIC 
				ITree srcBIC;
				ITree dstBIC;
				try {
					srcBIC = new JdtTreeGenerator().generateFromString(srcBlobBIC).getRoot();
					dstBIC = new JdtTreeGenerator().generateFromString(dstBlobBIC).getRoot();
				} catch (Exception e) {
					continue;
				}
				Matcher matchBIC = Matchers.getInstance().getMatcher(srcBIC, dstBIC);
				matchBIC.match();

				ActionGenerator gBIC = new ActionGenerator(srcBIC, dstBIC, matchBIC.getMappings());
				gBIC.generate();

				List<Action> actionsBIC = gBIC.getActions();

				if (actionsBIC.size() > MAX_SIZE)
					continue;
				if (actionsBIC.size() <= 0)
					continue;

				ArrayList<Integer> g_vec = new ArrayList<Integer>();
				for (Action action : actionsBIC) {

					if (action.getName().equals("INS")) {
						g_vec.add(action.getNode().getType() + 1);
					} else if (action.getName().equals("DEL")) {
						g_vec.add(action.getNode().getType() + 85 + 1);
					} else if (action.getName().equals("UPD")) {
						g_vec.add(action.getNode().getType() + 85 * 2 + 1);
					} else if (action.getName().equals("MOV")) {
						g_vec.add(action.getNode().getType() + 85 * 3 + 1);
					}
				}
				
				// zero padding if less than MAX_SIZE
				if(g_vec.size() < MAX_SIZE) {
					for (int i = 0; i < MAX_SIZE - g_vec.size(); i++) {
						g_vec.add(0);
					}
				}
				
				
				// for fix change
				ITree srcFIX;
				ITree dstFIX;
				try {
					srcFIX = new JdtTreeGenerator().generateFromString(srcBlobFIX).getRoot();
					dstFIX = new JdtTreeGenerator().generateFromString(dstBlobFIX).getRoot();
				} catch (Exception e) {
					continue;
				}
				Matcher matchFIX = Matchers.getInstance().getMatcher(srcFIX, dstFIX);
				matchFIX.match();

				ActionGenerator gFIX = new ActionGenerator(srcFIX, dstFIX, matchFIX.getMappings());
				gFIX.generate();

				List<Action> actionsFIX = gFIX.getActions();

				if (actionsFIX.size() > 500)
					continue;
				if (actionsFIX.size() <= 0)
					continue;

				for (Action action : actionsBIC) {

					if (action.getName().equals("INS")) {
						g_vec.add(action.getNode().getType() + 1);
					} else if (action.getName().equals("DEL")) {
						g_vec.add(action.getNode().getType() + 85 + 1);
					} else if (action.getName().equals("UPD")) {
						g_vec.add(action.getNode().getType() + 85 * 2 + 1);
					} else if (action.getName().equals("MOV")) {
						g_vec.add(action.getNode().getType() + 85 * 3 + 1);
					}

				}
				
				// zero padding if less than MAX_SIZE
				if(g_vec.size() < MAX_SIZE * 2) {
					for (int i = 0; i < (MAX_SIZE * 2) - g_vec.size(); i++) {
						g_vec.add(0);
					}
				}
				
				gumtree_vectors.add(g_vec);
				new_bbics.add(bbic);
				
				csvprinter_Y.printRecord(input.projectName + cnt, bbic.pathBefore, bbic.pathBIC, bbic.shaBefore,
						bbic.shaBIC, bbic.pathFix, bbic.shaFix, bbic.key);
				csvprinter_Y.flush();
				
				csvprinter_GV.print(input.projectName + cnt);
				for (Integer val : g_vec) {
					csvprinter_GV.print(val);
				}
				csvprinter_GV.println();
				csvprinter_GV.flush();
				
				System.out.println(cnt + "/" + bbics.size());
				cnt++;
			}
//			writeGumVecs(input, gumtree_vectors);
//			BeforeBIC.writeBBICsOnCSV(input, new_bbics, "Y_" + input.projectName + ".csv");
			System.out.println("wrting gumvecs complete!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		csvprinter_Y.close();
		csvprinter_GV.close();
		walk.close();
	}

	public static void writeGumVecs(Input input, ArrayList<ArrayList<Integer>> gumtree_vectors) throws IOException {
		File fileP = new File(input.outFile + "GV_" + input.projectName + ".csv");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileP.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT);

		int index = 0;
		for (ArrayList<Integer> g_vec : gumtree_vectors) {
			csvprinter.print(input.projectName + index);
			for (Integer val : g_vec) {
				csvprinter.print(val);
			}
			csvprinter.println();
			csvprinter.flush();
			index++;
		}

		csvprinter.close();
		System.out.println("wrting gumvecs complete!");
		return;
	}

}
