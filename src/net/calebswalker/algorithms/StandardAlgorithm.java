package net.calebswalker.algorithms;

import java.io.IOException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jgrapht.Graphs;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

public class StandardAlgorithm extends TestCaseAlgorithm {

	public StandardAlgorithm() {
		super("Standard Algorithm");
	}
	
	public void run(int maxValue, TreeMap<Integer, TreeSet<Integer>> totalTrues, TreeMap<Integer, TreeSet<Integer>> totalFalses) throws IOException {
		int minSize = 4;
		final int maxSize = Math.max(1, maxValue);
		
		final TreeSet<Integer> primes = new TreeSet<>();
		final DefaultUndirectedGraph<Integer, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
				
		TreeSet<Integer> oldTrues = null;
		TreeSet<Integer> oldFalses = null;
		
		for (int size = 1; size <= maxSize; size++) { // Set size = 1 for full board else 2
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
			
			if (graph.degreeOf(size) == 1) { // Set == 1 if using full board else 0
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
			
			EdmondsMaximumCardinalityMatching<Integer, DefaultEdge> defaultMatching = new EdmondsMaximumCardinalityMatching<>(graph);
			final double oldWeight = defaultMatching.getMatching().getWeight();
			
			for (int s = 2; s <= size; s++) {
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
			
			oldTrues = trues;
			oldFalses = falses;
			
			totalTrues.put(size, new TreeSet<>(trues));
			totalFalses.put(size, new TreeSet<>(falses));
		}
		
	}
}
