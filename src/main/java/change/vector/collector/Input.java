package change.vector.collector;

import java.io.File;
import java.io.IOException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Repository;

public class Input {
	
	public String url = "";
	public String inFile = "";
	public String outFile = "";
	public String bbicFilePath = "";
	public Repository repo = null;
	public final String REMOTE_URI;
	public final String projectName;
	public Git git;
	
	
	public Input(String url, String inFile, String outFile)	throws InvalidRemoteException, TransportException, GitAPIException, IOException {
		this.url = url;
		this.inFile = inFile;
		this.outFile = outFile;
		this.REMOTE_URI = url + ".git";
		this.projectName = Utils.getProjectName(REMOTE_URI);
		if (!outFile.endsWith(File.separator))
			outFile += File.separator;
//		this.git = Utils.gitClone(REMOTE_URI);
//		this.repo = git.getRepository();
		this.bbicFilePath = "./assets/BBIC_" + projectName + ".csv";
	}

}
