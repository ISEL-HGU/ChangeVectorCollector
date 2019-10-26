package change.vector.collector;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.RepositoryNotFoundException;


public class Utils {

	static public DiffAlgorithm diffAlgorithm = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.MYERS);
	static public RawTextComparator diffComparator = RawTextComparator.WS_IGNORE_ALL;


	public static Git gitClone(String REMOTE_URI)
			throws InvalidRemoteException, TransportException, GitAPIException, IOException {

		File repositoriesDir = new File("repositories" + File.separator + getProjectName(REMOTE_URI));
		Git git = null;
		if (repositoriesDir.exists()) {
			try {
				git = Git.open(repositoriesDir);
			} catch(RepositoryNotFoundException e) {
				if(repositoriesDir.delete()) {
					return gitClone(REMOTE_URI);
				}
			}
		} else {
			repositoriesDir.mkdirs();
			System.out.println("cloning..");
			git = Git.cloneRepository().setURI(REMOTE_URI).setDirectory(repositoriesDir)
//				  .setBranch("refs/heads/master") // only master
					.setCloneAllBranches(true).call();
		}
		return git;
	}

	public static String getProjectName(String URI) {

		Pattern p = Pattern.compile(".*/(.+)\\.git");
		Matcher m = p.matcher(URI);
		m.find();
//		System.out.println(m.group(1));
		return m.group(1);

	}
	
}
