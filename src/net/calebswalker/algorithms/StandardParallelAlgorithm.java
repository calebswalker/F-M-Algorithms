package net.calebswalker.algorithms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.MatchingAlgorithm.Matching;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

public class StandardParallelAlgorithm extends TestCaseAlgorithm {

	private final int threadCount;
	
	public StandardParallelAlgorithm() {
		this(4);
	}
	
	public StandardParallelAlgorithm(int threadCount) {
		super("Standard Parallel Algorithm: " + threadCount + " Threads");
		if (threadCount <= 0) {
			throw new IllegalArgumentException("Must have positive number of threads!");
		}
		this.threadCount = threadCount;
	}
	
	public void run(int maxValue, TreeMap<Integer, TreeSet<Integer>> totalTrues, TreeMap<Integer, TreeSet<Integer>> totalFalses) throws IOException {
		int minSize = 4;
		final int maxSize = maxValue;
		
		final TreeSet<Integer> primes = new TreeSet<>();
		final List<DefaultUndirectedGraph<Integer, DefaultEdge>> threadGraphs = new ArrayList<>();
		
		for (int i = 0; i < threadCount; i++) {
			threadGraphs.add(new DefaultUndirectedGraph<>(DefaultEdge.class));
		}
		
		TreeSet<Integer> oldTrues = null;
		TreeSet<Integer> oldFalses = null;
		
		for (int size = 1; size <= maxSize; size++) { // Set size = 1 for full board else 2
			for (DefaultUndirectedGraph<Integer, DefaultEdge> graph : threadGraphs) {
				graph.addVertex(size);
				for (int j = 1; j*j <= size; j++) { // Set j = 1 for full board else 2
					if (size == j)
						continue;
					if (size % j == 0) {
						graph.addEdge(j, size);
						if (j != 1) {
							graph.addEdge(size / j, size);
						}
					}
				}
			}
			
			if (threadGraphs.get(0).degreeOf(size) == 1) { // Set == 1 if using full board else 0
				primes.add(size);
			}
			
			if (size < minSize)
				continue;
			
			if (oldTrues != null && size >= 12) {
				if (primes.contains(size)) { // Prime special case
					oldTrues.add(size);
					totalTrues.put(size, new TreeSet<>(oldTrues));
					totalFalses.put(size, new TreeSet<>(oldFalses));
					continue;
				}
				else if ((size & 2) == 0 && primes.contains(size / 2)) { // Twice a prime special case
					oldFalses.add(size);
					if (oldFalses.contains(2)) {
						oldFalses.add(size / 2);
						oldTrues.remove(size / 2);
					}
					else {
						oldTrues.add(size / 2);
						oldFalses.remove(size / 2);
					}
					
					totalTrues.put(size, new TreeSet<>(oldTrues));
					totalFalses.put(size, new TreeSet<>(oldFalses));
					continue;
				}
			}
			
			TreeSet<Integer> trues = new TreeSet<>();
			TreeSet<Integer> falses = new TreeSet<>();
			
			if (size > 1)
				falses.add(1);
			else
				trues.add(1);
			
			EdmondsMaximumCardinalityMatching<Integer, DefaultEdge> defaultMatching = new EdmondsMaximumCardinalityMatching<>(threadGraphs.get(0));
			final double oldWeight = defaultMatching.getMatching().getWeight();
			
			List<GeneratorThread> createdThreads = new ArrayList<>();
			AtomicInteger atomicInteger = new AtomicInteger(2);
			
			for (int i = 0; i < threadCount; i++) {
				GeneratorThread newThread = new GeneratorThread(threadGraphs.get(i), size, atomicInteger, oldWeight);
				createdThreads.add(newThread);
				newThread.start();
			}
			
			try {
				for (GeneratorThread generatorThread : createdThreads) {
					generatorThread.join();
					trues.addAll(generatorThread.trues);
					falses.addAll(generatorThread.falses);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			oldTrues = trues;
			oldFalses = falses;
			
			totalTrues.put(size, new TreeSet<>(trues));
			totalFalses.put(size, new TreeSet<>(falses));
		}
		
	}
		
	private final class GeneratorThread extends Thread implements Runnable {

		private final DefaultUndirectedGraph<Integer, DefaultEdge> graph;
		public final Set<Integer> trues = new TreeSet<>();
		public final Set<Integer> falses = new TreeSet<>();
		private final int size;
		private final AtomicInteger counter;
		private final double oldWeight;
		
		public GeneratorThread(DefaultUndirectedGraph<Integer, DefaultEdge> baseGraph, int size, AtomicInteger counter, double oldWeight) {
			super();
			this.size = size;
			this.counter = counter;
			this.graph = baseGraph;
			this.oldWeight = oldWeight;
		}
		
		@Override
		public void run() {
			int s;
			while((s = counter.getAndIncrement()) <= size) {	
				Set<Integer> detachedVertices = Graphs.neighborSetOf(graph, s);
				graph.removeVertex(s);
				
				EdmondsMaximumCardinalityMatching<Integer, DefaultEdge> matching = new EdmondsMaximumCardinalityMatching<>(graph);
				Matching<Integer, DefaultEdge> theMatching = matching.getMatching();
				double newWeight = theMatching.getWeight();
				
				if (newWeight < oldWeight) {
					falses.add(s);
				}
				else {
					trues.add(s);
				}
				
				graph.addVertex(s);
				for (int v : detachedVertices) {
					graph.addEdge(v, s);
				}
			}
		}
		
	}
}
