/*
 * Ahsan Rabbani <ahsan@xargsgrep.com>
 */

package com.xargsgrep.rubikscube;

import java.util.ArrayList;
import java.util.List;

/* 
 * TODO: Solver for a 3x3x3 Rubik's Cube.
 */
public class RubiksCubeSolver {
	
	RubiksCube cube;
	List<Rotation> rotations;
	
	public RubiksCubeSolver(RubiksCube cube) {
		this.cube = cube;
		this.rotations = new ArrayList<Rotation>();
	}
	
	public List<Rotation> getSolution() {
		return rotations;
	}
	
}
