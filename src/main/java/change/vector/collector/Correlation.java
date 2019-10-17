package change.vector.collector;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.apache.commons.math3.ml.distance.ManhattanDistance;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.commons.math3.stat.correlation.KendallsCorrelation;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.text.similarity.JaccardDistance;
import org.apache.commons.text.similarity.JaccardSimilarity;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class Correlation {
	
	public static void computeAll(Input input) throws Exception {
		computePCC(input);
		computeSCC(input);
		computeKCC(input);
		computeED(input);
		computeCovariance(input);
		System.out.println("writing all corelations done!");
		
	}

	public static void computePCC(Input input) throws Exception {
		String filePath = input.inFile;
		
		DataSource source = null;
		Instances dataset = null;
		source = new DataSource(filePath);
		dataset = source.getDataSet();
		
		if(dataset.classIndex() == -1)
			dataset.setClassIndex(dataset.numAttributes() - 1);
		
		System.out.println("num of att: " + dataset.numAttributes());
		System.out.println("num of inst " + dataset.numInstances());
		// System.out.println(dataset);
		
		// double[][] instantiation
		ArrayList<ArrayList<Double>> pcc = new ArrayList<ArrayList<Double>>();
		for(int i = 0; i < dataset.numInstances(); i++) {
			pcc.add(new ArrayList<Double>());
			for(int j = 0; j < dataset.numInstances(); j++) {
				pcc.get(i).add(0.0);
			}
		}
		
		// computing PCC one by one
		for(int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			for(int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
			    pcc.get(i).set(j, new PearsonsCorrelation().correlation(x,y));
			}
		}
		
		// writing files
		File outFile = new File(input.outFile+"PeasonsCC.csv");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
		for(int i = 0; i < dataset.numInstances(); i++) {
			for(int j = 0; j < dataset.numInstances(); j++) {
				csvprinter.print(pcc.get(i).get(j));
				
			}
			csvprinter.println();
		}
		System.out.println("writing pcc done!");
		csvprinter.close();
	}
	
	public static void computeSCC(Input input) throws Exception {
		String filePath = input.inFile;
		
		DataSource source = null;
		Instances dataset = null;
		source = new DataSource(filePath);
		dataset = source.getDataSet();
		
		if(dataset.classIndex() == -1)
			dataset.setClassIndex(dataset.numAttributes() - 1);
		
		System.out.println("num of att: " + dataset.numAttributes());
		System.out.println("num of inst " + dataset.numInstances());
		// System.out.println(dataset);
		
		// double[][] instantiation
		ArrayList<ArrayList<Double>> pcc = new ArrayList<ArrayList<Double>>();
		for(int i = 0; i < dataset.numInstances(); i++) {
			pcc.add(new ArrayList<Double>());
			for(int j = 0; j < dataset.numInstances(); j++) {
				pcc.get(i).add(0.0);
			}
		}
		
		// computing SCC one by one
		for(int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			for(int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
			    pcc.get(i).set(j, new SpearmansCorrelation().correlation(x,y));
			}
		}
		
		// writing files
		File outFile = new File(input.outFile+"SpearmansCC.csv");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
		for(int i = 0; i < dataset.numInstances(); i++) {
			for(int j = 0; j < dataset.numInstances(); j++) {
				csvprinter.print(pcc.get(i).get(j));
				
			}
			csvprinter.println();
		}
		System.out.println("writing scc done!");
		csvprinter.close();
	}
	
	public static void computeKCC(Input input) throws Exception {
		String filePath = input.inFile;
		
		DataSource source = null;
		Instances dataset = null;
		source = new DataSource(filePath);
		dataset = source.getDataSet();
		
		if(dataset.classIndex() == -1)
			dataset.setClassIndex(dataset.numAttributes() - 1);
		
		System.out.println("num of att: " + dataset.numAttributes());
		System.out.println("num of inst " + dataset.numInstances());
		// System.out.println(dataset);
		
		// double[][] instantiation
		ArrayList<ArrayList<Double>> pcc = new ArrayList<ArrayList<Double>>();
		for(int i = 0; i < dataset.numInstances(); i++) {
			pcc.add(new ArrayList<Double>());
			for(int j = 0; j < dataset.numInstances(); j++) {
				pcc.get(i).add(0.0);
			}
		}
		
		// computing KCC one by one
		for(int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			for(int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
			    pcc.get(i).set(j, new KendallsCorrelation().correlation(x,y));
			}
		}
		
		// writing files
		File outFile = new File(input.outFile+"KendallsCC.csv");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
		for(int i = 0; i < dataset.numInstances(); i++) {
			for(int j = 0; j < dataset.numInstances(); j++) {
				csvprinter.print(pcc.get(i).get(j));
				
			}
			csvprinter.println();
		}
		System.out.println("writing kcc done!");
		csvprinter.close();
	}
	
	public static void computeJSC(Input input) throws Exception {
		String filePath = input.inFile;
		
		DataSource source = null;
		Instances dataset = null;
		source = new DataSource(filePath);
		dataset = source.getDataSet();
		
		if(dataset.classIndex() == -1)
			dataset.setClassIndex(dataset.numAttributes() - 1);
		
		System.out.println("num of att: " + dataset.numAttributes());
		System.out.println("num of inst " + dataset.numInstances());
		// System.out.println(dataset);
		
		// double[][] instantiation
		ArrayList<ArrayList<Double>> pcc = new ArrayList<ArrayList<Double>>();
		for(int i = 0; i < dataset.numInstances(); i++) {
			pcc.add(new ArrayList<Double>());
			for(int j = 0; j < dataset.numInstances(); j++) {
				pcc.get(i).add(0.0);
			}
		}
		
		// computing KCC one by one
		for(int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			CharSequence csx = Arrays.toString(x);
			for(int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
				CharSequence csy = Arrays.toString(y);
			    pcc.get(i).set(j, new JaccardSimilarity().apply(csx, csy));
			}
		}
		
		// writing files
		File outFile = new File(input.outFile+"jsc.csv");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
		for(int i = 0; i < dataset.numInstances(); i++) {
			for(int j = 0; j < dataset.numInstances(); j++) {
				csvprinter.print(pcc.get(i).get(j));
				
			}
			csvprinter.println();
		}
		System.out.println("writing JSC done!");
		csvprinter.close();
	}
	
	public static void computeED(Input input) throws Exception {
		String filePath = input.inFile;
		
		DataSource source = null;
		Instances dataset = null;
		source = new DataSource(filePath);
		dataset = source.getDataSet();
		
		if(dataset.classIndex() == -1)
			dataset.setClassIndex(dataset.numAttributes() - 1);
		
		System.out.println("num of att: " + dataset.numAttributes());
		System.out.println("num of inst " + dataset.numInstances());
		// System.out.println(dataset);
		
		// double[][] instantiation
		ArrayList<ArrayList<Double>> pcc = new ArrayList<ArrayList<Double>>();
		for(int i = 0; i < dataset.numInstances(); i++) {
			pcc.add(new ArrayList<Double>());
			for(int j = 0; j < dataset.numInstances(); j++) {
				pcc.get(i).add(0.0);
			}
		}
		
		// computing Covariance one by one
		for(int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			for(int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
			    pcc.get(i).set(j, new EuclideanDistance().compute(x,y));
			}
		}
		
		// writing files
		File outFile = new File(input.outFile+"Euclidean_distance.csv");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
		for(int i = 0; i < dataset.numInstances(); i++) {
			for(int j = 0; j < dataset.numInstances(); j++) {
				csvprinter.print(pcc.get(i).get(j));
				
			}
			csvprinter.println();
		}
		System.out.println("writing Euclidean Distance done!");
		csvprinter.close();
	}
	
	public static void computeMD(Input input) throws Exception {
		String filePath = input.inFile;
		
		DataSource source = null;
		Instances dataset = null;
		source = new DataSource(filePath);
		dataset = source.getDataSet();
		
		if(dataset.classIndex() == -1)
			dataset.setClassIndex(dataset.numAttributes() - 1);
		
		System.out.println("num of att: " + dataset.numAttributes());
		System.out.println("num of inst " + dataset.numInstances());
		// System.out.println(dataset);
		
		// double[][] instantiation
		ArrayList<ArrayList<Double>> pcc = new ArrayList<ArrayList<Double>>();
		for(int i = 0; i < dataset.numInstances(); i++) {
			pcc.add(new ArrayList<Double>());
			for(int j = 0; j < dataset.numInstances(); j++) {
				pcc.get(i).add(0.0);
			}
		}
		
		// computing Covariance one by one
		for(int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			for(int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
			    pcc.get(i).set(j, new ManhattanDistance().compute(x,y));
			}
		}
		
		// writing files
		File outFile = new File(input.outFile+"Manhattan_distance.csv");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
		for(int i = 0; i < dataset.numInstances(); i++) {
			for(int j = 0; j < dataset.numInstances(); j++) {
				csvprinter.print(pcc.get(i).get(j));
				
			}
			csvprinter.println();
		}
		System.out.println("writing Manhattan Distance done!");
		csvprinter.close();
	}
	
	public static void computeCovariance(Input input) throws Exception {
		String filePath = input.inFile;
		
		DataSource source = null;
		Instances dataset = null;
		source = new DataSource(filePath);
		dataset = source.getDataSet();
		
		if(dataset.classIndex() == -1)
			dataset.setClassIndex(dataset.numAttributes() - 1);
		
		System.out.println("num of att: " + dataset.numAttributes());
		System.out.println("num of inst " + dataset.numInstances());
		// System.out.println(dataset);
		
		// double[][] instantiation
		ArrayList<ArrayList<Double>> pcc = new ArrayList<ArrayList<Double>>();
		for(int i = 0; i < dataset.numInstances(); i++) {
			pcc.add(new ArrayList<Double>());
			for(int j = 0; j < dataset.numInstances(); j++) {
				pcc.get(i).add(0.0);
			}
		}
		
		// computing Covariance one by one
		for(int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			for(int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
			    pcc.get(i).set(j, new Covariance().covariance(x,y));
			}
		}
		
		// writing files
		File outFile = new File(input.outFile+"Covariance.csv");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
		for(int i = 0; i < dataset.numInstances(); i++) {
			for(int j = 0; j < dataset.numInstances(); j++) {
				csvprinter.print(pcc.get(i).get(j));
				
			}
			csvprinter.println();
		}
		System.out.println("writing Covariance done!");
		csvprinter.close();
	}
}
