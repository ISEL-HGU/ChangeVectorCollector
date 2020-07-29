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
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
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

	public static void runD4j(Input input) throws IOException, GitAPIException {
		int MAX_SIZE = 2000;
		Reader in = new FileReader(input.inFile);
		Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
		File file_Y = new File(input.outFile + "Y_defects4j.csv");
		File file_GV = new File(input.outFile + "GVNC_defects4j.csv");
		BufferedWriter writer_Y = Files.newBufferedWriter(Paths.get(file_Y.getAbsolutePath()));
		BufferedWriter writer_GV = Files.newBufferedWriter(Paths.get(file_GV.getAbsolutePath()));
		CSVPrinter csvprinter_Y = new CSVPrinter(writer_Y, CSVFormat.DEFAULT);
		CSVPrinter csvprinter_GV = new CSVPrinter(writer_GV, CSVFormat.DEFAULT);
		RevWalk walk = new RevWalk(input.repo);



		// for each line of records (e.g. 40)
		int all_cnt = 0;
		int record_idx = 0;
		for (CSVRecord record : records) {
			
			String shaBeforeFIX = record.get(1);
			String shaFIX = record.get(2);
			String pathBeforeFIX = "";
			String pathFIX = "";

			if (shaFIX.equals("revision.id.fixed"))
				continue;

			ObjectId bFix = input.repo.resolve(shaBeforeFIX);
			ObjectId fix = input.repo.resolve(shaFIX);
			RevCommit commitBFIX = walk.parseCommit(bFix);
			RevCommit commitFIX = walk.parseCommit(fix);
			RevTree treeBFIX = walk.parseTree(bFix);
			RevTree treeFIX = walk.parseTree(fix);

			ObjectReader reader = input.repo.newObjectReader();
			CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
			oldTreeIter.reset(reader, treeBFIX);
			CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
			newTreeIter.reset(reader, treeFIX);

			List<DiffEntry> diffs = input.git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();

			// for each files in a commit (1 ~ 3)
			for (DiffEntry entry : diffs) {
				ArrayList<Integer> g_vec = new ArrayList<Integer>();
				pathBeforeFIX = entry.getOldPath();
				pathFIX = entry.getNewPath();

				String srcBlobFIX = Utils.fetchBlob(input.repo, commitBFIX.getName(), pathBeforeFIX);
				String dstBlobFIX = Utils.fetchBlob(input.repo, commitFIX.getName(), pathFIX);

				Run.initGenerators();

				ITree srcFIX;
				ITree dstFIX;

				try {
					srcFIX = new JdtTreeGenerator().generateFromString(srcBlobFIX).getRoot();
					dstFIX = new JdtTreeGenerator().generateFromString(dstBlobFIX).getRoot();
				} catch (Exception e) {
					System.out.println("excetion: " + e);
					continue;
				}
				Matcher matchFIX = Matchers.getInstance().getMatcher(srcFIX, dstFIX);
				matchFIX.match();

				ActionGenerator gFIX = new ActionGenerator(srcFIX, dstFIX, matchFIX.getMappings());
				gFIX.generate();

				List<Action> actionsFIX = gFIX.getActions();

				if (actionsFIX.size() <= 0) {
					System.out.println("actionsFIX size < 0");
					continue;
				}
					

				// for each action node in a change
				for (Action action : actionsFIX) {
					if (action.getNode().getType() == 40 || action.getNode().getType() == 26) {
						continue;
					}

					if (action.getName().equals("DEL")) {
						g_vec.add(action.getNode().getType() + 1);
					}
				}

				if (g_vec.size() > MAX_SIZE || g_vec.size() <= 0)
					
					continue;

				if (actionsFIX.size() < MAX_SIZE) {
					for (int i = 0; i < MAX_SIZE - actionsFIX.size(); i++) {
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
				parser.setSource(dstBlobFIX.toCharArray());
				CompilationUnit cu = (CompilationUnit) parser.createAST(null);

				ArrayList<Integer> context_vec = new ArrayList<Integer>();
				HashMap<Integer, Boolean> map = new HashMap<Integer, Boolean>();

				// adding context vectors
				for (Action action : actionsFIX) {
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

				csvprinter_Y.printRecord(input.projectName + all_cnt, "-", "-", "-", "-", pathFIX, pathBeforeFIX, shaBeforeFIX, shaFIX,
											pathFIX + pathBeforeFIX + shaBeforeFIX + shaFIX, input.projectName);
				
				
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

		int MAX_SIZE = 500;
		Repository repo = input.repo;
		RevWalk walk = new RevWalk(repo);
		ArrayList<ArrayList<Integer>> gumtree_vectors = new ArrayList<ArrayList<Integer>>();
		ArrayList<BeforeBIC> new_bbics = new ArrayList<BeforeBIC>();
		File file_Y = new File(input.outFile + "Y_" + input.projectName + ".csv");
		File file_GV = new File(input.outFile + "GVNC_" + input.projectName + ".csv");
		BufferedWriter writer_Y = Files.newBufferedWriter(Paths.get(file_Y.getAbsolutePath()));
		BufferedWriter writer_GV = Files.newBufferedWriter(Paths.get(file_GV.getAbsolutePath()));
		CSVPrinter csvprinter_Y = new CSVPrinter(writer_Y, CSVFormat.DEFAULT);
		CSVPrinter csvprinter_GV = new CSVPrinter(writer_GV, CSVFormat.DEFAULT);

		try {
			int cnt = 0;
			for (BeforeBIC bbic : bbics) {
				RevCommit commitBIC = walk.parseCommit(repo.resolve(bbic.shaBIC));
				RevCommit commitBBIC = walk.parseCommit(repo.resolve(bbic.shaBefore));
//				RevCommit commitFIX = walk.parseCommit(repo.resolve(bbic.shaFix));
//				RevCommit commitBFIX = walk.parseCommit(repo.resolve(bbic.shaFix + "^"));

				String pathBIC = bbic.pathBIC;
				String pathBBIC = bbic.pathBefore;
//				String pathFIX = bbic.pathFix;
//				String pathBFIX = bbic.pathBFix;

				String srcBlobBIC = Utils.fetchBlob(repo, commitBBIC.getName(), pathBBIC);
				String dstBlobBIC = Utils.fetchBlob(repo, commitBIC.getName(), pathBIC);
//				String srcBlobFIX = Utils.fetchBlob(repo, commitBFIX.getName(), pathBFIX);
//				String dstBlobFIX = Utils.fetchBlob(repo, commitFIX.getName(), pathFIX);

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

				if (actionsBIC.size() <= 0)
					continue;

				ArrayList<Integer> g_vec = new ArrayList<Integer>();
				for (Action action : actionsBIC) {

//					System.out.println(action.getNode().getType() + " " + action.getNode().getLabel() + " "
//							+ action.getNode().toTreeString() + " " + action.getNode().toShortString() + " ");
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
//					else if (action.getName().equals("UPD")) {
//						g_vec.add(action.getNode().getType() + 85 * 2 + 1);
//					} else if (action.getName().equals("MOV")) {
//						g_vec.add(action.getNode().getType() + 85 * 3 + 1);
//					}
				}

				// if over max_size or zero, discard instance.
				if (g_vec.size() > MAX_SIZE || g_vec.size() <= 0)
					continue;

				// zero padding if less than MAX_SIZE
				if (actionsBIC.size() < MAX_SIZE) {
					for (int i = 0; i < MAX_SIZE - actionsBIC.size(); i++) {
						g_vec.add(0);
					}
				}

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

//				// adding fix change
//				ITree srcFIX;
//				ITree dstFIX;
//				try {
//					srcFIX = new JdtTreeGenerator().generateFromString(srcBlobFIX).getRoot();
//					dstFIX = new JdtTreeGenerator().generateFromString(dstBlobFIX).getRoot();
//				} catch (Exception e) {
//					continue;
//				}
//				Matcher matchFIX = Matchers.getInstance().getMatcher(srcFIX, dstFIX);
//				matchFIX.match();
//
//				ActionGenerator gFIX = new ActionGenerator(srcFIX, dstFIX, matchFIX.getMappings());
//				gFIX.generate();
//
//				List<Action> actionsFIX = gFIX.getActions();
//
//				if (actionsFIX.size() > MAX_SIZE)
//					continue;
//				if (actionsFIX.size() <= 0)
//					continue;
//
//				for (Action action : actionsBIC) {
//					if (action.getName().equals("INS")) {
//						g_vec.add(action.getNode().getType() + 1);
//					} else if (action.getName().equals("DEL")) {
//						g_vec.add(action.getNode().getType() + 85 + 1);
//					} else if (action.getName().equals("UPD")) {
//						g_vec.add(action.getNode().getType() + 85 * 2 + 1);
//					} else if (action.getName().equals("MOV")) {
//						g_vec.add(action.getNode().getType() + 85 * 3 + 1);
//					}
//				}
//
//				// zero padding if less than MAX_SIZE
//				if (g_vec.size() < MAX_SIZE * 2) {
//					for (int i = 0; i < (MAX_SIZE * 2) - g_vec.size() + MAX_SIZE; i++) {
//						g_vec.add(0);
//					}
//				}

				// adding the two lists

				g_vec.addAll(context_vec);
				System.out.println(cnt + ": " + g_vec.size());

				gumtree_vectors.add(g_vec);
				new_bbics.add(bbic);

				csvprinter_Y.printRecord(input.projectName + cnt, bbic.pathBefore, bbic.pathBIC, bbic.shaBefore,
						bbic.shaBIC, bbic.pathFix, bbic.pathBFix, bbic.shaBFix, bbic.shaFix, bbic.key,
						input.projectName);

				csvprinter_Y.flush();

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
