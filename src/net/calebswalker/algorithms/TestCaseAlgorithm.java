package net.calebswalker.algorithms;

import java.io.IOException;
import java.util.TreeMap;
import java.util.TreeSet;

public abstract class TestCaseAlgorithm {

	private String algorithmName;

	public TestCaseAlgorithm(String algorithmName) {
		this.algorithmName = algorithmName;
	}
	
	public final String getAlgorithmName() {
		return algorithmName;
	}
	
	public abstract void run(int maxValue, TreeMap<Integer, TreeSet<Integer>> totalTrues, TreeMap<Integer, TreeSet<Integer>> totalFalses) throws IOException;
	
}
