/*
 * Ahsan Rabbani <ahsan@xargsgrep.com>
 */

package com.xargsgrep.rubikscube;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.xargsgrep.rubikscube.Rotation.Axis;
import com.xargsgrep.rubikscube.Rotation.Direction;

public class RubiksCubeSolverTester {
	
	public static void main(String[] args) {
		int minSolutionLength = -1;
		int maxSolutionLength = -1;
		int solutionLengthsSum = 0;
		
		int numCycles = 10000;
		int numScrambleMoves = 100;
		int invalidSolutions = 0;
		int progressStep = numCycles/10;
		
		long start = System.currentTimeMillis();
		
		for (int i=0; i<numCycles; i++) {
			RubiksCube cube = new RubiksCube(3);
			
			scrambleCube(cube, numScrambleMoves);
			
			RubiksCubeSolver solver = new RubiksCubeSolver(cube.getCopy());
			List<Rotation> solution = solver.getSolution();
			invalidSolutions += isValidSolution(cube, solution) ? 0 : 1;
			
			solutionLengthsSum += solution.size();
			if (solution.size() < minSolutionLength || minSolutionLength == -1) minSolutionLength = solution.size();
			if (solution.size() > maxSolutionLength || maxSolutionLength == -1) maxSolutionLength = solution.size();
			
			if ((i+1) % progressStep == 0) System.out.println("Progress: " + (i+1));
		}
		
		long end = System.currentTimeMillis();
		
		System.out.println();
		System.out.println("Num cycles: " + numCycles);
		System.out.println("Invalid solutions: " + invalidSolutions);
		
		System.out.println("Min solution length: " + minSolutionLength);
		System.out.println("Max solution length: " + maxSolutionLength);
		System.out.println("Avg solution length: " + (double) solutionLengthsSum/numCycles);
		
		System.out.println("total time (sec): " + (double) (end-start)/1000);
	}
	
	private static void scrambleCube(RubiksCube cube, int numMoves) {
		List<Rotation> rotations = new ArrayList<Rotation>();
		for (int i=0; i<numMoves; i++) {
			int section = new Random().nextInt(cube.getSize());
			Axis axis = Axis.values()[new Random().nextInt(Axis.values().length)];
			Direction direction = new Random().nextBoolean() ? Direction.CLOCKWISE : Direction.COUNTER_CLOCKWISE;
			rotations.add(new Rotation(axis, section, direction));
		}
		
		for (Rotation rotation : rotations) {
			cube.applyRotation(rotation);
		}
	}
	
	public static boolean isValidSolution(RubiksCube cube, List<Rotation> solution) {
		for (Rotation rotation : solution) {
			cube.applyRotation(rotation);
		}
		
		List<CubiePosition> positions = new ArrayList<CubiePosition>();
		positions.add(RubiksCubeSolver.CENTER_FRONT);
		positions.add(RubiksCubeSolver.CENTER_REAR);
		positions.add(RubiksCubeSolver.CENTER_TOP);
		positions.add(RubiksCubeSolver.CENTER_BOTTOM);
		positions.add(RubiksCubeSolver.CENTER_LEFT);
		positions.add(RubiksCubeSolver.CENTER_RIGHT);
		positions.add(RubiksCubeSolver.CORNER_FRONT_BOTTOM_LEFT);
		positions.add(RubiksCubeSolver.CORNER_FRONT_BOTTOM_RIGHT);
		positions.add(RubiksCubeSolver.CORNER_FRONT_TOP_LEFT);
		positions.add(RubiksCubeSolver.CORNER_FRONT_TOP_RIGHT);
		positions.add(RubiksCubeSolver.EDGE_FRONT_LEFT);
		positions.add(RubiksCubeSolver.EDGE_FRONT_RIGHT);
		positions.add(RubiksCubeSolver.EDGE_FRONT_BOTTOM);
		positions.add(RubiksCubeSolver.EDGE_FRONT_TOP);
		positions.add(RubiksCubeSolver.EDGE_MIDDLE_BOTTOM_LEFT);
		positions.add(RubiksCubeSolver.EDGE_MIDDLE_BOTTOM_RIGHT);
		positions.add(RubiksCubeSolver.EDGE_MIDDLE_TOP_LEFT);
		positions.add(RubiksCubeSolver.EDGE_MIDDLE_TOP_RIGHT);
		
		/*
		positions.add(RubiksCubeSolver.CORNER_REAR_BOTTOM_LEFT);
		positions.add(RubiksCubeSolver.CORNER_REAR_BOTTOM_RIGHT);
		positions.add(RubiksCubeSolver.CORNER_REAR_TOP_LEFT);
		positions.add(RubiksCubeSolver.CORNER_REAR_TOP_RIGHT);
		positions.add(RubiksCubeSolver.EDGE_REAR_LEFT);
		positions.add(RubiksCubeSolver.EDGE_REAR_RIGHT);
		positions.add(RubiksCubeSolver.EDGE_REAR_BOTTOM);
		positions.add(RubiksCubeSolver.EDGE_REAR_TOP);
		*/
		
		for (CubiePosition position : positions) {
			if (!cube.isPositionSolved(position)) return false;
		}
		
		return true;
	}

}
