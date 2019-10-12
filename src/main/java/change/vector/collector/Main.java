package change.vector.collector;

//import org.apache.commons.cli.Options;
import change.vector.collector.Input;
import change.vector.collector.Collector;
import change.vector.collector.ChangeVector;
import org.eclipse.jgit.api.errors.GitAPIException;
import java.io.IOException;
import java.util.ArrayList;

public class Main {

	public static void main(String[] args) throws IOException, GitAPIException{
		ArrayList<BeforeBIC> bbics = new ArrayList<BeforeBIC>();
	
		Input input = new Input("https://github.com/apache/zookeeper",
				"/Users/jihoshin/eclipse-workspace/change-vector-collector/assets/BIC_zookeeper.csv",
				"/Users/jihoshin/eclipse-workspace/change-vector-collector/assets/");
				
		// collect bbic from git repository
//		bbics = Collector.collectBeforeBIC(input);
		
		// collect bbic from csv file
		String bbicFilePath = "/Users/jihoshin/eclipse-workspace/change-vector-collector/assets/zookeeper_BBIC.csv";
		bbics = Collector.collectBeforeBICFromLocalFile(input, bbicFilePath);
		
		// collect java files of bbic of bic
		Collector.collectFiles(input, bbics);
		
		// perform Gumtree to retrieve change vector
		ChangeVector.runGumtreeDIST();
	}

}
