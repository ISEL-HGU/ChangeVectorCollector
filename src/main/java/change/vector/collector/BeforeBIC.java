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
	public String pathBefore;
	public String pathBIC;
	public String shaBefore;
	public String shaBIC;
	public String pathFix;
	public String shaFix;
	public String key;

	public BeforeBIC(String pathBefore, String pathBIC, String shaBefore, String shaBIC, String pathFix, String shaFix,
			String key) {
		this.pathBefore = pathBefore;
		this.pathBIC = pathBIC;
		this.shaBefore = shaBefore;
		this.shaBIC = shaBIC;
		this.pathFix = pathFix;
		this.shaFix = shaFix;
		this.key = key;
	}

	public static void writeBBICsOnCSV(Input input, ArrayList<BeforeBIC> bbics) throws IOException {
		writeBBICsOnCSV(input, bbics, "BBIC_"+input.projectName+".csv");
	}
	
	public static void writeBBICsOnCSV(Input input, ArrayList<BeforeBIC> bbics, String out_path) throws IOException {
		
		final String[] headers = { "index", "path_before", "path_BIC", "sha_before", "sha_BIC", "path_fix", "sha_fix",
				"key", "url" };
		File fileP = new File(input.outFile+out_path);
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileP.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers));
		
		int index = 0;
		for(BeforeBIC bbic: bbics) {
			// writing the BBIC file
			csvprinter.printRecord(input.projectName + index, bbic.pathBefore, bbic.pathBIC, bbic.shaBefore,
					bbic.shaBIC, bbic.pathFix, bbic.shaFix, bbic.key, input.url);
			csvprinter.flush();
			index++;
		}
		
		csvprinter.close();
		return;
	}
	
	@Override
	public String toString() {
		return key + "\n";
	}
}
