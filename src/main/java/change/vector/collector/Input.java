package change.vector.collector;

import java.io.File;
import java.io.IOException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Repository;

public class Input {
	
	public static String bicFilePath = "./data/SimFin/BIC/";
	public static String bbicFilePath = "./data/SimFin/BBIC/";
	public static String allFilePath = "./data/SimFin/All/";
	public static String cleanFilePath = "./data/SimFin/Clean/";
	public static String gumvecFilePath = "./data/SimFin/GumVecs/";
	public static String stringFilePath = "./data/SimFin/String/";
	public static String changevecFilePath = "./data/SimFin/String/";
	public static String d4jFilePath = "./data/SimFin/d4j/";
	
	public String url = "";
	public Repository repo = null;
	public final String REMOTE_URI;
	public final String projectName;
	public Git git;
	public File gitDir;

	public Input(String url)
			throws InvalidRemoteException, TransportException, GitAPIException, IOException {
		this.url = url;
		this.REMOTE_URI = url + ".git";
		this.projectName = Utils.getProjectName(REMOTE_URI);
		if (Utils.isCloned(this)) {
			this.gitDir = Utils.getGitDirectory(this);
		} else {
			this.gitDir = Utils.GitClone(this);
		}
		this.git = Git.open(gitDir);
		this.repo = git.getRepository();
	}

}
