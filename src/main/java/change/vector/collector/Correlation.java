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
		computeCor(input, "CosSim");
		computeCor(input, "Pearsons");
		computeCor(input, "Spearmans");
		computeCor(input, "Kendalls");
		computeCor(input, "Jaccard");
		computeCor(input, "EuclideanD");
		computeCor(input, "ManhattanD");
		computeCor(input, "Covariance");
		computeCor(input, "CosSim");
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
		
		if(mode.equals("Pearsons")) {
			cor = computePCC(input, dataset, cor);
		} else if(mode.equals("Spearmans")) {
			cor = computeSCC(input, dataset, cor);
		} else if(mode.equals("Kendalls")) {
			cor = computeKCC(input, dataset, cor);
		} else if(mode.equals("Jaccard")) {
			cor = computeJSC(input, dataset, cor);
		} else if(mode.equals("EuclideanD")) {
			cor = computeEucD(input, dataset, cor);
		} else if(mode.equals("ManhattanD")) {
			cor = computeManD(input, dataset, cor);
		} else if(mode.equals("Covariance")) {
			cor = computeCov(input, dataset, cor);
		}  else if(mode.equals("CosSim")) {
			cor = computeCov(input, dataset, cor);
		} 
			

		
		// writing files
		File outFile = new File(input.outFile + mode + "_combined" + ".csv");
//		File outFile = new File(input.outFile + mode + "_"+ input.projectName + ".csv");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
		
	// combined part
		int numOfIgnite = 1658;
		int numOfLucene = 3076;
		int numOfZookeeper = 685;
		
		// index of x-axis
		csvprinter.print(mode);
		for (int i = 0; i < numOfIgnite; i++) {
			csvprinter.print("ignite"+i);
		}
		for (int i = 0; i < numOfLucene; i++) {
			csvprinter.print("lucene"+i);
		}
		for (int i = 0; i < numOfZookeeper; i++) {
			csvprinter.print("zookeeper"+i);
		}
		csvprinter.println();

		// writing data
		for(int i = 0, lucene = 0, zookeeper = 0; i < dataset.numInstances(); i++) {
			if(i < numOfIgnite) {
				csvprinter.print("ignite"+i);
			} else if(i<(numOfIgnite+numOfLucene)) {
				csvprinter.print("lucene"+(lucene++));
			} else {
				csvprinter.print("zookeeper"+(zookeeper++));
			}
			for(int j = 0; j < dataset.numInstances(); j++) {
				csvprinter.print(cor.get(i).get(j));
			}
			csvprinter.println();
		}
	// combined part
		
		
		
	// one-by-one
		// index of x-axis
//		csvprinter.print(mode);
//		for (int i = 0; i < dataset.numInstances(); i++) {
//			csvprinter.print(i);
//		}
//		csvprinter.println();
//		
//		for(int i = 0; i < dataset.numInstances(); i++) {
//			csvprinter.print(i);
//			for(int j = 0; j < dataset.numInstances(); j++) {
//				csvprinter.print(cor.get(i).get(j));
//			}
//			csvprinter.println();
//		}
	// one-bye-one
		
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
	
	public static ArrayList<ArrayList<Double>> computeCOS(Input input, Instances dataset, ArrayList<ArrayList<Double>> cor) throws Exception {
		// computing Jaccard Similarity Coefficient one by one
		for(int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			for(int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
				cor.get(i).set(j, cosineSimilarity(x, y));
			}
		}		
		return cor;
	}
	
	public static double cosineSimilarity(double[] vectorA, double[] vectorB) {
	    double dotProduct = 0.0;
	    double normA = 0.0;
	    double normB = 0.0;
	    for (int i = 0; i < vectorA.length; i++) {
	        dotProduct += vectorA[i] * vectorB[i];
	        normA += Math.pow(vectorA[i], 2);
	        normB += Math.pow(vectorB[i], 2);
	    }   
	    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
	}
}
