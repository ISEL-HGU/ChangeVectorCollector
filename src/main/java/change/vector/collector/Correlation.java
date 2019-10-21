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
import org.apache.commons.text.similarity.JaccardSimilarity;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class Correlation {
	
	public static void computeAll(Input input) throws Exception {
		computeCor(input, "PearsonsCC");
		computeCor(input, "SpearmansCC");
		computeCor(input, "KendallsCC");
		computeCor(input, "JaccardSC");
		computeCor(input, "Euclidean_distance");
		computeCor(input, "Manhattan_distance");
		computeCor(input, "Covariance");
		System.out.println("writing all correlations done!");
	}
	
	public static void computeCor(Input input, String mode) throws Exception{
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
		ArrayList<ArrayList<Double>> cor = new ArrayList<ArrayList<Double>>();
		for(int i = 0; i < dataset.numInstances(); i++) {
			cor.add(new ArrayList<Double>());
			for(int j = 0; j < dataset.numInstances(); j++) {
				cor.get(i).add(0.0);
			}
		}
		
		if(mode.equals("PearsonsCC")) {
			cor = computePCC(input, dataset, cor);
		} else if(mode.equals("SpearmansCC")) {
			cor = computeSCC(input, dataset, cor);
		} else if(mode.equals("KendallsCC")) {
			cor = computeKCC(input, dataset, cor);
		} else if(mode.equals("JaccardSC")) {
			cor = computeJSC(input, dataset, cor);
		} else if(mode.equals("Euclidean_distance")) {
			cor = computeEucD(input, dataset, cor);
		} else if(mode.equals("Manhattan_distance")) {
			cor = computeManD(input, dataset, cor);
		} else if(mode.equals("Covariance")) {
			cor = computeCov(input, dataset, cor);
		} 
			
		// writing files
		File outFile = new File(input.outFile + mode + "_"+ input.projectName + ".csv");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
		for(int i = 0; i < dataset.numInstances(); i++) {
			for(int j = 0; j < dataset.numInstances(); j++) {
				csvprinter.print(cor.get(i).get(j));
				
			}
			csvprinter.println();
		}
		System.out.println("writing " + mode + " done!");
		csvprinter.close();
	}

	public static ArrayList<ArrayList<Double>> computePCC(Input input, Instances dataset, ArrayList<ArrayList<Double>> cor) throws Exception {
		// computing Pearsons Correlation Coefficient one by one
		for(int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			for(int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
			    cor.get(i).set(j, new PearsonsCorrelation().correlation(x,y));
			}
		}		
		return cor;
	}
	
	public static ArrayList<ArrayList<Double>> computeSCC(Input input, Instances dataset, ArrayList<ArrayList<Double>> cor) throws Exception {
		// computing Spearmans Correlation Coefficient one by one
		for(int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			for(int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
			    cor.get(i).set(j, new SpearmansCorrelation().correlation(x,y));
			}
		}		
		return cor;
	}
	
	public static ArrayList<ArrayList<Double>> computeKCC(Input input, Instances dataset, ArrayList<ArrayList<Double>> cor) throws Exception {
		// computing Kendalls Correlation Coefficient one by one
		for(int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			for(int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
			    cor.get(i).set(j, new KendallsCorrelation().correlation(x,y));
			}
		}		
		return cor;
	}
	
	public static ArrayList<ArrayList<Double>> computeJSC(Input input, Instances dataset, ArrayList<ArrayList<Double>> cor) throws Exception {
		// computing Jaccard Similarity Coefficient one by one
		for(int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			CharSequence csx = Arrays.toString(x);
			for(int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
				CharSequence csy = Arrays.toString(y);
				cor.get(i).set(j, new JaccardSimilarity().apply(csx, csy));
			}
		}		
		return cor;
	}
	
	public static ArrayList<ArrayList<Double>> computeEucD(Input input, Instances dataset, ArrayList<ArrayList<Double>> cor) throws Exception {
		// computing Euclidean Distance one by one
		for(int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			for(int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
			    cor.get(i).set(j, new EuclideanDistance().compute(x,y));
			}
		}		
		return cor;
	}
	
	public static ArrayList<ArrayList<Double>> computeManD(Input input, Instances dataset, ArrayList<ArrayList<Double>> cor) throws Exception {
		// computing Manhattan Distance one by one
		for(int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			for(int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
			    cor.get(i).set(j, new ManhattanDistance().compute(x,y));
			}
		}		
		return cor;
	}
	
	public static ArrayList<ArrayList<Double>> computeCov(Input input, Instances dataset, ArrayList<ArrayList<Double>> cor) throws Exception {
		// computing Covariance one by one
		for(int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			for(int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
			    cor.get(i).set(j, new Covariance().covariance(x,y));
			}
		}		
		return cor;
	}
}
