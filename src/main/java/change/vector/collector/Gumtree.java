package change.vector.collector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;

public class Gumtree {

	public static void runD4j3(Input input)
			throws IOException, RevisionSyntaxException, NoHeadException, GitAPIException {

		ArrayList<String> bfcList = new ArrayList<String>();
		Reader d4j_reader = new FileReader(Input.d4jFilePath + "d4j_" + input.projectName + ".csv");
		Iterable<CSVRecord> d4j_records = CSVFormat.RFC4180.parse(d4j_reader);

		for (CSVRecord record : d4j_records) {
			bfcList.add(record.get(2));
		}

		ArrayList<BICInfo> bicLists = BICInfo.collectFrom(input, bfcList);

		// writing BIC_.csv for d4j instance.
		final String[] headers = BICInfo.headers;
		File file_BIC = new File(Input.bicFilePath + "BIC_d4j_" + input.projectName + ".csv");
		BufferedWriter writer_BIC = Files.newBufferedWriter(Paths.get(file_BIC.getAbsolutePath()));
		CSVPrinter csvprinter_BIC = new CSVPrinter(writer_BIC, CSVFormat.DEFAULT.withHeader(headers));

		for (BICInfo bic : bicLists) {
			csvprinter_BIC.printRecord(bic.getBISha1(), bic.getBIPath(), bic.getPath(), bic.getFixSha1(),
					bic.getBIDate(), bic.getFixDate(), bic.getLineNum(), bic.getLineNumInPrevFixRev(),
					bic.getIsAddedLine(), bic.getLine());
			csvprinter_BIC.flush();
		}
		csvprinter_BIC.close();
	}

	public static void runD4j2(Input input) throws IOException {
		ArrayList<BeforeBIC> new_bbics = new ArrayList<BeforeBIC>();
		Reader d4j_reader = new FileReader(Input.d4jFilePath + "d4j_" + input.projectName + ".csv");
		Reader bbic_reader = new FileReader(Input.bbicFilePath + "BBIC_" + input.projectName + ".csv");
		Iterable<CSVRecord> d4j_records = CSVFormat.RFC4180.parse(d4j_reader);
		Iterable<CSVRecord> bbic_records = CSVFormat.RFC4180.parse(bbic_reader);

		ArrayList<String> sha_d4js = new ArrayList<String>();
		ArrayList<BeforeBIC> bbics = new ArrayList<BeforeBIC>();
		for (CSVRecord record : d4j_records) {
			sha_d4js.add(record.get(2));
		}
		for (CSVRecord record : bbic_records) {
			BeforeBIC bbic = new BeforeBIC(record.get(1), record.get(2), record.get(3), record.get(4), record.get(5),
					record.get(6), record.get(7), record.get(8), record.get(9), record.get(10), record.get(11));
			bbics.add(bbic);
		}

		int match_cnt = 0;
		for (int i = 0; i < sha_d4js.size(); i++) {
			for (int j = 0; j < bbics.size(); j++) {
				if (sha_d4js.get(i).equals(bbics.get(j).shaBFC)) {
					new_bbics.add(bbics.get(j));
					match_cnt++;
				}
			}
		}

		runGumtree(input, new_bbics);
		System.out.println("match_cnt: " + match_cnt);
		System.out.println("d4j: " + sha_d4js.size());
		System.out.println("bbic: " + bbics.size());
	}

	public static void runD4j(Input input) throws IOException, GitAPIException {
		int MAX_SIZE = 2000;
		Reader in = new FileReader(Input.d4jFilePath + "d4j_" + input.projectName + ".csv");
		Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
		File file_Y = new File(Input.gumvecFilePath + "Y_defects4j.csv");
		File file_GV = new File(Input.gumvecFilePath + "GVNC_defects4j.csv");
		BufferedWriter writer_Y = Files.newBufferedWriter(Paths.get(file_Y.getAbsolutePath()));
		BufferedWriter writer_GV = Files.newBufferedWriter(Paths.get(file_GV.getAbsolutePath()));
		CSVPrinter csvprinter_Y = new CSVPrinter(writer_Y, CSVFormat.DEFAULT);
		CSVPrinter csvprinter_GV = new CSVPrinter(writer_GV, CSVFormat.DEFAULT);
		RevWalk walk = new RevWalk(input.repo);

		// for each line of records (e.g. 40)
		int all_cnt = 0;
		int record_idx = 0;
		for (CSVRecord record : records) {

			String shaBeforeBFC = record.get(1);
			String shaBFC = record.get(2);
			String pathBeforeBFC = "";
			String pathBFC = "";

			// skip header
			if (shaBFC.equals("revision.id.fixed"))
				continue;

			ObjectId beforeBFC = input.repo.resolve(shaBeforeBFC);
			ObjectId bfc = input.repo.resolve(shaBFC);
			RevCommit commitBBFC = walk.parseCommit(beforeBFC);
			RevCommit commitBFC = walk.parseCommit(bfc);
			RevTree treeBBFC = walk.parseTree(beforeBFC);
			RevTree treeBFC = walk.parseTree(bfc);

			ObjectReader reader = input.repo.newObjectReader();
			CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
			oldTreeIter.reset(reader, treeBBFC);
			CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
			newTreeIter.reset(reader, treeBFC);

			List<DiffEntry> diffs = input.git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();

			// for each files in a commit (1 ~ 3)
			for (DiffEntry entry : diffs) {
				ArrayList<Integer> g_vec = new ArrayList<Integer>();
				pathBeforeBFC = entry.getOldPath();
				pathBFC = entry.getNewPath();

				String srcBlobBFC = Utils.fetchBlob(input.repo, commitBBFC.getName(), pathBeforeBFC);
				String dstBlobBFC = Utils.fetchBlob(input.repo, commitBFC.getName(), pathBFC);

				Run.initGenerators();

				ITree srcBFC;
				ITree dstBFC;

				try {
					srcBFC = new JdtTreeGenerator().generateFromString(srcBlobBFC).getRoot();
					dstBFC = new JdtTreeGenerator().generateFromString(dstBlobBFC).getRoot();
				} catch (Exception e) {
					System.out.println("excetion: " + e);
					continue;
				}
				Matcher matchBFC = Matchers.getInstance().getMatcher(srcBFC, dstBFC);
				matchBFC.match();

				ActionGenerator gBFC = new ActionGenerator(srcBFC, dstBFC, matchBFC.getMappings());
				gBFC.generate();

				List<Action> actionsBFC = gBFC.getActions();

				if (actionsBFC.size() <= 0) {
					System.out.println("actionsFIX size < 0");
					continue;
				}

				// for each action node in a change
				for (Action action : actionsBFC) {
					if (action.getNode().getType() == 40 || action.getNode().getType() == 26) {
						continue;
					}

					if (action.getName().equals("DEL")) {
						g_vec.add(action.getNode().getType() + 1);
					}
				}

				if (g_vec.size() > MAX_SIZE || g_vec.size() <= 0)

					continue;

				if (actionsBFC.size() < MAX_SIZE) {
					for (int i = 0; i < MAX_SIZE - actionsBFC.size(); i++) {
						g_vec.add(0);
					}
				}

				@SuppressWarnings("deprecation")
				ASTParser parser = ASTParser.newParser(AST.JLS9);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				Hashtable<String, String> pOptions = JavaCore.getOptions();
				pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_9);
				pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_9);
				pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_9);
				pOptions.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
				parser.setCompilerOptions(pOptions);
				parser.setSource(dstBlobBFC.toCharArray());
				CompilationUnit cu = (CompilationUnit) parser.createAST(null);

				ArrayList<Integer> context_vec = new ArrayList<Integer>();
				HashMap<Integer, Boolean> map = new HashMap<Integer, Boolean>();

				// adding context vectors
				for (Action action : actionsBFC) {
					int lineNumOfBIC = cu.getLineNumber(action.getNode().getPos());
					int parent_hash = action.getNode().getParent().getHash();
					if (map.containsKey(parent_hash)) {
						continue;
					} else {
						map.put(parent_hash, true);
						List<ITree> descendants = action.getNode().getParent().getDescendants();
						for (ITree descendant : descendants) {
							if (map.containsKey(descendant.getHash())) {
								continue;
							} else {
								int lineNumOfDescendant = cu.getLineNumber(descendant.getPos());
								if (Math.abs(lineNumOfBIC - lineNumOfDescendant) < 3) {
									map.put(descendant.getHash(), true);
									context_vec.add(descendant.getType());
								}
							}
						}
					}
				}
				g_vec.addAll(context_vec);
				String key = pathBeforeBFC + pathBFC + shaBeforeBFC + shaBFC;
				csvprinter_Y.printRecord(input.projectName + all_cnt, "-", "-", "-", "-", pathBeforeBFC, pathBFC,
						shaBeforeBFC, shaBFC, key, input.projectName);

				csvprinter_Y.flush();

				for (Integer val : g_vec) {
					csvprinter_GV.print(val);
				}
				csvprinter_GV.println();
				csvprinter_GV.flush();
				all_cnt++;

			} // end of diff for
			record_idx++;
		} // end of record for

		csvprinter_Y.close();
		csvprinter_GV.close();
		walk.close();
		System.out.println("record_cnt: " + record_idx);
		System.out.println("all_cnt: " + all_cnt);

		System.out.println("writing gumvecs complete!");
	}

	public static void runGumtree(Input input, ArrayList<BeforeBIC> bbics)
			throws MissingObjectException, IncorrectObjectTypeException, IOException {

//		int MAX_SIZE = 500;
		Repository repo = input.repo;
		RevWalk walk = new RevWalk(repo);
		ArrayList<ArrayList<Integer>> gumtree_vectors = new ArrayList<ArrayList<Integer>>();
		ArrayList<BeforeBIC> new_bbics = new ArrayList<BeforeBIC>();
		File file_Y;
		File file_GV;

		file_Y = new File(Input.gumvecFilePath + "Y_" + input.projectName + ".csv");
		file_GV = new File(Input.gumvecFilePath + "GVNC_" + input.projectName + ".csv");

		BufferedWriter writer_Y = Files.newBufferedWriter(Paths.get(file_Y.getAbsolutePath()));
		BufferedWriter writer_GV = Files.newBufferedWriter(Paths.get(file_GV.getAbsolutePath()));
		CSVPrinter csvprinter_Y = new CSVPrinter(writer_Y, CSVFormat.DEFAULT);
		CSVPrinter csvprinter_GV = new CSVPrinter(writer_GV, CSVFormat.DEFAULT);

//		// get the max length of changes
//		for (BeforeBIC bbic : bbics) {
//			RevCommit commitBIC = walk.parseCommit(repo.resolve(bbic.shaBIC));
//			RevCommit commitBBIC = walk.parseCommit(repo.resolve(bbic.shaBeforeBIC));
//
//			String pathBIC = bbic.pathBIC;
//			String pathBBIC = bbic.pathBeforeBIC;
//			String srcBlobBIC = Utils.fetchBlob(repo, commitBBIC.getName(), pathBBIC);
//			String dstBlobBIC = Utils.fetchBlob(repo, commitBIC.getName(), pathBIC);
//			Run.initGenerators();
//			ITree srcBIC;
//			ITree dstBIC;
//			try {
//				srcBIC = new JdtTreeGenerator().generateFromString(srcBlobBIC).getRoot();
//				dstBIC = new JdtTreeGenerator().generateFromString(dstBlobBIC).getRoot();
//			} catch (Exception e) {
//				continue;
//			}
//			Matcher matchBIC = Matchers.getInstance().getMatcher(srcBIC, dstBIC);
//			matchBIC.match();
//
//			ActionGenerator gBIC = new ActionGenerator(srcBIC, dstBIC, matchBIC.getMappings());
//			gBIC.generate();
//
//			List<Action> actionsBIC = gBIC.getActions();
//			int max = 0;
//			for (Action action : actionsBIC) {
//				if (action.getNode().getType() == 40 || action.getNode().getType() == 26) {
//					continue;
//				}
//				if (action.getName().equals("INS")) {
//					max++;
//				} else if (action.getName().equals("DEL")) {
//					max++;
//				}
//			}
//
//			if (max > MAX_SIZE) {
//				MAX_SIZE = max;
//			}
//		}
//		System.out.println("maxsize of gumvec: " + MAX_SIZE);

		int cnt = 0;
		for (BeforeBIC bbic : bbics) {
			RevCommit commitBIC = walk.parseCommit(repo.resolve(bbic.shaBIC));
			RevCommit commitBBIC = walk.parseCommit(repo.resolve(bbic.shaBeforeBIC));
			// RevCommit commitFIX = walk.parseCommit(repo.resolve(bbic.shaFix));
			// RevCommit commitBFIX = walk.parseCommit(repo.resolve(bbic.shaFix + "^"));

			String pathBIC = bbic.pathBIC;
			String pathBBIC = bbic.pathBeforeBIC;
			// String pathFIX = bbic.pathFix;
			// String pathBFIX = bbic.pathBFix;

			String srcBlobBIC = Utils.fetchBlob(repo, commitBBIC.getName(), pathBBIC);
			String dstBlobBIC = Utils.fetchBlob(repo, commitBIC.getName(), pathBIC);
			// String srcBlobFIX = Utils.fetchBlob(repo, commitBFIX.getName(), pathBFIX);
			// String dstBlobFIX = Utils.fetchBlob(repo, commitFIX.getName(), pathFIX);

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

			ArrayList<Integer> g_vec = new ArrayList<Integer>();
			for (Action action : actionsBIC) {
				// if regards import, discard
				if (action.getNode().getType() == 40 || action.getNode().getType() == 26) {
					continue;
				}

				if (action.getName().equals("INS")) {
					g_vec.add(action.getNode().getType() + 1);
				} else if (action.getName().equals("DEL")) {
					g_vec.add(action.getNode().getType() + 85 + 1);
				}
				// disregard move or update
				// else if (action.getName().equals("UPD")) {
				// g_vec.add(action.getNode().getType() + 85 * 2 + 1);
				// } else if (action.getName().equals("MOV")) {
				// g_vec.add(action.getNode().getType() + 85 * 3 + 1);
				// }
			}

//			// zero padding if less than MAX_SIZE
//			if (actionsBIC.size() < MAX_SIZE) {
//				for (int i = 0; i < MAX_SIZE - actionsBIC.size(); i++) {
//					g_vec.add(0);
//				}
//			}

			// using JDT parser to get the line number of AST nodes
			@SuppressWarnings("deprecation")
			ASTParser parser = ASTParser.newParser(AST.JLS9);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			Hashtable<String, String> pOptions = JavaCore.getOptions();
			pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_9);
			pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_9);
			pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_9);
			pOptions.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
			parser.setCompilerOptions(pOptions);
			parser.setSource(dstBlobBIC.toCharArray());
			CompilationUnit cu = (CompilationUnit) parser.createAST(null);

			// retrieving the context vector in AST
			ArrayList<Integer> context_vec = new ArrayList<Integer>();
			HashMap<Integer, Boolean> map = new HashMap<Integer, Boolean>();
			for (Action action : actionsBIC) {
				int lineNumOfBIC = cu.getLineNumber(action.getNode().getPos());
				int parent_hash = action.getNode().getParent().getHash();
				if (map.containsKey(parent_hash)) {
					continue;
				} else {
					map.put(parent_hash, true);
					List<ITree> descendants = action.getNode().getParent().getDescendants();
					for (ITree descendant : descendants) {
						if (map.containsKey(descendant.getHash())) {
							continue;
						} else {
							int lineNumOfDescendant = cu.getLineNumber(descendant.getPos());
							if (Math.abs(lineNumOfBIC - lineNumOfDescendant) < 3) {
								map.put(descendant.getHash(), true);
								context_vec.add(descendant.getType());
							}
						}
					}
				}
			}

			// adding the two lists
			g_vec.addAll(context_vec);
			System.out.println(cnt + ": " + g_vec.size());

			gumtree_vectors.add(g_vec);
			new_bbics.add(bbic);

			csvprinter_Y.printRecord(input.projectName + cnt, bbic.pathBeforeBIC, bbic.pathBIC, bbic.shaBeforeBIC,
					bbic.shaBIC, bbic.pathBeforeBFC, bbic.pathBFC, bbic.shaBeforeBFC, bbic.shaBFC, bbic.key,
					input.projectName, bbic.label);

			csvprinter_Y.flush();

			for (Integer val : g_vec) {
				csvprinter_GV.print(val);
			}
			csvprinter_GV.println();
			csvprinter_GV.flush();

			System.out.println(cnt + "/" + bbics.size());
			cnt++;
		}

		System.out.println("writing gumvecs complete!");

		csvprinter_Y.close();
		csvprinter_GV.close();
		walk.close();
	}

	public static void writeGumVecs(Input input, ArrayList<ArrayList<Integer>> gumtree_vectors) throws IOException {
		File fileP = new File(Input.gumvecFilePath + "GV_" + input.projectName + ".csv");
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
