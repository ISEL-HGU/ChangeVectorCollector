package change.vector.collector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class BeforeBIC {
	public String pathBeforeBIC;
	public String pathBIC;
	public String shaBeforeBIC;
	public String shaBIC;
	public String pathBeforeBFC;
	public String pathBFC;
	public String shaBeforeBFC;
	public String shaBFC;
	public String key;
	public String project;

	public BeforeBIC(String pathBeforeBIC, String pathBIC, 
					 String shaBeforeBIC, String shaBIC,
					 String pathBeforeBFC, String pathBFC,
					 String shaBeforeBFC, String shaBFC,
					 String key, String project) {
		this.pathBeforeBIC = pathBeforeBIC;
		this.pathBIC = pathBIC;
		this.shaBeforeBIC = shaBeforeBIC;
		this.shaBIC = shaBIC;
		this.pathBeforeBFC = pathBeforeBFC;
		this.pathBFC = pathBFC;
		this.shaBeforeBFC = shaBeforeBFC;
		this.shaBFC = shaBFC;
		this.key = key;
		this.project = project;
	}

	public static void writeBBICsOnCSV(Input input, ArrayList<BeforeBIC> bbics) throws IOException {
		writeBBICsOnCSV(input, bbics, "BBIC_" + input.projectName + ".csv");
	}

	public static void writeBBICsOnCSV(Input input, ArrayList<BeforeBIC> bbics, String out_path) throws IOException {

		final String[] headers = { "index",
								   "path_bbic", "path_bic",
								   "sha_bbic", "sha_bic",
								   "path_bbfc", "path_bfc",
								   "sha_bbfc", "sha_bfc",
								   "key", "project" };
		File fileP = new File(input.outFile + out_path);
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileP.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers));

		int index = 0;
		for (BeforeBIC bbic : bbics) {
			// writing the BBIC file
			csvprinter.printRecord(input.projectName + index,
								   bbic.pathBeforeBIC, bbic.pathBIC,
								   bbic.shaBeforeBIC, bbic.shaBIC,
								   bbic.pathBeforeBFC, bbic.pathBFC,
								   bbic.shaBeforeBFC, bbic.shaBFC,
								   bbic.key, bbic.project);
			csvprinter.flush();
			index++;
		}

		csvprinter.close();
		return;
	}

	@Override
	public String toString() {
		return pathBeforeBIC + "\n" + pathBIC + "\n" +
			   shaBeforeBIC + "\n" + shaBIC + "\n" +
			   pathBeforeBIC + "\n" + pathBFC + "\n" +
			   shaBeforeBFC + "\n" + shaBFC + "\n" + project;
	}
}
