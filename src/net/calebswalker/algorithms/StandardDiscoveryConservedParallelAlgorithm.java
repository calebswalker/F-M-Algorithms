package net.calebswalker.algorithms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.MatchingAlgorithm.Matching;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

public class StandardDiscoveryConservedParallelAlgorithm extends TestCaseAlgorithm {

	private final int threadCount;
	
	public StandardDiscoveryConservedParallelAlgorithm() {
		this(4);
	}
	
	public StandardDiscoveryConservedParallelAlgorithm(int threadCount) {
		super("Standard Parallel Algorithm by Discovery with Conservation: " + threadCount + " Threads");
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
		Matching<Integer, DefaultEdge> oldMatching = null;
		double oldWeight = 0;
		
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
			TreeSet<Integer> toCheck;
			
			DefaultUndirectedGraph<Integer, DefaultEdge> graph = threadGraphs.get(0);
			
			if (oldTrues != null) { // We got some stuff to work with
				Set<Integer> neighbors = Graphs.neighborSetOf(graph, size);
				if (oldFalses.containsAll(neighbors)) { // Size is true
					trues = oldTrues;
					trues.add(size);
					
					falses = new TreeSet<>();
					toCheck = new TreeSet<>(oldFalses);
					
					falses.add(1);
					toCheck.remove(1);
				}
				else { // Size is false
					falses = oldFalses;
					falses.add(size);
					
					trues = new TreeSet<>();
					toCheck = new TreeSet<>(oldTrues);
					
					EdmondsMaximumCardinalityMatching<Integer, DefaultEdge> defaultMatching = new EdmondsMaximumCardinalityMatching<>(graph);
					oldMatching = defaultMatching.getMatching();
					oldWeight = oldMatching.getWeight();
				}
			}
			else { // We don't have anything to start from
				trues = new TreeSet<>();
				falses = new TreeSet<>();
				toCheck = new TreeSet<>();
				for (int i = 2; i <= size; i++)
					toCheck.add(i);
				
				falses.add(1);
				toCheck.remove(1);
				
				EdmondsMaximumCardinalityMatching<Integer, DefaultEdge> defaultMatching = new EdmondsMaximumCardinalityMatching<>(graph);
				oldMatching = defaultMatching.getMatching();
				oldWeight = oldMatching.getWeight();
			}
			
			TreeMap<Integer, Integer> theOldMatching = new TreeMap<>();
			for (DefaultEdge e : oldMatching.getEdges()) {
				int source = graph.getEdgeSource(e);
				int target = graph.getEdgeTarget(e);
				
				theOldMatching.put(source, target);
				theOldMatching.put(target, source);
			}
			
			TreeSet<Integer> outerVertices = new TreeSet<>();
			for (int i = 1; i <= size; i++) {
				if (!theOldMatching.containsKey(i))
					outerVertices.add(i);
			}
			
			List<GeneratorThread> createdThreads = new ArrayList<>();
			
			for (int i = 0; i < threadCount; i++) {
				GeneratorThread newThread = new GeneratorThread(threadGraphs.get(i), toCheck, oldWeight, new TreeSet<>(outerVertices), new TreeMap<>(theOldMatching));
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
		private final TreeSet<Integer> toCheck, outerVertices;
		private final double oldWeight;
		private final TreeMap<Integer, Integer> theOldMatching;
		
		public GeneratorThread(DefaultUndirectedGraph<Integer, DefaultEdge> baseGraph, TreeSet<Integer> toCheck, double oldWeight, TreeSet<Integer> outerVertices, TreeMap<Integer, Integer> theOldMatching) {
			super();
			this.toCheck = toCheck;
			this.graph = baseGraph;
			this.oldWeight = oldWeight;
			this.outerVertices = outerVertices;
			this.theOldMatching = theOldMatching;
		}
		
		@Override
		public void run() {
			Set<Integer> toRemoveFromToCheck = new TreeSet<>();
			
			while(true) {
				int s;
				synchronized(toCheck) {
					toCheck.removeAll(toRemoveFromToCheck);
					if (toCheck.isEmpty())
						break;
					s = toCheck.pollFirst();
				}
				
				Set<Integer> detachedVertices = Graphs.neighborSetOf(graph, s);
				graph.removeVertex(s);
				
				EdmondsMaximumCardinalityMatching<Integer, DefaultEdge> matching = new EdmondsMaximumCardinalityMatching<>(graph);
				Matching<Integer, DefaultEdge> newMatching = matching.getMatching();
				double newWeight = newMatching.getWeight();
				
				if (newWeight < oldWeight) {
					falses.add(s);
				}
				else {
					trues.add(s);
					
					TreeMap<Integer, Integer> theNewMatching = new TreeMap<>();
					for (DefaultEdge e : newMatching.getEdges()) {
						int source = graph.getEdgeSource(e);
						int target = graph.getEdgeTarget(e);
						
						theNewMatching.put(source, target);
						theNewMatching.put(target, source);
					}
					
					toRemoveFromToCheck = new TreeSet<>();
					
					// Outer Vertex Transform Algorithm
					for (int u : outerVertices) {
						int x = u;
						while (theNewMatching.containsKey(x)) {
							int y = theNewMatching.get(x);
							int z = theOldMatching.get(y);
							
							trues.add(z);
							toRemoveFromToCheck.add(z);
							
							x = z;
						}
					}
				}
				
				graph.addVertex(s);
				for (int v : detachedVertices) {
					graph.addEdge(v, s);
				}
			}
		}
		
	}
}
