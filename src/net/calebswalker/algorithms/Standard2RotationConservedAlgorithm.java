package net.calebswalker.algorithms;

import java.io.IOException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.MatchingAlgorithm.Matching;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

public class Standard2RotationConservedAlgorithm extends TestCaseAlgorithm {

	public Standard2RotationConservedAlgorithm() {
		super("Standard Algorithm with Conservation and 2 Partial Rotation");
	}
	
	public void run(int maxValue, TreeMap<Integer, TreeSet<Integer>> totalTrues, TreeMap<Integer, TreeSet<Integer>> totalFalses) throws IOException {
		int minSize = 4;
		final int maxSize = Math.max(1, maxValue);
		
		final TreeSet<Integer> primes = new TreeSet<>();
		final DefaultUndirectedGraph<Integer, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
		
		TreeSet<Integer> oldTrues = null;
		TreeSet<Integer> oldFalses = null;
		Matching<Integer, DefaultEdge> oldMatching = null;
		
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
			
			TreeSet<Integer> trues;
			TreeSet<Integer> falses;
			TreeSet<Integer> toCheck;
			
			if (oldTrues != null) { // We got some stuff to work with
				Set<Integer> neighbors = Graphs.neighborSetOf(graph, size);
				if (oldFalses.containsAll(neighbors)) { // Size is true
					trues = oldTrues;
					trues.add(size);
					
					falses = new TreeSet<>();
					toCheck = new TreeSet<>(oldFalses);
					
					falses.add(1);
					toCheck.remove(1);
					
					// Partial rotate
					TreeMap<Integer, Integer> theMatching = new TreeMap<>();
					for (DefaultEdge e : oldMatching) {
						int target = graph.getEdgeTarget(e);
						int source = graph.getEdgeSource(e);
						
						theMatching.put(target, source);
						theMatching.put(source, target);
					}
					
					TreeSet<Integer> phase2Rotate = new TreeSet<>();
					for (int n : neighbors) {
						int pair = theMatching.get(n);
						
						trues.add(pair);
						toCheck.remove(pair);
						phase2Rotate.add(pair);
					}
					
					for (int n : phase2Rotate) {
						Set<Integer> locals = Graphs.neighborSetOf(graph, n);
						for (int l : locals) {
							if (!theMatching.containsKey(l))
								continue;
							int pair = theMatching.get(l);
							
							trues.add(pair);
							toCheck.remove(pair);
						}
					}
				}
				else { // Size is false
					falses = oldFalses;
					falses.add(size);
					
					trues = new TreeSet<>();
					toCheck = new TreeSet<>(oldTrues);
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
			}
			
			EdmondsMaximumCardinalityMatching<Integer, DefaultEdge> defaultMatching = new EdmondsMaximumCardinalityMatching<>(graph);
			oldMatching = defaultMatching.getMatching();
			final double oldWeight = oldMatching.getWeight();
			
			while (!toCheck.isEmpty()) {
				int s = toCheck.pollFirst();
				
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
					
					// Partial rotate
					TreeMap<Integer, Integer> unpackedMatching = new TreeMap<>();
					for (DefaultEdge e : theMatching) {
						int target = graph.getEdgeTarget(e);
						int source = graph.getEdgeSource(e);
						
						unpackedMatching.put(target, source);
						unpackedMatching.put(source, target);
					}
					
					TreeSet<Integer> phase2Rotate = new TreeSet<>();
					for (int n : detachedVertices) {
						int pair = unpackedMatching.get(n);
						
						trues.add(pair);
						toCheck.remove(pair);
						phase2Rotate.add(pair);
					}
					
					for (int n : phase2Rotate) {
						Set<Integer> locals = Graphs.neighborSetOf(graph, n);
						for (int l : locals) {
							int pair = unpackedMatching.get(l);
							
							trues.add(pair);
							toCheck.remove(pair);
						}
					}
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
