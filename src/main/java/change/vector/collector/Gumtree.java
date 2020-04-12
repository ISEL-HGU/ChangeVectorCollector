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

		Repository repo = input.repo;
		RevWalk walk = new RevWalk(repo);
		ArrayList<ArrayList<Integer>> gumtree_vectors = new ArrayList<ArrayList<Integer>>();
		ArrayList<BeforeBIC> new_bbics = new ArrayList<BeforeBIC>();

		try {
			int cnt = 0;
			for (BeforeBIC bbic : bbics) {

				RevCommit commitBIC = walk.parseCommit(repo.resolve(bbic.shaBIC));
				RevCommit commitBefore = walk.parseCommit(repo.resolve(bbic.shaBefore));

				String pathBefore = bbic.pathBefore;
				String pathBIC = bbic.pathBIC;

				String srcBlob = Utils.fetchBlob(repo, commitBefore.getName(), pathBefore);
				String dstBlob = Utils.fetchBlob(repo, commitBIC.getName(), pathBIC);

				Run.initGenerators();

				ITree src;
				ITree dst;
				try {
					src = new JdtTreeGenerator().generateFromString(srcBlob).getRoot();
					dst = new JdtTreeGenerator().generateFromString(dstBlob).getRoot();
				} catch (Exception e) {
					continue;
				}
				Matcher m = Matchers.getInstance().getMatcher(src, dst);
				m.match();

				ActionGenerator g = new ActionGenerator(src, dst, m.getMappings());
				g.generate();

				List<Action> actions = g.getActions();

				if (actions.size() > 500)
					continue;
				if (actions.size() <= 0)
					continue;

				ArrayList<Integer> g_vec = new ArrayList<Integer>();
				for (Action action : actions) {

					if (action.getName().equals("INS")) {
						g_vec.add(action.getNode().getType());
					} else if (action.getName().equals("DEL")) {
						g_vec.add(action.getNode().getType() + 85);
					} else if (action.getName().equals("UPD")) {
						g_vec.add(action.getNode().getType() + 85 * 2);
					} else if (action.getName().equals("MOV")) {
						g_vec.add(action.getNode().getType() + 85 * 3);
					}

				}
				gumtree_vectors.add(g_vec);
				new_bbics.add(bbic);
				System.out.println(cnt + "/" + bbics.size());
				cnt++;

			}
			writeGumVecs(input, gumtree_vectors);
			BeforeBIC.writeBBICsOnCSV(input, new_bbics, "Y_" + input.projectName + ".csv");
		} catch (Exception e) {
			e.printStackTrace();
		}
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
