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
import org.apache.commons.math3.exception.MathIllegalArgumentException;
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
		if(input.inFile.contains("test")) {
			testCor(input, "Pearsons");
			testCor(input, "Kendalls");
			testCor(input, "EuclideanD");
			System.out.println("testing all correlations done!");
		} else {
			computeCor(input, "Pearsons");
			computeCor(input, "Kendalls");
			computeCor(input, "EuclideanD");
			//computeCor(input, "Spearmans");
			//computeCor(input, "Jaccard");
			//computeCor(input, "ManhattanD");
			//computeCor(input, "Covariance");
			//computeCor(input, "CosSim");
			System.out.println("writing all correlations done!");
		}
	}
	
	public static void testCor(Input input, String mode) throws Exception{
		String trainPath = "./assets/test/database.arff";
		String testPath = input.inFile;
		
		DataSource trainSource = new DataSource(trainPath);
		DataSource testSource = new DataSource(testPath);
		Instances trainset = trainSource.getDataSet();
		Instances testset = testSource.getDataSet();
		
		if(trainset.classIndex() == -1)
			trainset.setClassIndex(trainset.numAttributes() - 1);
		if(testset.classIndex() == -1)
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
		for(int i = 0; i < testSize; i++) {
			cor.add(new ArrayList<Double>());
			for(int j = 0; j < trainSize; j++) {
				cor.get(i).add(0.0);
			}
		}
		
		// init train_arr
		ArrayList<double[]> train_arr = new ArrayList<double[]>();
		for(int i = 0; i < trainSize; i++) {
			train_arr.add(new double[attrNum]);
		}
		for(int i = 0; i < trainSize; i++) {
			train_arr.set(i, trainset.get(i).toDoubleArray());
		}
		
		// removing changeVector info before computing correlation coefficients
		ArrayList<double[]> train_rm = new ArrayList<double[]>();
		for(int i = 0; i < trainSize; i++) {
			train_rm.add(new double[attrNum-4]);
			
		}
		for(int i = 0; i < trainSize; i++) {
			for(int j = 4; j < attrNum-4; j++) {
				train_rm.get(i)[j] = train_arr.get(i)[j];
			}
		}

		// init test_arr
		ArrayList<double[]> test_arr = new ArrayList<double[]>();
		for(int i = 0; i < testset.numInstances(); i++) {
			test_arr.add(new double[testset.numAttributes()]);
		}
		for(int i = 0; i < testset.numInstances(); i++) {
			test_arr.set(i, testset.get(i).toDoubleArray());
		}
		// removing changeVector info before computing correlation coefficients
		ArrayList<double[]> test_rm = new ArrayList<double[]>();
		for(int i = 0; i < testSize; i++) {
			test_rm.add(new double[attrNum-4]);
			
		}
		for(int i = 0; i < testSize; i++) {
			for(int j = 4; j < attrNum-4; j++) {
				test_rm.get(i)[j] = test_arr.get(i)[j];
			}
		}
			

		if(mode.equals("Pearsons")) {
			cor = computePCC(input, train_rm, test_rm, cor);
		} else if(mode.equals("Kendalls")) {
			cor = computeKCC(input, train_rm, test_rm, cor);
		} else if(mode.equals("EuclideanD")) {
			cor = computeEucD(input, train_rm, test_rm, cor);
		} 
		
		writeCombined(input, mode, trainSize, testSize, cor);
		System.out.println("\nWriting " + mode + " done!\n");
	}
	
	public static void computeCor(Input input, String mode) throws Exception{	

		String filePath = input.inFile;
		DataSource source = new DataSource(filePath);
		Instances dataset = source.getDataSet();
		
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
			cor = computeCos(input, dataset, cor);
		} 
			
		// writing files
		if(input.inFile.contains("combined")) {
			writeCombined(input, mode, dataset, cor);
		} else {
			writeOnebyOne(input, mode, dataset, cor);
		}
		
		System.out.println("\nWriting " + mode + " done!\n");
	}

	public static void writeOnebyOne(Input input, String mode, Instances dataset, ArrayList<ArrayList<Double>> cor) throws IOException {
		File outFile = new File(input.outFile + mode + "_"+ input.projectName + ".csv");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
		
		// index of x-axis
		csvprinter.print(mode);
		for (int i = 0; i < dataset.numInstances(); i++) {
			csvprinter.print(i);
		}
		csvprinter.println();
		
		for(int i = 0; i < dataset.numInstances(); i++) {
			csvprinter.print(i);
			for(int j = 0; j < dataset.numInstances(); j++) {
				csvprinter.print(cor.get(i).get(j));
			}
			csvprinter.println();
		}
		csvprinter.close();
	}
	
	// writing file for test
	public static void writeCombined(Input input, String mode, int trainSize, int testSize, ArrayList<ArrayList<Double>> cor) throws IOException {
		File outFile = new File(input.outFile + mode + "_test_commons" + ".csv");
		
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile.getAbsolutePath()));
		CSVPrinter csvprinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
		
		// combined part
		int trainIgnite = 150;
		int trainLucene = 236;
		int trainZookeeper = 140;

		int testIO = 2865;
		int testLang = 6306;
		//int testMath = 19383;
		String test1 = "commons-io";
		String test2 = "commons-lang";
		String test3 = "commons-math";
		
		// writing index of trainset
		csvprinter.print(mode);
		for (int i = 0; i < trainIgnite; i++) {
			csvprinter.print("ignite"+i);
		}
		for (int i = 0; i < trainLucene; i++) {
			csvprinter.print("lucene-solr"+i);
		}
		for (int i = 0; i < trainZookeeper; i++) {
			csvprinter.print("zookeeper"+i);
		}
		csvprinter.println();
		
		// writing correlation
		for(int i = 0, lang = 0, math = 0; i < testSize; i++) {
			// writing index of testset
			if(i < testIO) {
				csvprinter.print(test1+i);
			} else if(i < testIO + testLang) {
				csvprinter.print(test2+(lang++));
			} else {
				csvprinter.print(test3+(math++));
			}
			// writing the computed correlation
			for(int j = 0; j < trainSize; j++) {
				csvprinter.print(cor.get(i).get(j));
			}
			csvprinter.println();
		}
		csvprinter.close();
	}
	
	public static void writeCombined(Input input, String mode, Instances dataset, ArrayList<ArrayList<Double>> cor) throws IOException {
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
			csvprinter.print("ignite"+i);
		}
		for (int i = 0; i < numOfLucene; i++) {
			csvprinter.print("lucene-solr"+i);
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
				csvprinter.print("lucene-solr"+(lucene++));
			} else {
				csvprinter.print("zookeeper"+(zookeeper++));
			}
			for(int j = 0; j < dataset.numInstances(); j++) {
				csvprinter.print(cor.get(i).get(j));
			}
			csvprinter.println();
		}
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
	public static ArrayList<ArrayList<Double>> computePCC(Input input, ArrayList<double[]> train_arr, ArrayList<double[]> test_arr, ArrayList<ArrayList<Double>> cor) throws Exception {
		// computing Pearsons Correlation Coefficient one by one
		int attrNum = train_arr.get(0).length;
		for(int i = 0; i < test_arr.size(); i++) {
			for(int j = 0; j < train_arr.size(); j++) {
				
				// make new changeVector 
				ArrayList<Double> excludeCVtrain = new ArrayList<Double>();
				ArrayList<Double> excludeCVtest = new ArrayList<Double>();
				// traverse attributes
				for(int k = 0; k < attrNum; k++) {
					// if either test and train has non_zero value, add to the new CV
					if(test_arr.get(i)[k] !=0 || train_arr.get(j)[k] != 0) {
						excludeCVtrain.add(train_arr.get(j)[k]);
						excludeCVtest.add(test_arr.get(i)[k]);
					}
				}
				// changing the type to double[]
				double[] train = new double[excludeCVtrain.size()];
				for(int k = 0; k < excludeCVtrain.size(); k++) {
					train[k] = excludeCVtrain.get(k);
				}
				double[] test = new double[excludeCVtest.size()];
				for(int k = 0; k < excludeCVtest.size(); k++) {
					test[k] = excludeCVtest.get(k);
				}
				double pearson;
				try {
					pearson = new PearsonsCorrelation().correlation(test, train);
				} catch(MathIllegalArgumentException miae) {
					System.out.println("exception: "+miae);
					pearson = 0.0;
				}
				
			    cor.get(i).set(j, pearson);
			    System.out.println("cor: " + pearson);
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
	public static ArrayList<ArrayList<Double>> computeKCC(Input input, ArrayList<double[]> train_arr, ArrayList<double[]> test_arr, ArrayList<ArrayList<Double>> cor) throws Exception {
		// computing Kendalls Correlation Coefficient one by one
		int attrNum = train_arr.get(0).length;
		for(int i = 0; i < test_arr.size(); i++) {
			for(int j = 0; j < train_arr.size(); j++) {
				ArrayList<Double> excludeCVtrain = new ArrayList<Double>();
				ArrayList<Double> excludeCVtest = new ArrayList<Double>();
				for(int k = 0; k < attrNum; k++) {
					// only compute for non_zero values
					if(test_arr.get(i)[k] != 0 || train_arr.get(j)[k] != 0) {
						excludeCVtrain.add(train_arr.get(j)[k]);
						excludeCVtest.add(test_arr.get(i)[k]);
					}
				}
				double[] train = new double[excludeCVtrain.size()];
				for(int k = 0; k < excludeCVtrain.size(); k++) {
					train[k] = excludeCVtrain.get(k);
				}
				double[] test = new double[excludeCVtest.size()];
				for(int k = 0; k < excludeCVtest.size(); k++) {
					test[k] = excludeCVtest.get(k);
				}
				double kendalls;
				try {
					kendalls = new KendallsCorrelation().correlation(test, train);
				} catch(ArrayIndexOutOfBoundsException aioobe) {
					System.out.println("exception: "+aioobe);
					kendalls = 0.0;
				}
				
			    cor.get(i).set(j, kendalls);
			    System.out.println("cor: " + kendalls);
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
	public static ArrayList<ArrayList<Double>> computeEucD(Input input, ArrayList<double[]> train_arr, ArrayList<double[]> test_arr, ArrayList<ArrayList<Double>> cor) throws Exception {
		// computing Euclidean Distance one by one
		int attrNum = train_arr.get(0).length;
		for(int i = 0; i < test_arr.size(); i++) {
			for(int j = 0; j < train_arr.size(); j++) {
				ArrayList<Double> excludeCVtrain = new ArrayList<Double>();
				ArrayList<Double> excludeCVtest = new ArrayList<Double>();
				for(int k = 0; k < attrNum; k++) {
					// only compute for non_zero values
					if(test_arr.get(i)[k] != 0 || train_arr.get(j)[k] != 0) {
						excludeCVtrain.add(train_arr.get(j)[k]);
						excludeCVtest.add(test_arr.get(i)[k]);
					}
				}
				double[] train = new double[excludeCVtrain.size()];
				for(int k = 0; k < excludeCVtrain.size(); k++) {
					train[k] = excludeCVtrain.get(k);
				}
				double[] test = new double[excludeCVtest.size()];
				for(int k = 0; k < excludeCVtest.size(); k++) {
					test[k] = excludeCVtest.get(k);
				}
			    cor.get(i).set(j, new EuclideanDistance().compute(test, train));
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
	public static ArrayList<ArrayList<Double>> computeCos(Input input, Instances dataset, ArrayList<ArrayList<Double>> cor) throws Exception {
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
