package net.calebswalker.algorithms;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;

import net.calebswalker.util.FileHelper;

public class AlgorithmTester {

	public static void main(String[] args) throws IOException {
		FileHelper.init();
		System.out.println("File Helper initialized.");
		int[] allTrials = {2, 10, 10, 10};
		int[] allMaxSizes = {50, 100, 250, 500};
		
		ArrayList<TestCaseAlgorithm> algorithmsToTest = new ArrayList<>();
		
		algorithmsToTest.add(new StandardAlgorithm());
		algorithmsToTest.add(new StandardConservedAlgorithm());
		algorithmsToTest.add(new Standard1RotationConservedAlgorithm());
		algorithmsToTest.add(new Standard2RotationConservedAlgorithm());
		algorithmsToTest.add(new StandardDiscoveryConservedAlgorithm());
		
		algorithmsToTest.add(new StandardParallelAlgorithm(2));
		algorithmsToTest.add(new StandardParallelAlgorithm(4));
		algorithmsToTest.add(new StandardParallelAlgorithm(6));
		algorithmsToTest.add(new StandardParallelAlgorithm(8));
		
		algorithmsToTest.add(new StandardConservedParallelAlgorithm(2));
		algorithmsToTest.add(new StandardConservedParallelAlgorithm(4));
		algorithmsToTest.add(new StandardConservedParallelAlgorithm(6));
		algorithmsToTest.add(new StandardConservedParallelAlgorithm(8));
		
		algorithmsToTest.add(new Standard1RotationConservedParallelAlgorithm(2));
		algorithmsToTest.add(new Standard1RotationConservedParallelAlgorithm(4));
		algorithmsToTest.add(new Standard1RotationConservedParallelAlgorithm(6));
		algorithmsToTest.add(new Standard1RotationConservedParallelAlgorithm(8));
		
		algorithmsToTest.add(new Standard2RotationConservedParallelAlgorithm(2));
		algorithmsToTest.add(new Standard2RotationConservedParallelAlgorithm(4));
		algorithmsToTest.add(new Standard2RotationConservedParallelAlgorithm(6));
		algorithmsToTest.add(new Standard2RotationConservedParallelAlgorithm(8));
		
		algorithmsToTest.add(new StandardDiscoveryConservedParallelAlgorithm(2));
		algorithmsToTest.add(new StandardDiscoveryConservedParallelAlgorithm(4));
		algorithmsToTest.add(new StandardDiscoveryConservedParallelAlgorithm(6));
		algorithmsToTest.add(new StandardDiscoveryConservedParallelAlgorithm(8));
		
		assert allTrials.length == allMaxSizes.length;
		
		for (int n = 0; n < allTrials.length; n++) {
			int trials = allTrials[n];
			int maxSize = allMaxSizes[n];
			
			System.out.println("Beginning " + trials + " trials of size " + maxSize + ".");
			
			for (TestCaseAlgorithm testAlgorithm : algorithmsToTest) {
				System.out.print("Testing " + testAlgorithm.getAlgorithmName() + "... ");
				ArrayList<Long> results = new ArrayList<>();
				
				for (int i = 0; i < trials; i++) {
					long amnt = evaluate(testAlgorithm, maxSize);
					if (amnt >= 0) {
						results.add(amnt);
					}
					else {
						break;
					}
				}
				appendToFile(testAlgorithm, maxSize, results);
				System.out.println("Complete.");
			}
			System.out.println("==================================");
		}
		System.out.println("All Trials Complete!");
	}
	
	private static long evaluate(TestCaseAlgorithm algorithm, int maxSize) throws IOException {
		Stopwatch stopwatch = Stopwatch.createUnstarted();
		TreeMap<Integer, TreeSet<Integer>> totalTrues = new TreeMap<>();
		TreeMap<Integer, TreeSet<Integer>> totalFalses = new TreeMap<>();
		
		stopwatch.start();
		algorithm.run(maxSize, totalTrues, totalFalses);
		stopwatch.stop();
		long ms = stopwatch.elapsed(TimeUnit.MILLISECONDS);
		if (vaildateData(totalTrues, totalFalses)) {
			return ms;
		}
		return -1;
	}
	
	private static void appendToFile(TestCaseAlgorithm algorithm, int maxSize, ArrayList<Long> times) throws IOException {
		
		FileWriter writer = new FileWriter("results.txt", true);
		PrintWriter printWriter = new PrintWriter(writer);
		printWriter.println(algorithm.getAlgorithmName());
		printWriter.println(maxSize + " boards");
		printWriter.println(times.toString());
		printWriter.println("=========================");
		printWriter.close();
	}
	
	private static boolean vaildateData(TreeMap<Integer, TreeSet<Integer>> truesToCheck, TreeMap<Integer, TreeSet<Integer>> falsesToCheck) {
		for (int size : truesToCheck.keySet()) {
			TreeSet<Integer> trues = truesToCheck.get(size);
			TreeSet<Integer> actualTrues = FileHelper.getTrues(size);
			if (!trues.equals(actualTrues)) {
				System.err.println("board size " + size + " trues are not correct!");
				System.err.println("Actual: " + actualTrues);
				System.err.println("Reported: " + trues);
				return false;
			}
		}
		
		for (int size : falsesToCheck.keySet()) {
			TreeSet<Integer> falses = falsesToCheck.get(size);
			TreeSet<Integer> actualFalses = FileHelper.getFalses(size);
			if (!falses.equals(actualFalses)) {
				System.err.println("board size " + size + " falses are not correct!");
				System.err.println("Actual: " + actualFalses);
				System.err.println("Reported: " + falses);
				return false;
			}
		}
		return true;
	}

}
