package change.vector.collector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
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

	// runner for all
	public static void computeAll(Input input) throws Exception {
		if (input.inFile.contains("test")) {
			//calcCorrelationAxB(input, "Pearsons");
			calcCorrelationAxB(input, "Kendalls");
			//calcCorrelationAxB(input, "EuclideanD");
			System.out.println("testing all correlations done!");
		} else {
			calcCorrelationAxA(input, "Pearsons");
			calcCorrelationAxA(input, "Kendalls");
			calcCorrelationAxA(input, "EuclideanD");
			// calcCorrelationAxA(input, "Spearmans");
			// calcCorrelationAxA(input, "Jaccard");
			// calcCorrelationAxA(input, "ManhattanD");
			// calcCorrelationAxA(input, "Covariance");
			// calcCorrelationAxA(input, "CosSim");
			System.out.println("writing all correlations done!");
		}
	}

	
	// calculates correlation of two different set of vectors (e.g. train x test)
	public static void calcCorrelationAxB(Input input, String mode) throws Exception {
		String trainPath = "./assets/test/database3.arff";
		String testPath = input.inFile;

		DataSource trainSource = new DataSource(trainPath);
		DataSource testSource = new DataSource(testPath);
		Instances trainset = trainSource.getDataSet();
		Instances testset = testSource.getDataSet();

		if (trainset.classIndex() == -1)
			trainset.setClassIndex(trainset.numAttributes() - 1);
		if (testset.classIndex() == -1)
			testset.setClassIndex(testset.numAttributes() - 1);

		int attrNum = trainset.numAttributes();
		int trainSize = trainset.numInstances();
		int testSize = testset.numInstances();

		System.out.println("<Trainset>");
		System.out.println("num of att: " + trainset.numAttributes());
		System.out.println("num of inst " + trainSize);
		System.out.println();
		System.out.println("<Testset>");
		System.out.println("num of att: " + testset.numAttributes());
		System.out.println("num of inst " + testSize);

		// init correlation matrix
		ArrayList<ArrayList<Double>> cor = new ArrayList<ArrayList<Double>>();
		for (int i = 0; i < testSize; i++) {
			cor.add(new ArrayList<Double>());
			for (int j = 0; j < trainSize; j++) {
				cor.get(i).add(0.0);
			}
		}

		// init train_arr
		ArrayList<double[]> train_arr = new ArrayList<double[]>();
		for (int i = 0; i < trainSize; i++) {
			train_arr.add(new double[attrNum]);
		}
		for (int i = 0; i < trainSize; i++) {
			train_arr.set(i, trainset.get(i).toDoubleArray());
		}

		// removing changeVector info before computing correlation coefficients
		ArrayList<double[]> train_rm = new ArrayList<double[]>();
		for (int i = 0; i < trainSize; i++) {
			train_rm.add(new double[attrNum - 4]);

		}
		for (int i = 0; i < trainSize; i++) {
			for (int j = 4; j < attrNum - 4; j++) {
				train_rm.get(i)[j] = train_arr.get(i)[j];
			}
		}

		// init test_arr
		ArrayList<double[]> test_arr = new ArrayList<double[]>();
		for (int i = 0; i < testset.numInstances(); i++) {
			test_arr.add(new double[testset.numAttributes()]);
		}
		for (int i = 0; i < testset.numInstances(); i++) {
			test_arr.set(i, testset.get(i).toDoubleArray());
		}
		// removing changeVector info before computing correlation coefficients
		ArrayList<double[]> test_rm = new ArrayList<double[]>();
		for (int i = 0; i < testSize; i++) {
			test_rm.add(new double[attrNum - 4]);

		}
		for (int i = 0; i < testSize; i++) {
			for (int j = 4; j < attrNum - 4; j++) {
				test_rm.get(i)[j] = test_arr.get(i)[j];
			}
		}

		if (mode.equals("Pearsons")) {
			//cor = calcPearsons(input, trainset, testset, cor);
			cor = calcPearsonsExclude0(input, train_rm, test_rm, cor);
		} else if (mode.equals("Kendalls")) {
			//cor = calcKendalls(input, trainset, testset, cor);
			cor = calcKendallsExclude0(input, train_rm, test_rm, cor);
		} else if (mode.equals("EuclideanD")) {
			//cor = calcEuclidean(input, trainset, testset, cor);
			cor = calcEuclideanExclude0(input, train_rm, test_rm, cor);
		}

		writeMultiAxB(input, mode, trainSize, testSize, cor);
		System.out.println("\nWriting " + mode + " done!\n");
	}

	
	// calculates correlation of instances itself (e.g. train x test)
	public static void calcCorrelationAxA(Input input, String mode) throws Exception {

		String filePath = input.inFile;
		DataSource source = new DataSource(filePath);
		Instances dataset = source.getDataSet();

		if (dataset.classIndex() == -1)
			dataset.setClassIndex(dataset.numAttributes() - 1);

		System.out.println("num of att: " + dataset.numAttributes());
		System.out.println("num of inst " + dataset.numInstances());
		// System.out.println(dataset);

		// double[][] instantiation
		ArrayList<ArrayList<Double>> cor = new ArrayList<ArrayList<Double>>();
		for (int i = 0; i < dataset.numInstances(); i++) {
			cor.add(new ArrayList<Double>());
			for (int j = 0; j < dataset.numInstances(); j++) {
				cor.get(i).add(0.0);
			}
		}

		if (mode.equals("Pearsons")) {
			cor = calcPearsons(input, dataset, cor);
		} else if (mode.equals("Spearmans")) {
			cor = calcSpearmans(input, dataset, cor);
		} else if (mode.equals("Kendalls")) {
			cor = calcKendalls(input, dataset, cor);
		} else if (mode.equals("Jaccard")) {
			cor = calcJaccard(input, dataset, cor);
		} else if (mode.equals("EuclideanD")) {
			cor = calcEuclidean(input, dataset, cor);
		} else if (mode.equals("ManhattanD")) {
			cor = calcManhattan(input, dataset, cor);
		} else if (mode.equals("Covariance")) {
			cor = calcCovariance(input, dataset, cor);
		} else if (mode.equals("CosSim")) {
			cor = calcCosine(input, dataset, cor);
		}

		// writing files
		if (input.inFile.contains("combined")) {
			writeMultiAxA(input, mode, dataset, cor);
		} else {
			writeSingleAxA(input, mode, dataset, cor);
		}

		System.out.println("\nWriting " + mode + " done!\n");
	}
	
	
	// writing the result of correlation computation on csv 
	public static void writeSingleAxA(Input input, String mode, Instances dataset, ArrayList<ArrayList<Double>> cor)
			throws IOException {
		File outFile = new File(input.outFile + mode + "_" + input.projectName + ".csv");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT);

		// index of x-axis
		csvprinter.print(mode);
		for (int i = 0; i < dataset.numInstances(); i++) {
			csvprinter.print(i);
		}
		csvprinter.println();

		for (int i = 0; i < dataset.numInstances(); i++) {
			csvprinter.print(i);
			for (int j = 0; j < dataset.numInstances(); j++) {
				csvprinter.print(cor.get(i).get(j));
			}
			csvprinter.println();
		}
		csvprinter.close();
	}

	
	// writing the result of correlation computation on csv 
	public static void writeMultiAxA(Input input, String mode, Instances dataset, ArrayList<ArrayList<Double>> cor)
			throws IOException {
		File outFile = new File(input.outFile + mode + "_combined" + ".csv");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT);

		// combined part
		int numOfIgnite = 1658;
		int numOfLucene = 3076;
		int numOfZookeeper = 685;

		// index of x-axis
		csvprinter.print(mode);
		for (int i = 0; i < numOfIgnite; i++) {
			csvprinter.print("ignite" + i);
		}
		for (int i = 0; i < numOfLucene; i++) {
			csvprinter.print("lucene-solr" + i);
		}
		for (int i = 0; i < numOfZookeeper; i++) {
			csvprinter.print("zookeeper" + i);
		}
		csvprinter.println();

		// writing data
		for (int i = 0, lucene = 0, zookeeper = 0; i < dataset.numInstances(); i++) {
			if (i < numOfIgnite) {
				csvprinter.print("ignite" + i);
			} else if (i < (numOfIgnite + numOfLucene)) {
				csvprinter.print("lucene-solr" + (lucene++));
			} else {
				csvprinter.print("zookeeper" + (zookeeper++));
			}
			for (int j = 0; j < dataset.numInstances(); j++) {
				csvprinter.print(cor.get(i).get(j));
			}
			csvprinter.println();
		}
		csvprinter.close();
	}
	
	
	// writing the result of correlation computation on csv 
	public static void writeMultiAxB(Input input, String mode, int trainSize, int testSize,
			ArrayList<ArrayList<Double>> cor) throws IOException {
		File outFile = new File(input.outFile + mode + "_test_database" + ".csv");

		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT);

		// combined part
		int trainIgnite = 150;
		int trainLucene = 236;
		int trainZookeeper = 140;
//		int trainFlink = 459;
//		int trainIsis = 124;
//		int trainMahout = 130;
//		int trainOozie = 186;

		// int testIO = 2865;
		// int testLang = 6306;
		// int testMath = 19383;
		int testFlink = 459;
		int testIsis = 124;
		int testMahout = 130;
		//int testOozie = 186;
		
		String test1 = "flink";
		String test2 = "isis";
		String test3 = "mahout";
		String test4 = "oozie";
		
		// writing index of x-axis
		csvprinter.print(mode);
		for (int i = 0; i < trainIgnite; i++) {
			csvprinter.print("ignite" + i);
		}
		for (int i = 0; i < trainLucene; i++) {
			csvprinter.print("lucene-solr" + i);
		}
		for (int i = 0; i < trainZookeeper; i++) {
			csvprinter.print("zookeeper" + i);
		}
		csvprinter.println();

		// writing correlation
		for (int i = 0, isis = 0, mahout = 0, oozie = 0; i < testSize; i++) {
			// writing index of y-axis
			if (i < testFlink) {
				csvprinter.print(test1 + i);
			} else if (i < testFlink + testIsis) {
				csvprinter.print(test2 + (isis++));
			} else if (i < testFlink + testIsis + testMahout){
				csvprinter.print(test3 + (mahout++));
			} else {
				csvprinter.print(test4 + (oozie++));
			}
			// writing the computed correlation
			for (int j = 0; j < trainSize; j++) {
				csvprinter.print(cor.get(i).get(j));
			}
			csvprinter.println();
		}
		csvprinter.close();
	}

	
	// calculates Pearsons for AxA
	public static ArrayList<ArrayList<Double>> calcPearsons(Input input, Instances dataset,
			ArrayList<ArrayList<Double>> cor) throws Exception {
		// Pearsons for single dim
		for (int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			for (int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
				cor.get(i).set(j, new PearsonsCorrelation().correlation(x, y));
			}
		}
		return cor;
	}

	
	// calculates Pearsons AxB fullsize
	public static ArrayList<ArrayList<Double>> calcPearsons(Input input, Instances trainset,
	Instances testset, ArrayList<ArrayList<Double>> cor){
		
		for (int i = 0; i < testset.numInstances(); i++) {
			double test[] = testset.get(i).toDoubleArray();
			for (int j = 0; j < trainset.numInstances(); j++) {
				double train[] = trainset.get(j).toDoubleArray();
				cor.get(i).set(j, new PearsonsCorrelation().correlation(test, train));
			}
			System.out.println(i + "/" + testset.size());
		}
		return cor;
	}
	
	
	// calculates Pearsons AxB remove zeros
	public static ArrayList<ArrayList<Double>> calcPearsonsExclude0(Input input,
	ArrayList<double[]> train_arr, ArrayList<double[]> test_arr,
	ArrayList<ArrayList<Double>> cor) throws Exception {
		int attrNum = train_arr.get(0).length;
		
		for (int i = 0; i < test_arr.size(); i++) {
			for (int j = 0; j < train_arr.size(); j++) {
				
				// make new changeVector that has no both zeros
				ArrayList<Double> excludeCVtrain = new ArrayList<Double>();
				ArrayList<Double> excludeCVtest = new ArrayList<Double>();
				
				// traverse attributes
				for (int k = 0; k < attrNum; k++) {
					// if either test and train has non_zero value, add to the new CV
					if (test_arr.get(i)[k] != 0 || train_arr.get(j)[k] != 0) {
						excludeCVtrain.add(train_arr.get(j)[k]);
						excludeCVtest.add(test_arr.get(i)[k]);
					}
				}
				
				// changing the type to double[]
				double[] train = new double[excludeCVtrain.size()];
				for (int k = 0; k < excludeCVtrain.size(); k++) {
					train[k] = excludeCVtrain.get(k);
				}
				double[] test = new double[excludeCVtest.size()];
				for (int k = 0; k < excludeCVtest.size(); k++) {
					test[k] = excludeCVtest.get(k);
				}
				
				if(excludeCVtrain.size() < 2 && excludeCVtest.size() < 2) {
					cor.get(i).set(j, 0.0);
					continue;
				}
				
				cor.get(i).set(j, new PearsonsCorrelation().correlation(test, train));
			}
			System.out.println(i + "/" + test_arr.size());
		}
		return cor;
	}

	
	// calculates Kendalls AxA
	public static ArrayList<ArrayList<Double>> calcKendalls(Input input, Instances dataset,
			ArrayList<ArrayList<Double>> cor) throws Exception {
		// computing Kendalls Correlation Coefficient one by one
		for (int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			for (int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
				cor.get(i).set(j, new KendallsCorrelation().correlation(x, y));
			}
		}
		return cor;
	}
	
	
	// calculates Kendalls AxB full size
	public static ArrayList<ArrayList<Double>> calcKendalls(Input input, Instances trainset,
			Instances testset, ArrayList<ArrayList<Double>> cor) throws Exception {
		
		for (int i = 0; i < testset.numInstances(); i++) {
			double test[] = testset.get(i).toDoubleArray();
			for (int j = 0; j < trainset.numInstances(); j++) {
				double train[] = trainset.get(j).toDoubleArray();
				cor.get(i).set(j, new KendallsCorrelation().correlation(test, train));
			}
			System.out.println(i + "/" + testset.size());
		}
		return cor;
	}

	
	// calculates Kendalls AxB remove zero
	public static ArrayList<ArrayList<Double>> calcKendallsExclude0(Input input,
	ArrayList<double[]> train_arr, ArrayList<double[]> test_arr,
	ArrayList<ArrayList<Double>> cor) throws Exception {
		
		int attrNum = train_arr.get(0).length;
		
		for (int i = 0; i < test_arr.size(); i++) {
			for (int j = 0; j < train_arr.size(); j++) {
				
				ArrayList<Double> excludeCVtest = new ArrayList<Double>();
				ArrayList<Double> excludeCVtrain = new ArrayList<Double>();
				
				for (int k = 0; k < attrNum; k++) {
					// only compute for non_zero values
					if (test_arr.get(i)[k] != 0 || train_arr.get(j)[k] != 0) {
						excludeCVtrain.add(train_arr.get(j)[k]);
						excludeCVtest.add(test_arr.get(i)[k]);
					}
				}
				
				double[] train = new double[excludeCVtrain.size()];
				for (int k = 0; k < excludeCVtrain.size(); k++) {
					train[k] = excludeCVtrain.get(k);
					
				}
				
				double[] test = new double[excludeCVtest.size()];
				for (int k = 0; k < excludeCVtest.size(); k++) {
					test[k] = excludeCVtest.get(k);
				
				}
				
				if(excludeCVtrain.size() == 0 && excludeCVtest.size() == 0) {
					cor.get(i).set(j, 0.0);
					continue;
				}
				
				double kendalls = new KendallsCorrelation().correlation(test, train);
				if(kendalls < 0.0) {
					cor.get(i).set(j, -1.0);
				} else {
					cor.get(i).set(j, kendalls);
				}
				
			}
			System.out.println(i + "/" + test_arr.size());
		}
		return cor;
	}

	// calculates Euclidean Distance AxA
	public static ArrayList<ArrayList<Double>> calcEuclidean(Input input, Instances dataset,
			ArrayList<ArrayList<Double>> cor) throws Exception {

		for (int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			for (int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
				cor.get(i).set(j, new EuclideanDistance().compute(x, y));
			}
		}
		return cor;
	}

	
	// calculates Euclidean Distance AxB full size
	public static ArrayList<ArrayList<Double>> calcEuclidean(Input input, Instances trainset,
			Instances testset, ArrayList<ArrayList<Double>> cor) throws Exception {
		// Euclidean Distance for train and test full size
		for (int i = 0; i < testset.numInstances(); i++) {
			double test[] = testset.get(i).toDoubleArray();
			for (int j = 0; j < trainset.numInstances(); j++) {
				double train[] = trainset.get(j).toDoubleArray();
				cor.get(i).set(j, new EuclideanDistance().compute(test, train));
			}
			System.out.println(i + "/" + testset.size());
		}
		return cor;
	}
	
	
	// calculates Euclidean Distance AxB remove zeros
	public static ArrayList<ArrayList<Double>> calcEuclideanExclude0(Input input, ArrayList<double[]> train_arr,
			ArrayList<double[]> test_arr, ArrayList<ArrayList<Double>> cor) throws Exception {

		int attrNum = train_arr.get(0).length;
		for (int i = 0; i < test_arr.size(); i++) {
			for (int j = 0; j < train_arr.size(); j++) {
				ArrayList<Double> excludeCVtrain = new ArrayList<Double>();
				ArrayList<Double> excludeCVtest = new ArrayList<Double>();
				for (int k = 0; k < attrNum; k++) {
					// Make a new vector with values without zero for both train and test
					if (test_arr.get(i)[k] != 0.0 || train_arr.get(j)[k] != 0.0) {
						excludeCVtrain.add(train_arr.get(j)[k]);
						excludeCVtest.add(test_arr.get(i)[k]);
					}
				}
				double[] train = new double[excludeCVtrain.size()];
				for (int k = 0; k < excludeCVtrain.size(); k++) {
					train[k] = excludeCVtrain.get(k);
				}
				double[] test = new double[excludeCVtest.size()];
				for (int k = 0; k < excludeCVtest.size(); k++) {
					test[k] = excludeCVtest.get(k);
				}
				cor.get(i).set(j, new EuclideanDistance().compute(test, train));
			}
			System.out.println(i + "/" + test_arr.size());
		}
		return cor;
	}
	
	
	// calculates Spearmans AxA
	public static ArrayList<ArrayList<Double>> calcSpearmans(Input input, Instances dataset,
			ArrayList<ArrayList<Double>> cor) throws Exception {

		for (int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			for (int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
				cor.get(i).set(j, new SpearmansCorrelation().correlation(x, y));
			}
		}
		return cor;
	}

	
	// calculates Jaccard AxA
	public static ArrayList<ArrayList<Double>> calcJaccard(Input input, Instances dataset,
			ArrayList<ArrayList<Double>> cor) throws Exception {

		for (int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			CharSequence csx = Arrays.toString(x);
			for (int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
				CharSequence csy = Arrays.toString(y);
				cor.get(i).set(j, new JaccardSimilarity().apply(csx, csy));
			}
		}
		return cor;
	}

	
	// calculates Manhattans Distance AxA
	public static ArrayList<ArrayList<Double>> calcManhattan(Input input, Instances dataset,
			ArrayList<ArrayList<Double>> cor) throws Exception {
		// computing Manhattan Distance one by one
		for (int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			for (int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
				cor.get(i).set(j, new ManhattanDistance().compute(x, y));
			}
		}
		return cor;
	}

	
	// calculates Covariance matrix AxA
	public static ArrayList<ArrayList<Double>> calcCovariance(Input input, Instances dataset,
			ArrayList<ArrayList<Double>> cor) throws Exception {
		// computing Covariance one by one
		for (int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			for (int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
				cor.get(i).set(j, new Covariance().covariance(x, y));
			}
		}
		return cor;
	}

	
	// calculates cosine similarity AxA
	public static ArrayList<ArrayList<Double>> calcCosine(Input input, Instances dataset,
			ArrayList<ArrayList<Double>> cor) throws Exception {
		// computing Jaccard Similarity Coefficient one by one
		for (int i = 0; i < dataset.numInstances(); i++) {
			double[] x = dataset.get(i).toDoubleArray();
			for (int j = 0; j < dataset.numInstances(); j++) {
				double[] y = dataset.get(j).toDoubleArray();
				cor.get(i).set(j, cosineSimilarity(x, y));
			}
		}

		return cor;
	}

	
	// cosine similarity implementation
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