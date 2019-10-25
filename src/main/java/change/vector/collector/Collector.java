package change.vector.collector;

import change.vector.collector.BeforeBIC;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

public class Collector {
	public static int instanceNumber;
	
	public static ArrayList<BeforeBIC> collectBeforeBIC(Input input) throws GitAPIException, FileNotFoundException, IOException {
		ArrayList<BeforeBIC> bbics = new ArrayList<BeforeBIC>();
		
		// load the prepared BIC file from BugPatchCollector
		Reader in = new FileReader(input.inFile);
		
		Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
		final String[] headers = {"index", "path_before", "path_BIC", "sha_before", "sha_BIC", "path_fix", "sha_fix", "key"};
		File fileP = new File(input.bbicFilePath);
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileP.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers));
		
		int index = 0;
		for(CSVRecord record : records) {
			boolean isKeyDuplicate = false;
			BlameCommand blamer = new BlameCommand(input.repo);
	
			// retrieve the record we need
			String sha = record.get(0);
			String file = record.get(1);
			String lineNum = record.get(4);
			String content = record.get(6);
			String path_fix = record.get(2);
			String sha_fix = record.get(3);
			
			if(sha.contains("BIShal1")) continue; //skip the header
			if(content.length()<3) continue; //skip really short ones


			// call blamer of BIC line-by-line
			ObjectId commitID = input.repo.resolve(sha);
			blamer.setStartCommit(commitID);
			blamer.setFilePath(file);
			BlameResult blame = blamer.call();
			RevCommit commit = blame.getSourceCommit(Integer.parseInt(lineNum));
		
			// retrieve SHA and path of before BIC
			String pathBefore = blame.getSourcePath(Integer.parseInt(lineNum));
			String shaBefore = commit.getName();
			String key = sha + file + shaBefore + pathBefore + sha_fix + path_fix;
			
			if(shaBefore.equals(sha)) continue; //skip when there are no BBIC
			
			for (int j = 0; j < bbics.size(); j++) { //skip duplicates
				if(bbics.get(j).key.equals(key)) {
					isKeyDuplicate = true;
				}
			}
			if (isKeyDuplicate) continue;
			
			// add BBIC when passed all of the above
			BeforeBIC bbic = new BeforeBIC(pathBefore, file, shaBefore, sha, path_fix, sha_fix, key);
			bbics.add(bbic);

			// writing the BBIC file
			csvprinter.printRecord(input.projectName+index, bbic.pathBefore, bbic.pathBIC, bbic.shaBefore, bbic.shaBIC,
					bbic.pathFix, bbic.shaFix, bbic.key);
			csvprinter.flush();
			
			System.out.println("#" + index);
			System.out.println(bbic.toString());
			index++;
		}
		

		System.out.println("########### Finish collecting BBIC from repo! ###########");
		
		csvprinter.close();
		
		return bbics;
	}
	
	public static ArrayList<BeforeBIC> collectBeforeBICFromLocalFile(Input input) throws FileNotFoundException, IOException {
		ArrayList<BeforeBIC> bbics = new ArrayList<BeforeBIC>();
		Reader in = new FileReader(input.inFile);
		
		Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
		
		
		for(CSVRecord record : records) {	
			String pathBefore = record.get(0);
			String pathBIC = record.get(1);
			String shaBefore = record.get(2);
			String shaBIC = record.get(3);
			String key = record.get(4);
			String path_fix = record.get(2);
			String sha_fix = record.get(3);
			if(pathBefore.contains("path before")) continue;
			
			BeforeBIC bbic = new BeforeBIC(pathBefore, pathBIC, shaBefore, shaBIC, path_fix, sha_fix, key);
			bbics.add(bbic);
		}
			
		System.out.println("########### Finish collecting BBIC from local file! ###########");
		return bbics;
	}
	
	/*
	   Copyright 2013, 2014 Dominik Stadler
	   Licensed under the Apache License, Version 2.0 (the "License");
	   you may not use this file except in compliance with the License.
	   You may obtain a copy of the License at
	     http://www.apache.org/licenses/LICENSE-2.0
	   Unless required by applicable law or agreed to in writing, software
	   distributed under the License is distributed on an "AS IS" BASIS,
	   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	   See the License for the specific language governing permissions and
	   limitations under the License.
	 */
	public static void collectFiles(Input input, ArrayList<BeforeBIC> bbics) throws IOException {
		String outPath = "./assets/collectedFiles/";
		OutputStream outputStream;
        try (Repository repo = input.repo) {
            // find the HEAD
            
            // a RevWalk allows to walk over commits based on some filtering that is defined
            try (RevWalk revWalk = new RevWalk(repo)) {
            	int i = 0;
            	for(BeforeBIC bbic: bbics) {
            		ObjectId beforeId = repo.resolve(bbic.shaBefore);
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
                        treeWalk.setFilter(PathFilter.create(bbic.pathBefore));
                        if (!treeWalk.next()) {
                            throw new IllegalStateException("Did not find expected file " + bbic.pathBefore);
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
}




