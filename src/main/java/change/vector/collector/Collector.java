package change.vector.collector;

import java.io.File;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.DiffConfig;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.FollowFilter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

public class Collector {
	public static int instanceNumber;
	public static int dups = 0;

	// -r option
	public static ArrayList<BeforeBIC> collectBeforeBIC(Input input)
			throws GitAPIException, FileNotFoundException, IOException {
		ArrayList<BeforeBIC> bbics = new ArrayList<BeforeBIC>();

		// load the prepared BIC file from BugPatchCollector
		Reader in;
		in = new FileReader(input.inputDir + "BIC_" + input.projectName + ".csv");

		Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);

		final String[] headers = { "index", "path_bbic", "path_bic", "sha_bbic", "sha_bic", "path_bbfc", "path_bfc",
				"sha_bbfc", "sha_bfc", "key", "project", "label" };

		File fileP = new File(input.outputDir + "BBIC_" + input.projectName + ".csv");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileP.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers));

		int index = 0;
		for (CSVRecord record : records) {
			boolean isKeyDuplicate = false;
			BlameCommand blamer = new BlameCommand(input.repo);

			// retrieve the record we need
			String shaBIC = record.get(0);
			String pathBIC = record.get(1);
			String pathBFC = record.get(2);
			String shaBFC = record.get(3);
			String lineBIC = record.get(6);
			String lineBFC = record.get(7);
			String content = record.get(9);

			if (shaBIC.contains("BISha1"))
				continue; // skip the header
			if (content.length() < 3)
				continue; // skip really short ones
			if (shaBIC.equals(shaBFC))
				continue;// skip if BIC == FIX

			// get before instance that has before instances by blaming
			ObjectId bicID = input.repo.resolve(shaBIC);
			blamer.setStartCommit(bicID);
			blamer.setFilePath(pathBIC);

			BlameResult blameBIC;
			try {
				blameBIC = blamer.call();
			} catch (Exception e) {
				continue;
			}

			RevCommit commitBeforeBIC;
			try {
				commitBeforeBIC = blameBIC.getSourceCommit(Integer.parseInt(lineBIC) - 1);
			} catch (Exception e) {
				continue;
			}

			// retrieve SHA and path of before BIC
			String pathBeforeBIC = blameBIC.getSourcePath(Integer.parseInt(lineBIC) - 1);
			String shaBeforeBIC = commitBeforeBIC.getName();

			// if there are no before instances
			// (blamed commit is equal to BIC)
			// get the path of BIC~1
			RevWalk walk = new RevWalk(input.repo);
			RevCommit commitBIC = walk.parseCommit(bicID);
			if (commitBIC.getParentCount() == 0) {
				continue;
			}

			if (shaBeforeBIC.equals(shaBIC)) {
				DiffEntry diff = runDiff(input.repo, shaBIC + "^", shaBIC, pathBIC);
				if (diff == null) {
					continue;
				} else {
					pathBeforeBIC = diff.getOldPath();
					shaBeforeBIC = shaBIC + "^";
				}
			}

			// get before instance of fix as well.
			ObjectId fixID = input.repo.resolve(shaBFC);
			blamer.setStartCommit(fixID);
			blamer.setFilePath(pathBFC);

			BlameResult blameFIX;
			try {
				blameFIX = blamer.call();
			} catch (Exception e) {
				continue;
			}

			RevCommit commitBeforeFix;
			try {
				commitBeforeFix = blameFIX.getSourceCommit(Integer.parseInt(lineBFC) - 1);
			} catch (Exception e) {
				continue;
			}

			// retrieve SHA and path of before FIX
			String pathBeforeBFC = blameFIX.getSourcePath(Integer.parseInt(lineBFC) - 1);
			String shaBeforeBFC = commitBeforeFix.getName();

			// if there are no before instances
			// (blamed commit is equal to BIC)
			// get the path of BIC~1
			RevCommit commitFix = walk.parseCommit(fixID);
			if (commitFix.getParentCount() == 0) {
				continue;
			}
			if (shaBeforeBFC.equals(shaBFC)) {
				DiffEntry diff = runDiff(input.repo, shaBFC + "^", shaBFC, pathBFC);
				if (diff == null) {
					continue;
				} else {
					pathBeforeBFC = diff.getOldPath();
					shaBeforeBFC = shaBFC + "^";
				}
			}

			String key = pathBIC + "\n" + shaBIC + pathBFC + "\n" + shaBFC + "\n";

			// skip duplicates
			for (int j = 0; j < bbics.size(); j++) {
				if (bbics.get(j).key.equals(key)) {
					isKeyDuplicate = true;
					dups++;
				}
			}
			if (isKeyDuplicate) {
				continue;
			}

			// add BBIC when passed all of the above
			BeforeBIC bbic = new BeforeBIC(pathBeforeBIC, pathBIC, shaBeforeBIC, shaBIC, pathBeforeBFC, pathBFC,
					shaBeforeBFC, shaBFC, key, input.projectName, "1");
			bbics.add(bbic);

			csvprinter.printRecord(input.projectName + index, bbic.pathBeforeBIC, bbic.pathBIC, bbic.shaBeforeBIC,
					bbic.shaBIC, bbic.pathBeforeBFC, bbic.pathBFC, bbic.shaBeforeBFC, bbic.shaBFC, bbic.key,
					input.projectName, "1");
			csvprinter.flush();

			index++;
			System.out.println(index);
			walk.close();
		}

		System.out.println("########### Finish collecting " + index + " BBICs from repo! ###########");
		csvprinter.close();
		return bbics;
	}

	public static ArrayList<BeforeBIC> collectBeforeBICFromLocalFile(Input input)
			throws FileNotFoundException, IOException {
		ArrayList<BeforeBIC> bbics = new ArrayList<BeforeBIC>();
		Reader in = new FileReader(input.inputDir + "BBIC_" + input.projectName + ".csv");

		Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);

		for (CSVRecord record : records) {
			String pathBeforeBIC = record.get(1);
			String pathBIC = record.get(2);
			String shaBeforeBIC = record.get(3);
			String shaBIC = record.get(4);
			String pathBeforeBFC = record.get(5);
			String pathBFC = record.get(6);
			String shaBeforeBFC = record.get(7);
			String shaBFC = record.get(8);
			String key = record.get(9);
			String project = record.get(10);
			String label = record.get(11);
			if (pathBeforeBIC.contains("path_bbic"))
				continue;
			BeforeBIC bbic = new BeforeBIC(pathBeforeBIC, pathBIC, shaBeforeBIC, shaBIC, pathBeforeBFC, pathBFC,
					shaBeforeBFC, shaBFC, key, project, label);
			bbics.add(bbic);
		}

		System.out.println("########### Finish collecting BBIC from local file: " + bbics.size() + " ###########");
		return bbics;
	}

	public static String getDiff(Repository repo, DiffEntry diff) throws IOException {
		String code = "";
		OutputStream out = new ByteArrayOutputStream();
		try (DiffFormatter formatter = new DiffFormatter(out)) {
			formatter.setRepository(repo);
			formatter.format(diff);
			code = out.toString();
		}
		return code;
	}

	// https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/porcelain/DiffRenamedFile.java
	public static DiffEntry runDiff(Repository repo, String oldCommit, String newCommit, String path)
			throws IOException, GitAPIException {
		DiffEntry diff = diffFile(repo, oldCommit, newCommit, path);
		return diff;
	}

	private static @NonNull DiffEntry diffFile(Repository repo, String oldCommit, String newCommit, String path)
			throws IOException, GitAPIException {
		Config config = new Config();
		config.setBoolean("diff", null, "renames", true);
		DiffConfig diffConfig = config.get(DiffConfig.KEY);
		try (Git git = new Git(repo)) {
			List<DiffEntry> diffList = git.diff().setOldTree(prepareTreeParser(repo, oldCommit))
					.setNewTree(prepareTreeParser(repo, newCommit)).setPathFilter(FollowFilter.create(path, diffConfig))
					.call();
			if (diffList.size() == 0) {
//				System.out.println("diffList.size() == 0");
				return null;
			}

			if (diffList.size() > 1) {
				throw new RuntimeException("invalid diff");
			}
			return diffList.get(0);
		}
	}

	private static AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws IOException {
		// from the commit we can build the tree which allows us to construct the
		// TreeParser
		// noinspection Duplicates
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(objectId));
			RevTree tree = walk.parseTree(commit.getTree().getId());

			CanonicalTreeParser treeParser = new CanonicalTreeParser();
			try (ObjectReader reader = repository.newObjectReader()) {
				treeParser.reset(reader, tree.getId());
			}

			walk.dispose();

			return treeParser;
		}
	}

	/*
	 * Copyright 2013, 2014 Dominik Stadler Licensed under the Apache License,
	 * Version 2.0 (the "License"); you may not use this file except in compliance
	 * with the License. You may obtain a copy of the License at
	 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
	 * or agreed to in writing, software distributed under the License is
	 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
	 * KIND, either express or implied. See the License for the specific language
	 * governing permissions and limitations under the License.
	 */
	public static void collectFiles(Input input, ArrayList<BeforeBIC> bbics) throws IOException {
		String outPath = "./assets/collectedFiles/";
		if (Main.is_all)
			outPath = "./assets/alls/collectedFiles/";
		OutputStream outputStream;
		try (Repository repo = input.repo) {
			// find the HEAD

			// a RevWalk allows to walk over commits based on some filtering that is defined
			try (RevWalk revWalk = new RevWalk(repo)) {
				int i = 0;
				for (BeforeBIC bbic : bbics) {
					ObjectId beforeId = repo.resolve(bbic.shaBeforeBIC);
					ObjectId bicId = repo.resolve(bbic.shaBIC);

					RevCommit beforeCommit = revWalk.parseCommit(beforeId);
					RevCommit bicCommit = revWalk.parseCommit(bicId);
					// and using commit's tree find the path
					RevTree beforeTree = beforeCommit.getTree();
					RevTree bicTree = bicCommit.getTree();

					System.out.println("Having beforeTree: " + beforeTree);
					System.out.println("Having bicTree: " + bicTree);

					// bbic files
					try (TreeWalk treeWalk = new TreeWalk(repo)) {
						treeWalk.addTree(beforeTree);
						treeWalk.setRecursive(true);
						treeWalk.setFilter(PathFilter.create(bbic.pathBeforeBIC));
						if (!treeWalk.next()) {
							throw new IllegalStateException("Did not find expected file " + bbic.pathBeforeBIC);
						}

						ObjectId objectId = treeWalk.getObjectId(0);
						ObjectLoader loader = repo.open(objectId);

						// and then one can the loader to read the file
						outputStream = new FileOutputStream(outPath + input.projectName + i + "_before.java");
						loader.copyTo(outputStream);
					}

					// bic files
					try (TreeWalk treeWalk = new TreeWalk(repo)) {
						treeWalk.addTree(bicTree);
						treeWalk.setRecursive(true);
						treeWalk.setFilter(PathFilter.create(bbic.pathBIC));
						if (!treeWalk.next()) {
							throw new IllegalStateException("Did not find expected file " + bbic.pathBIC);
						}

						ObjectId objectId = treeWalk.getObjectId(0);
						ObjectLoader loader = repo.open(objectId);

						// and then one can the loader to read the file
						outputStream = new FileOutputStream(outPath + input.projectName + i + "_bic.java");
						loader.copyTo(outputStream);
					}
					revWalk.dispose();
					i++;
				}
				instanceNumber = i;
			}
		}
		System.out.println("instance number: " + instanceNumber);
		System.out.println("########### Finish collecting Files from BBIC! ###########");
	}

	// removes instances that
	public static ArrayList<BeforeBIC> rmDups(ArrayList<BeforeBIC> bbics, Input input) throws IOException {

		final String[] headers = { "index", "path_bbic", "path_bic", "sha_bbic", "sha_bic", "path_bbfc", "path_bfc",
				"sha_bbfc", "sha_bfc", "key", "project", "label" };
		File fileP = new File(input.outputDir + "BBIC_" + input.projectName + ".csv");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileP.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers));
		Map<String, MutableInt> dupMap = new HashMap<String, MutableInt>();

		// put bbics in a hashmap w.r.t. shaFix
		for (BeforeBIC bbic : bbics) {
			String key = bbic.pathBFC + bbic.shaBFC;

			MutableInt count = dupMap.get(key);
			if (count == null) {
				dupMap.put(key, new MutableInt());
			} else {
				count.increment();
			}
		}

		// if an instance count is greater than 1, remove
		for (int i = 0; i < bbics.size(); i++) {
			String key = bbics.get(i).pathBFC + bbics.get(i).shaBFC;
			if (dupMap.get(key).value > 1) {
				bbics.remove(i);
				i--;
			}
		}

		// overwrite the new bbics on BBIC.csv
		int index = 0;
		for (BeforeBIC bbic : bbics) {
			// writing the BBIC file
			csvprinter.printRecord(input.projectName + index, bbic.pathBeforeBIC, bbic.pathBIC, bbic.shaBeforeBIC,
					bbic.shaBIC, bbic.pathBeforeBFC, bbic.pathBFC, bbic.shaBeforeBFC, bbic.shaBFC, bbic.key,
					input.projectName);
			csvprinter.flush();
			index++;
		}

		csvprinter.close();
		return bbics;
	}

	public static ArrayList<BeforeBIC> getAllCleanCommits(Input input)
			throws NoHeadException, GitAPIException, IOException {
		ArrayList<BeforeBIC> bbics = new ArrayList<BeforeBIC>();
		RevWalk walk = new RevWalk(input.repo);
		TreeWalk treeWalk = new TreeWalk(input.repo);
		Iterable<RevCommit> all_commits = input.git.log().all().call();
		Reader in = new FileReader(input.inputDir + "BIC_" + input.projectName + ".csv");
		Iterable<CSVRecord> records_iter = CSVFormat.RFC4180.parse(in);
		Iterator<CSVRecord> iter = records_iter.iterator();
		List<CSVRecord> records = new ArrayList<CSVRecord>();
		while (iter.hasNext()) {
			records.add(iter.next());
		}
		Map<String, Boolean> bic_map = new HashMap<String, Boolean>();

		// make hashmap of bic
		for (CSVRecord record : records) {
			String pathBIC = record.get(1);
			String shaBIC = record.get(0);
			if (!bic_map.containsKey(pathBIC + shaBIC)) {
				bic_map.put(pathBIC + shaBIC, true);
			}
		}

		int count = 0;
		for (RevCommit commit : all_commits) {

			if (commit.getParentCount() < 1) {
				continue;
			}
			String cur_sha = commit.getName();
			String prev_sha = cur_sha + "~1";

			RevCommit prev_commit = walk.parseCommit(input.repo.resolve(prev_sha));
			RevTree prev_tree = walk.parseTree(prev_commit);
			RevTree cur_tree = walk.parseTree(commit);

			ObjectReader reader = input.repo.newObjectReader();
			CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
			oldTreeIter.reset(reader, prev_tree);
			CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
			newTreeIter.reset(reader, cur_tree);

			List<DiffEntry> diffs = input.git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();

			// for all the file paths in each commit
			for (DiffEntry entry : diffs) {
				String prev_path = entry.getOldPath();
				String cur_path = entry.getNewPath();
				String key = prev_path + cur_path + prev_sha + cur_sha;

				if (prev_path.contains("/dev/null") || cur_path.indexOf("Test") >= 0 || !cur_path.endsWith(".java")) {
					continue;
				}

				// if it is a bic retrieve the full bbic with bfc
				BeforeBIC bbic;
				if (bic_map.containsKey(cur_path + cur_sha)) {
					continue;
				}
				// else get only the commit itself because there is no fix for non-buggy commit

				bbic = new BeforeBIC(prev_path, cur_path, prev_sha, cur_sha, "-", "-", "-", "-", key, input.projectName,
						"0");
				bbics.add(bbic);
			}
			count++;
		}
		System.out.println(count);
		System.out.println("All clean commits collected!");
		walk.close();
		treeWalk.close();

		return bbics;
	}

	public static ArrayList<BeforeBIC> getAllCommits(Input input) throws NoHeadException, GitAPIException, IOException {
		ArrayList<BeforeBIC> bbics;
		// load bbic from repo or local
		if (new File(input.inputDir + "BBIC_" + input.projectName + ".csv").exists()) {
			bbics = collectBeforeBICFromLocalFile(input);

		} else {
			bbics = collectBeforeBIC(input);
		}

		RevWalk walk = new RevWalk(input.repo);
		TreeWalk treeWalk = new TreeWalk(input.repo);
		Iterable<RevCommit> all_commits = input.git.log().all().call();
		Reader in = new FileReader(input.inputDir + "BIC_" + input.projectName + ".csv");
		Iterable<CSVRecord> records_iter = CSVFormat.RFC4180.parse(in);
		Iterator<CSVRecord> iter = records_iter.iterator();
		List<CSVRecord> records = new ArrayList<CSVRecord>();
		while (iter.hasNext()) {
			records.add(iter.next());
		}
		Map<String, BeforeBIC> bic_map = new HashMap<String, BeforeBIC>();

		// make hashmap of bbic
		for (BeforeBIC bbic : bbics) {
			if (!bic_map.containsKey(bbic.pathBIC + bbic.shaBIC)) {
				bic_map.put(bbic.pathBIC + bbic.shaBIC, bbic);
			}
		}

		// for each of all the commit
		int count = 0;
		for (RevCommit commit : all_commits) {

			if (commit.getParentCount() < 1) {
				continue;
			}
			
			String cur_sha = commit.getName();
			String prev_sha = cur_sha + "~1";

			RevCommit prev_commit = walk.parseCommit(input.repo.resolve(prev_sha));
			RevTree prev_tree = walk.parseTree(prev_commit);
			RevTree cur_tree = walk.parseTree(commit);

			ObjectReader reader = input.repo.newObjectReader();
			CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
			oldTreeIter.reset(reader, prev_tree);
			CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
			newTreeIter.reset(reader, cur_tree);

			List<DiffEntry> diffs = input.git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();

			// for all the file paths in each commit
			for (DiffEntry entry : diffs) {
				String prev_path = entry.getOldPath();
				String cur_path = entry.getNewPath();
				String key = prev_path + cur_path + prev_sha + cur_sha;

				if (prev_path.contains("/dev/null") || cur_path.indexOf("Test") >= 0 || !cur_path.endsWith(".java")) {
					continue;
				}

				// if it is a bic retrieve the full bbic with bfc
				BeforeBIC bbic;
				if (bic_map.containsKey(cur_path + cur_sha)) {
					bbic = bic_map.get(cur_path + cur_sha);
				}
				// else get only the commit itself because there is no fix for non-buggy commit
				else {
					bbic = new BeforeBIC(prev_path, cur_path, prev_sha, cur_sha, "-", "-", "-", "-", key,
							input.projectName, "0");
				}

				bbics.add(bbic);
			}
			count++;
		}
		System.out.println(count);
		System.out.println("All commits collected and merged!");
		walk.close();
		treeWalk.close();

		return bbics;
	}

}
