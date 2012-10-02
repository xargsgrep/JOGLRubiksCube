/*
 * Ahsan Rabbani <ahsan@xargsgrep.com>
 */

package com.xargsgrep.rubikscube;

import java.util.ArrayList;
import java.util.List;

import com.xargsgrep.rubikscube.Cubie.Color;
import com.xargsgrep.rubikscube.Rotation.Axis;
import com.xargsgrep.rubikscube.Rotation.Direction;

/* 
 * A rather poorly implemented solver for a 3x3x3 Rubik's Cube. This implementation is "dumb" in
 * the sense that it doesn't do any sort of optimizations to reduce the number of moves and doesn't
 * use any search algorithm (eg IDA*) that might normally be used in this context. It is simply
 * an implementation of the way I learned to solve a Rubik's Cube.
 */
public class RubiksCubeSolver {
	
//	private static final CubiePosition CENTER_FRONT  = new CubiePosition(1, 1, 0);
	private static final CubiePosition CENTER_REAR   = new CubiePosition(1, 1, 2);
	private static final CubiePosition CENTER_TOP    = new CubiePosition(1, 2, 1);
	private static final CubiePosition CENTER_BOTTOM = new CubiePosition(1, 0, 1);
	private static final CubiePosition CENTER_LEFT   = new CubiePosition(0, 1, 1);
	private static final CubiePosition CENTER_RIGHT  = new CubiePosition(2, 1, 1);
	
	private static final CubiePosition EDGE_FRONT_LEFT    = new CubiePosition(0, 1, 0);
	private static final CubiePosition EDGE_FRONT_RIGHT   = new CubiePosition(2, 1, 0);
	private static final CubiePosition EDGE_FRONT_TOP     = new CubiePosition(1, 2, 0);
	private static final CubiePosition EDGE_FRONT_BOTTOM  = new CubiePosition(1, 0, 0);
	private static final CubiePosition EDGE_MIDDLE_LEFT_TOP     = new CubiePosition(0, 2, 1);
	private static final CubiePosition EDGE_MIDDLE_LEFT_BOTTOM  = new CubiePosition(0, 0, 1);
	private static final CubiePosition EDGE_MIDDLE_RIGHT_TOP    = new CubiePosition(2, 2, 1);
	private static final CubiePosition EDGE_MIDDLE_RIGHT_BOTTOM = new CubiePosition(2, 0, 1);
	private static final CubiePosition EDGE_BACK_LEFT     = new CubiePosition(0, 1, 2);
	private static final CubiePosition EDGE_BACK_RIGHT    = new CubiePosition(2, 1, 2);
	private static final CubiePosition EDGE_BACK_TOP      = new CubiePosition(1, 2, 2);
	private static final CubiePosition EDGE_BACK_BOTTOM   = new CubiePosition(1, 0, 2);
	
	RubiksCube cube;
	List<Rotation> rotations;
	
	public RubiksCubeSolver(RubiksCube cube) {
		this.cube = cube;
		this.rotations = new ArrayList<Rotation>();
	}
	
	private Cubie getCubie(CubiePosition position) {
		return cube.getCubie(position.x, position.y, position.z);
	}
	
	public List<Rotation> getSolution() {
		solveStep1(); // front face cross
		solveStep2(); // front face corners
		solveStep3(); // middle face edges
		solveStep4(); // rear face cross
		solveStep5(); // rear face edges
		solveStep6(); // rear face corners' positions
		solveStep7(); // rear face corners' orientations
		return rotations;
	}
	
	private void addRotationAndApply(Rotation rotation) {
		rotations.add(rotation);
		cube.applyRotation(rotation);
	}
	
	private void solveStep1() {
		// move white center cubie to front face - this is just to get the cube into a known frame of reference
		if (getCubie(CENTER_REAR).rearColor == Color.WHITE) {
			addRotationAndApply(new Rotation(Axis.X, RubiksCube.COLUMN_MIDDLE, Direction.COUNTER_CLOCKWISE));
			addRotationAndApply(new Rotation(Axis.X, RubiksCube.COLUMN_MIDDLE, Direction.COUNTER_CLOCKWISE));
		}
		else if (getCubie(CENTER_TOP).topColor == Color.WHITE) {
			addRotationAndApply(new Rotation(Axis.X, RubiksCube.COLUMN_MIDDLE, Direction.COUNTER_CLOCKWISE));
		}
		else if (getCubie(CENTER_BOTTOM).bottomColor == Color.WHITE) {
			addRotationAndApply(new Rotation(Axis.X, RubiksCube.COLUMN_MIDDLE, Direction.CLOCKWISE));
		}
		else if (getCubie(CENTER_LEFT).leftColor == Color.WHITE) {
			addRotationAndApply(new Rotation(Axis.Y, RubiksCube.ROW_MIDDLE, Direction.COUNTER_CLOCKWISE));
		}
		else if (getCubie(CENTER_RIGHT).rightColor == Color.WHITE) {
			addRotationAndApply(new Rotation(Axis.Y, RubiksCube.ROW_MIDDLE, Direction.CLOCKWISE));
		}
		
		// move green center cubie to top face - this is just get the cube into a known frame of reference		if (getCubie(CENTER_BOTTOM).bottomColor == Color.GREEN) {
			addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_MIDDLE, Direction.COUNTER_CLOCKWISE));
			addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_MIDDLE, Direction.COUNTER_CLOCKWISE));
		}
		else if (getCubie(CENTER_LEFT).leftColor == Color.GREEN) {
			addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_MIDDLE, Direction.CLOCKWISE));
		}
		else if (getCubie(CENTER_RIGHT).rightColor == Color.GREEN) {
			addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_MIDDLE, Direction.COUNTER_CLOCKWISE));
		}
		
		// iterate through each front edge piece and solve it
		List<CubiePosition> frontEdges = getFrontEdges();
		for (CubiePosition edge : frontEdges) {
			Cubie cubie = getCubie(edge);
			
			if (edge.isInColumnLeft()) { // left edge
				// check if the edge piece is the correct position and orientation
				while (cubie.frontColor != Color.WHITE || cubie.leftColor != Color.RED) {
//					System.out.println("left edge is not solved");
					CubiePosition position = findEdgeWithColors(Color.WHITE, Color.RED);
					
					if (position.equals(edge)) {
						// edge is in correct position but wrong orientation
						addRotationAndApply(new Rotation(Axis.X, RubiksCube.COLUMN_LEFT, Direction.CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_FRONT, Direction.CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.Y, RubiksCube.ROW_TOP, Direction.COUNTER_CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_FRONT, Direction.COUNTER_CLOCKWISE));
					}
					else if (position.isInFaceFront()) {
						// edge is in the front face, get it into the rear face
						if (position.x == 0) {
							// do nothing
						}
						else if (position.x == 1) {
							addRotationAndApply(new Rotation(Axis.Y, position.y, Direction.CLOCKWISE));
							addRotationAndApply(new Rotation(Axis.Y, position.y, Direction.CLOCKWISE));
						}
						else if (position.x == 2) {
							addRotationAndApply(new Rotation(Axis.X, RubiksCube.COLUMN_RIGHT, Direction.CLOCKWISE));
							addRotationAndApply(new Rotation(Axis.X, RubiksCube.COLUMN_RIGHT, Direction.CLOCKWISE));
						}
					}
					else if (position.isInFaceMiddle()) {
						// edge is in the middle face, get it into the rear face
						addRotationAndApply(new Rotation(Axis.X, position.x, (position.y == 0) ? Direction.COUNTER_CLOCKWISE : Direction.CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.X, position.x, (position.y == 0) ? Direction.CLOCKWISE : Direction.COUNTER_CLOCKWISE));
					}
					else if (position.isInFaceRear()) {
						// edge is in the rear face, get it into the correct position in the front face
						if (position.x == 0) {
							// do nothing
						}
						else if (position.x == 1) {
							addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, (position.y == 0) ? Direction.CLOCKWISE : Direction.COUNTER_CLOCKWISE));
						}
						else if (position.x == 2) {
							addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
							addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
						}
						addRotationAndApply(new Rotation(Axis.X, RubiksCube.COLUMN_LEFT, Direction.CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.X, RubiksCube.COLUMN_LEFT, Direction.CLOCKWISE));
					}
					
					cubie = getCubie(edge);
				}
				System.out.println("left edge solved");
			}
			else if (edge.isInColumnMiddle() && edge.isInRowBottom()) { // bottom edge
				while (cubie.frontColor != Color.WHITE || cubie.bottomColor != Color.BLUE) {
//					System.out.println("bottom edge is not solved");
					CubiePosition position = findEdgeWithColors(Color.WHITE, Color.BLUE);
					
					if (position.equals(edge)) {
						// edge is in correct position but wrong orientation
						addRotationAndApply(new Rotation(Axis.Y, RubiksCube.ROW_BOTTOM, Direction.CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_FRONT, Direction.CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.X, RubiksCube.COLUMN_LEFT, Direction.CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_FRONT, Direction.COUNTER_CLOCKWISE));
					}
					else if (position.isInFaceFront()) {
						// edge is in the front face, get it into the rear face
						if (position.y == 0) {
							// do nothing
						}
						else if (position.y == 1) {
							addRotationAndApply(new Rotation(Axis.X, position.x, Direction.CLOCKWISE));
							addRotationAndApply(new Rotation(Axis.X, position.x, Direction.CLOCKWISE));
						}
						else if (position.y == 2) {
							addRotationAndApply(new Rotation(Axis.Y, RubiksCube.ROW_TOP, Direction.CLOCKWISE));
							addRotationAndApply(new Rotation(Axis.Y, RubiksCube.ROW_TOP, Direction.CLOCKWISE));
						}
					}
					else if (position.isInFaceMiddle()) {
						// edge is in the middle face, get it into the rear face
						addRotationAndApply(new Rotation(Axis.X, position.x, (position.y == 0) ? Direction.COUNTER_CLOCKWISE : Direction.CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.X, position.x, (position.y == 0) ? Direction.CLOCKWISE : Direction.COUNTER_CLOCKWISE));
					}
					else if (position.isInFaceRear()) {
						// edge is in the rear face, get it into the correct position in the front face
						if (position.y == 0) {
							// do nothing
						}
						else if (position.y == 1) {
							addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, (position.x == 0) ? Direction.COUNTER_CLOCKWISE : Direction.CLOCKWISE));
						}
						else if (position.y == 2) {
							addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
							addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
						}
						addRotationAndApply(new Rotation(Axis.Y, RubiksCube.ROW_BOTTOM, Direction.CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.Y, RubiksCube.ROW_BOTTOM, Direction.CLOCKWISE));
					}
					
					cubie = getCubie(edge);
				}
				
				System.out.println("bottom edge solved");
			}
			else if (edge.isInColumnMiddle() && edge.isInRowTop()) { // top edge
				while (cubie.frontColor != Color.WHITE || cubie.topColor != Color.GREEN) {
//					System.out.println("top edge is not solved");
					CubiePosition position = findEdgeWithColors(Color.WHITE, Color.GREEN);
					
					if (position.equals(edge)) {
						// edge is in correct position but wrong orientation
						addRotationAndApply(new Rotation(Axis.Y, RubiksCube.ROW_TOP, Direction.COUNTER_CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_FRONT, Direction.CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.X, RubiksCube.COLUMN_RIGHT, Direction.COUNTER_CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_FRONT, Direction.COUNTER_CLOCKWISE));
					}
					else if (position.isInFaceFront()) {
						// edge is in the front face, get it into the rear face
						if (position.y == 0) {
							addRotationAndApply(new Rotation(Axis.Y, RubiksCube.ROW_BOTTOM, Direction.CLOCKWISE));
							addRotationAndApply(new Rotation(Axis.Y, RubiksCube.ROW_BOTTOM, Direction.CLOCKWISE));
						}
						else if (position.y == 1) {
							addRotationAndApply(new Rotation(Axis.X, position.x, Direction.CLOCKWISE));
							addRotationAndApply(new Rotation(Axis.X, position.x, Direction.CLOCKWISE));
						}
						else if (position.y == 2) {
							// do nothing
						}
					}
					else if (position.isInFaceMiddle()) {
						// edge is in the middle face, get it into the rear face
						addRotationAndApply(new Rotation(Axis.X, position.x, (position.y == 0) ? Direction.COUNTER_CLOCKWISE : Direction.CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.X, position.x, (position.y == 0) ? Direction.CLOCKWISE : Direction.COUNTER_CLOCKWISE));
					}
					else if (position.isInFaceRear()) {
						// edge is in the rear face, get it into the correct position in the front face
						if (position.y == 0) {
							addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
							addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
						}
						else if (position.y == 1) {
							addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, (position.x == 0) ? Direction.CLOCKWISE : Direction.COUNTER_CLOCKWISE));
						}
						else if (position.y == 2) {
							// do nothing
						}
						addRotationAndApply(new Rotation(Axis.Y, RubiksCube.ROW_TOP, Direction.CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.Y, RubiksCube.ROW_TOP, Direction.CLOCKWISE));
					}
					
					cubie = getCubie(edge);
				}
				
				System.out.println("top edge solved");
			}
			else if (edge.isInColumnRight()) { // right edge
				while (cubie.frontColor != Color.WHITE || cubie.rightColor != Color.ORANGE) {
					CubiePosition position = findEdgeWithColors(Color.WHITE, Color.ORANGE);
					
					if (position.equals(edge)) {
						// edge is in correct position but wrong orientation
						addRotationAndApply(new Rotation(Axis.X, RubiksCube.COLUMN_RIGHT, Direction.COUNTER_CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_FRONT, Direction.CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.Y, RubiksCube.ROW_BOTTOM, Direction.CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_FRONT, Direction.COUNTER_CLOCKWISE));
					}
					else if (position.isInFaceFront()) {
						// edge is in the front face, get it into the rear face
						if (position.x == 0) {
							addRotationAndApply(new Rotation(Axis.X, RubiksCube.COLUMN_LEFT, Direction.CLOCKWISE));
							addRotationAndApply(new Rotation(Axis.X, RubiksCube.COLUMN_LEFT, Direction.CLOCKWISE));
						}
						else if (position.x == 1) {
							addRotationAndApply(new Rotation(Axis.Y, position.y, Direction.CLOCKWISE));
							addRotationAndApply(new Rotation(Axis.Y, position.y, Direction.CLOCKWISE));
						}
						else if (position.x == 2) {
							// do nothing
						}
					}
					else if (position.isInFaceMiddle()) {
						// edge is in the middle face, get it into the rear face
						addRotationAndApply(new Rotation(Axis.X, position.x, (position.y == 0) ? Direction.COUNTER_CLOCKWISE : Direction.CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.X, position.x, (position.y == 0) ? Direction.CLOCKWISE : Direction.COUNTER_CLOCKWISE));
					}
					else if (position.isInFaceRear()) {
						// edge is in the rear face, get it into the correct position in the front face
						if (position.x == 0) {
							addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
							addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
						}
						else if (position.x == 1) {
							addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, (position.y == 0) ? Direction.COUNTER_CLOCKWISE : Direction.CLOCKWISE));
						}
						else if (position.x == 2) {
							// do nothing
						}
						addRotationAndApply(new Rotation(Axis.X, RubiksCube.COLUMN_RIGHT, Direction.CLOCKWISE));
						addRotationAndApply(new Rotation(Axis.X, RubiksCube.COLUMN_RIGHT, Direction.CLOCKWISE));
					}
					
					cubie = getCubie(edge);
				}
				System.out.println("right edge solved");
			}
		}
	}
	
	private void solveStep2() {
		
	}

	private void solveStep3() {
		
	}
	
	private void solveStep4() {
		
	}
	
	private void solveStep5() {
		
	}
	
	private void solveStep6() {
		
	}
	
	private void solveStep7() {
		
	}
	
	private CubiePosition findEdgeWithColors(Color color1, Color color2) {
		List<CubiePosition> allEdges = getAllEdges();
		for (CubiePosition position : allEdges) {
			List<Color> colors = cube.getVisibleColors(position.x, position.y, position.z);
			if (colors.contains(color1) && colors.contains(color2)) return position;
		}
		return null;
	}
	
	private List<CubiePosition> getFrontEdges() {
		List<CubiePosition> edges = new ArrayList<CubiePosition>();
		edges.add(EDGE_FRONT_TOP);
		edges.add(EDGE_FRONT_BOTTOM);
		edges.add(EDGE_FRONT_LEFT);
		edges.add(EDGE_FRONT_RIGHT);
		return edges;
	}
	
	private List<CubiePosition> getMiddleEdges() {
		List<CubiePosition> edges = new ArrayList<CubiePosition>();
		edges.add(EDGE_MIDDLE_RIGHT_TOP);
		edges.add(EDGE_MIDDLE_RIGHT_BOTTOM);
		edges.add(EDGE_MIDDLE_LEFT_TOP);
		edges.add(EDGE_MIDDLE_LEFT_BOTTOM);
		return edges;
	}
	
	private List<CubiePosition> getBackEdges() {
		List<CubiePosition> edges = new ArrayList<CubiePosition>();
		edges.add(EDGE_BACK_TOP);
		edges.add(EDGE_BACK_BOTTOM);
		edges.add(EDGE_BACK_LEFT);
		edges.add(EDGE_BACK_RIGHT);
		return edges;
	}
	
	private List<CubiePosition> getAllEdges() {
		List<CubiePosition> edges = new ArrayList<CubiePosition>();
		edges.addAll(getFrontEdges());
		edges.addAll(getMiddleEdges());
		edges.addAll(getBackEdges());
		return edges;
	}
	
}