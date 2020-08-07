package change.vector.collector;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class Main {
	Input input = null;
	boolean is_help = false;
	public static boolean is_repo = false;
	public static boolean is_local = false;
	public static boolean is_correlation = false;
	public static boolean is_all = false;
	public static boolean is_precfix = false;
	public static boolean is_gumtree = false;
	public static boolean is_string = false;
	public static boolean is_defects4j = false;

	public static void main(String[] args) throws Exception {
		Main cvc = new Main();
		cvc.run(args);
	}

	public void run(String[] args) throws Exception {
		Options options = createOptions();
		ArrayList<BeforeBIC> bbics = new ArrayList<BeforeBIC>();

		if (parseOptions(options, args)) {
			if (is_help)
				printHelp(options);

			// collects all changes in a repository -a
			if (is_all) {
				bbics = Collector.getAllCommits(input);
				BeforeBIC.writeBBICsOnCSV(input, bbics, "BBIC_all_" + input.projectName + ".csv");
				return;
			}

			// compute correlations -c
			if (is_correlation) {
				Correlation.computeAll(input);
				return;
			}

			// collect bbic from git repository -r
			if (is_repo) {
				bbics = Collector.collectBeforeBIC(input);
				bbics = Collector.rmDups(bbics, input);
			}

			// collect bbic from .csv file -l
			if (is_local) {
				bbics = Collector.collectBeforeBICFromLocalFile(input);
				Precfix.runPrecfix(input, bbics);
				return;
			}

			// get Precfix results -p
			if (is_precfix) {
				bbics = Collector.collectBeforeBICFromLocalFile(input);
				Precfix.runPrecfix(input, bbics);
				return;
			}

			// get AST vectors with ordering using GumTree -g
			if (is_gumtree) {
				String inputFile = input.outDirectory + "BBIC_" + input.projectName + ".csv";
				File bbicFile = new File(inputFile);

				if (bbicFile.exists()) {
					bbics = Collector.collectBeforeBICFromLocalFile(input, inputFile);
				} else {
					bbics = Collector.collectBeforeBIC(input);
				}
				Gumtree.runGumtree(input, bbics);
				return;
			}

			// get gumtree vectors from defects4j instances -d
			if (is_defects4j) {
				File bicFile = new File(input.inputDirectory + "BIC_d4j_" + input.projectName + ".csv");
				if (!bicFile.exists()) {
					Gumtree.runD4j3(input);
				}

				File bbicFile = new File(input.inputDirectory + "BBIC_d4j_" + input.projectName + ".csv");
				if (bbicFile.exists()) {
					bbics = Collector.collectBeforeBICFromLocalFile(input,
							input.inputDirectory + "BBIC_d4j_" + input.projectName + ".csv");
				} else {
					bbics = Collector.collectBeforeBIC(input);
				}
				Gumtree.runGumtree(input, bbics);

				System.out.println("run d4j complete!");
				return;
			}

			// get string data of commit -s
			if (is_string) {
				ArrayList<BeforeBIC> new_bbics = new ArrayList<BeforeBIC>();
				FileWriter writer = new FileWriter(input.outDirectory + "S_" + input.projectName + ".txt");

				String inputFile = input.outDirectory + "BBIC_" + input.projectName + ".csv";
				File bbicFile = new File(inputFile);
				if (bbicFile.exists()) {
					bbics = Collector.collectBeforeBICFromLocalFile(input, inputFile);
				} else {
					bbics = Collector.collectBeforeBIC(input);
				}
				for (BeforeBIC bbic : bbics) {
					String bic = DefectPatchPair.getBICcode(input.repo, bbic, input);
					if (bic == null)
						continue;
					new_bbics.add(bbic);
					writer.write(bic + "\n");
				}

				BeforeBIC.writeBBICsOnCSV(input, new_bbics, "YS_" + input.projectName + ".csv");

				writer.close();

				System.out.println("writing strings done");
				return;
			}

			// collect java files of bbic of bic
//			Collector.collectFiles(input, bbics);

			// perform Gumtree to retrieve change vector
//			ChangeVector.runGumtreeDIST(input, bbics);
		}
	}

	private boolean parseOptions(Options options, String[] args) {
		CommandLineParser parser = new DefaultParser();
		String in;
		String out;
		String url;

		try {
			CommandLine cmd = parser.parse(options, args);
			try {
				if (cmd.hasOption("c"))
					is_correlation = true;
				else if (cmd.hasOption("r"))
					is_repo = true;
				else if (cmd.hasOption("l"))
					is_local = true;
				else if (cmd.hasOption("a"))
					is_all = true;
				else if (cmd.hasOption("p"))
					is_precfix = true;
				else if (cmd.hasOption("g"))
					is_gumtree = true;
				else if (cmd.hasOption("s"))
					is_string = true;
				else if (cmd.hasOption("d"))
					is_defects4j = true;

				in = cmd.getOptionValue("i");
				out = cmd.getOptionValue("o");
				url = cmd.getOptionValue("u");

			} catch (Exception e) {
				System.out.println(e.getMessage());
				printHelp(options);
				return false;
			}

			input = new Input(url, in, out);
		} catch (Exception e) {
			e.printStackTrace();
			printHelp(options);
			return false;
		}
		return true;
	}

	private Options createOptions() {
		Options options = new Options();

		options.addOption(Option.builder("s")
					  			.longOpt("string retrieval")
					  			.desc("mining commit as string")
					  			.build());

		options.addOption(Option.builder("d")
					  			.longOpt("defects4j")
					  			.desc("run Gumtree vectors for defects4j instances")
					  			.build());

		options.addOption(Option.builder("g")
					  			.longOpt("gumtree")
					  			.desc("run Gumtree")
					  			.build());

		options.addOption(Option.builder("p")
								.longOpt("precfix")
								.desc("run PRECFIX")
								.build());

		options.addOption(Option.builder("a")
								.longOpt("all")
								.desc("Collects all changes in a repo")
								.build());

		options.addOption(Option.builder("c")
								.longOpt("correlation")
								.desc("Computes correlation of each vectors")
								.build());

		options.addOption(Option.builder("r")
								.longOpt("repo")
								.desc("Collect change vectors straight from Git repository")
								.build());

		options.addOption(Option.builder("l")
								.longOpt("local")
								.desc("Collect change vectors with BBIC file in local")
								.build());

		options.addOption(Option.builder("u")
								.longOpt("url")
								.desc("url of the git repo")
								.hasArg()
								.argName("git_url")
								.required()
								.build());

		options.addOption(Option.builder("i")
								.longOpt("input")
								.desc("directory of the input file to parse")
								.hasArg()
								.argName("input_path")
								.required()
								.build());

		options.addOption(Option.builder("o")
								.longOpt("output")
								.desc("directory will have result file")
								.hasArg()
								.argName("output_path")
								.required()
								.build());

		options.addOption(Option.builder("h")
								.longOpt("help")
								.desc("Help")
								.build());

		return options;
	}

	private void printHelp(Options options) {
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		String header = "Collects change vectors from 2 files";
		String footer = "\nPlease report issues at https://github.com/HGUISEL/ChangeVectorCollector/issues";
		formatter.printHelp("ChangeVectorCollector", header, options, footer, true);
	}
}
