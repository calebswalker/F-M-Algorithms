package net.calebswalker.algorithms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.jgrapht.Graphs;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

public class StandardConservedParallelAlgorithm extends TestCaseAlgorithm {

	private final int threadCount;
	
	public StandardConservedParallelAlgorithm() {
		this(4);
	}
	
	public StandardConservedParallelAlgorithm(int threadCount) {
		super("Standard Parallel Algorithm with Conservation: " + threadCount + " Threads");
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
			
			TreeSet<Integer> trues;
			TreeSet<Integer> falses;
			List<Integer> toCheck;
			
			if (oldTrues != null) { // We got some stuff to work with
				Set<Integer> neighbors = Graphs.neighborSetOf(threadGraphs.get(0), size);
				if (oldFalses.containsAll(neighbors)) { // Size is true
					trues = oldTrues;
					trues.add(size);
					
					falses = new TreeSet<>();
					toCheck = new ArrayList<>(oldFalses);
					
					falses.add(1);
					toCheck.remove((Integer) 1);
				}
				else { // Size is false
					falses = oldFalses;
					falses.add(size);
					
					trues = new TreeSet<>();
					toCheck = new ArrayList<>(oldTrues);
				}
			}
			else { // We don't have anything to start from
				trues = new TreeSet<>();
				falses = new TreeSet<>();
				toCheck = new ArrayList<>();
				for (int i = 2; i <= size; i++)
					toCheck.add(i);
				
				falses.add(1);
				toCheck.remove((Integer) 1);
			}
			
			EdmondsMaximumCardinalityMatching<Integer, DefaultEdge> defaultMatching = new EdmondsMaximumCardinalityMatching<>(threadGraphs.get(0));
			final double oldWeight = defaultMatching.getMatching().getWeight();
			
			List<GeneratorThread> createdThreads = new ArrayList<>();
			AtomicInteger atomicInteger = new AtomicInteger(0);
			
			for (int i = 0; i < threadCount; i++) {
				GeneratorThread newThread = new GeneratorThread(threadGraphs.get(i), toCheck, atomicInteger, oldWeight);
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
		private final List<Integer> toCheck;
		private final AtomicInteger counter;
		private final double oldWeight;
		
		public GeneratorThread(DefaultUndirectedGraph<Integer, DefaultEdge> baseGraph, List<Integer> toCheck, AtomicInteger counter, double oldWeight) {
			super();
			this.toCheck = toCheck;
			this.counter = counter;
			this.graph = baseGraph;
			this.oldWeight = oldWeight;
		}
		
		@Override
		public void run() {
			int index;
			int size = toCheck.size();
			while((index = counter.getAndIncrement()) < size) {
				int s = toCheck.get(index);
				
				Set<Integer> detachedVertices = Graphs.neighborSetOf(graph, s);
				graph.removeVertex(s);
				
				EdmondsMaximumCardinalityMatching<Integer, DefaultEdge> matching = new EdmondsMaximumCardinalityMatching<>(graph);
				double newWeight = matching.getMatching().getWeight();
				
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
