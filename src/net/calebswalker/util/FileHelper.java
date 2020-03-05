package net.calebswalker.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

public class FileHelper {
	
	private FileHelper() {}
	
	private static List<String> lines;
	
	private static final int MAX_SIZE = 1000;
	
	private static void loadFile() {
		if (lines != null)
			return;
		try {
			lines = Files.readAllLines(Paths.get("F&M Boards 4-" + MAX_SIZE + ".txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void init() {
		loadFile();
	}
	
	public static TreeSet<Integer> getTrues(int boardSize) {
		if (boardSize <= 0) {
			throw new IllegalArgumentException("boardSize must be a postive number!");
		}
		else if (boardSize > MAX_SIZE) {
			throw new IllegalArgumentException("boardSize cannot exceed " + MAX_SIZE + "!");
		}
		
		switch(boardSize) {
		case 1:
			return new TreeSet<>(Arrays.asList(1));
		case 2:
			return new TreeSet<>();
		case 3:
			return new TreeSet<>(Arrays.asList(2, 3));
		default:
			return getOtherTrues(boardSize);
		}
	}
	
	private static TreeSet<Integer> getOtherTrues(int boardSize) {
		loadFile();
		
		String trues = lines.get((boardSize - 4) * 4 + 1);
		String trueValues = trues.substring(trues.indexOf('[') + 1, trues.indexOf(']'));
		String[] values = trueValues.split(", ");
		
		TreeSet<Integer> set = new TreeSet<>();
		for (String string : values) {
			if (string.equals(""))
				continue;
			set.add(Integer.parseInt(string));
		}
		
		return set;
	}
	
	public static TreeSet<Integer> getFalses(int boardSize) {
		if (boardSize <= 0) {
			throw new IllegalArgumentException("boardSize must be a postive number!");
		}
		
		switch(boardSize) {
		case 1:
			return new TreeSet<>();
		case 2:
			return new TreeSet<>(Arrays.asList(1, 2));
		case 3:
			return new TreeSet<>(Arrays.asList(1));
		default:
			return getOtherFalses(boardSize);
		}
	}
	
	private static TreeSet<Integer> getOtherFalses(int boardSize) {
		loadFile();
		
		String falses = lines.get((boardSize - 4) * 4 + 2);
		String falseValues = falses.substring(falses.indexOf('[') + 1, falses.indexOf(']'));
		String[] values = falseValues.split(", ");
		
		TreeSet<Integer> set = new TreeSet<>();
		for (String string : values) {
			set.add(Integer.parseInt(string));
		}
		
		return set;
	}

}
