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
 * an implementation of the way I learned to solve a Rubik's Cube (by layers).
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
	private static final CubiePosition EDGE_MIDDLE_TOP_LEFT     = new CubiePosition(0, 2, 1);
	private static final CubiePosition EDGE_MIDDLE_BOTTOM_LEFT  = new CubiePosition(0, 0, 1);
	private static final CubiePosition EDGE_MIDDLE_TOP_RIGHT    = new CubiePosition(2, 2, 1);
	private static final CubiePosition EDGE_MIDDLE_BOTTOM_RIGHT = new CubiePosition(2, 0, 1);
	private static final CubiePosition EDGE_REAR_LEFT     = new CubiePosition(0, 1, 2);
	private static final CubiePosition EDGE_REAR_RIGHT    = new CubiePosition(2, 1, 2);
	private static final CubiePosition EDGE_REAR_TOP      = new CubiePosition(1, 2, 2);
	private static final CubiePosition EDGE_REAR_BOTTOM   = new CubiePosition(1, 0, 2);
	
	private static final CubiePosition CORNER_FRONT_TOP_LEFT     = new CubiePosition(0, 2, 0);
	private static final CubiePosition CORNER_FRONT_BOTTOM_LEFT  = new CubiePosition(0, 0, 0);
	private static final CubiePosition CORNER_FRONT_TOP_RIGHT    = new CubiePosition(2, 2, 0);
	private static final CubiePosition CORNER_FRONT_BOTTOM_RIGHT = new CubiePosition(2, 0, 0);
	private static final CubiePosition CORNER_REAR_TOP_LEFT      = new CubiePosition(0, 2, 2);
	private static final CubiePosition CORNER_REAR_BOTTOM_LEFT   = new CubiePosition(0, 0, 2);
	private static final CubiePosition CORNER_REAR_TOP_RIGHT     = new CubiePosition(2, 2, 2);
	private static final CubiePosition CORNER_REAR_BOTTOM_RIGHT  = new CubiePosition(2, 0, 2);
	
	RubiksCube cube;
	List<Rotation> rotations;
	
	public RubiksCubeSolver(RubiksCube cube) {
		this.cube = cube;
		this.rotations = new ArrayList<Rotation>();
	}
	
	public List<Rotation> getSolution() {
		positionCenters(); // this is just to get the cube into a known orientation (white on the front and green on the top)
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
	
	private void positionCenters() {
		// move white center cubie to front face
		if (cube.getCubie(CENTER_REAR).rearColor == Color.WHITE) {
			addRotationAndApply(new Rotation(Axis.X, RubiksCube.COLUMN_MIDDLE, Direction.COUNTER_CLOCKWISE));
			addRotationAndApply(new Rotation(Axis.X, RubiksCube.COLUMN_MIDDLE, Direction.COUNTER_CLOCKWISE));
		}
		else if (cube.getCubie(CENTER_TOP).topColor == Color.WHITE) {
			addRotationAndApply(new Rotation(Axis.X, RubiksCube.COLUMN_MIDDLE, Direction.COUNTER_CLOCKWISE));
		}
		else if (cube.getCubie(CENTER_BOTTOM).bottomColor == Color.WHITE) {
			addRotationAndApply(new Rotation(Axis.X, RubiksCube.COLUMN_MIDDLE, Direction.CLOCKWISE));
		}
		else if (cube.getCubie(CENTER_LEFT).leftColor == Color.WHITE) {
			addRotationAndApply(new Rotation(Axis.Y, RubiksCube.ROW_MIDDLE, Direction.COUNTER_CLOCKWISE));
		}
		else if (cube.getCubie(CENTER_RIGHT).rightColor == Color.WHITE) {
			addRotationAndApply(new Rotation(Axis.Y, RubiksCube.ROW_MIDDLE, Direction.CLOCKWISE));
		}
		
		// move green center cubie to top face		if (cube.getCubie(CENTER_BOTTOM).bottomColor == Color.GREEN) {
			addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_MIDDLE, Direction.COUNTER_CLOCKWISE));
			addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_MIDDLE, Direction.COUNTER_CLOCKWISE));
		}
		else if (cube.getCubie(CENTER_LEFT).leftColor == Color.GREEN) {
			addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_MIDDLE, Direction.CLOCKWISE));
		}
		else if (cube.getCubie(CENTER_RIGHT).rightColor == Color.GREEN) {
			addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_MIDDLE, Direction.COUNTER_CLOCKWISE));
		}
	}
	
	/*********************************************************************************************************************************************************/
	
	private void solveStep1() {
		// iterate through each front edge and solve it
		List<CubiePosition> positions = getFrontEdgePositions();
		for (CubiePosition position : positions) {
			step1SolveEdge(position);
		}
	}
	
	private void step1SolveEdge(CubiePosition edgePosition) {
		Cubie edgeCubie = cube.getCubie(edgePosition);
		List<Color> colors = cube.getVisibleColorsInSolvedState(edgePosition);
		
		while (!cube.isCubieSolved(edgeCubie, edgePosition)) {
			CubiePosition source = findCubiePositionWithColors(colors.toArray(new Color[0]));
		
			if (source.equals(edgePosition)) {
				// edge is in correct position but wrong orientation, solve it
				Axis axis1 = source.isInRowMiddle() ? Axis.X : Axis.Y;
				int section1 = source.isInRowMiddle() ? source.x : source.y;
				Direction direction1 = (source.isInColumnLeft() || source.isInRowBottom()) ? Direction.CLOCKWISE : Direction.COUNTER_CLOCKWISE;
				
				Axis axis2 = source.isInRowMiddle() ? Axis.Y : Axis.X;
				int section2 = 0;
				if (source.isInColumnLeft())       section2 = RubiksCube.ROW_TOP;
				else if (source.isInColumnRight()) section2 = RubiksCube.ROW_BOTTOM;
				else if (source.isInRowBottom())   section2 = RubiksCube.COLUMN_LEFT;
				else if (source.isInRowTop())      section2 = RubiksCube.COLUMN_RIGHT;
				Direction direction2 = (source.isInColumnLeft() || source.isInRowTop()) ? Direction.COUNTER_CLOCKWISE : Direction.CLOCKWISE;
				
				addRotationAndApply(new Rotation(axis1, section1, direction1));
				addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_FRONT, Direction.CLOCKWISE));
				addRotationAndApply(new Rotation(axis2, section2, direction2));
				addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_FRONT, Direction.COUNTER_CLOCKWISE));
			}
			else if (source.isInFaceFront()) {
				// edge is in the front face, get it into the rear face
				if (source.isInColumnMiddle()) {
					addRotationAndApply(new Rotation(Axis.Y, source.y, Direction.CLOCKWISE));
					addRotationAndApply(new Rotation(Axis.Y, source.y, Direction.CLOCKWISE));
				}
				else if (source.isInRowMiddle()) {
					addRotationAndApply(new Rotation(Axis.X, source.x, Direction.CLOCKWISE));
					addRotationAndApply(new Rotation(Axis.X, source.x, Direction.CLOCKWISE));
				}
			}
			else if (source.isInFaceMiddle()) {
				// edge is in the middle face, get it into the rear face
				addRotationAndApply(new Rotation(Axis.X, source.x, (source.y == 0) ? Direction.COUNTER_CLOCKWISE : Direction.CLOCKWISE));
				addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
				addRotationAndApply(new Rotation(Axis.X, source.x, (source.y == 0) ? Direction.CLOCKWISE : Direction.COUNTER_CLOCKWISE));
			}
			else if (source.isInFaceRear()) {
				// edge is in the rear face, get it into the correct position in the front face
				if (source.x != edgePosition.x && source.y != edgePosition.y) {
					Direction direction = ((edgePosition.isInColumnLeft() && source.isInRowTop()) || (edgePosition.isInColumnRight() && source.isInRowBottom()))
							? Direction.COUNTER_CLOCKWISE
							: Direction.CLOCKWISE;
					
					addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, direction));
				}
				else if ((source.x == edgePosition.x && source.y != edgePosition.y) || (source.x != edgePosition.x && source.y == edgePosition.y)) {
					addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
					addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
				}
				
				Axis axis = edgePosition.isInRowMiddle() ? Axis.X : Axis.Y;
				int section = edgePosition.isInRowMiddle() ? edgePosition.x : edgePosition.y;
				addRotationAndApply(new Rotation(axis, section, Direction.CLOCKWISE));
				addRotationAndApply(new Rotation(axis, section, Direction.CLOCKWISE));
			}
			
			edgeCubie = cube.getCubie(edgePosition);
		}
	}
	
	/*********************************************************************************************************************************************************/
	
	private void solveStep2() {
		// iterate through each front corner and solve it
		List<CubiePosition> positions = getFrontCornerPositions();
		for (CubiePosition position : positions) {
			step2SolveCorner(position);
		}
	}
	
	private void step2SolveCorner(CubiePosition cornerPosition) {
		Cubie cornerCubie = cube.getCubie(cornerPosition);
		List<Color> colors = cube.getVisibleColorsInSolvedState(cornerPosition);
		
		while (!cube.isCubieSolved(cornerCubie, cornerPosition)) {
			CubiePosition source = findCubiePositionWithColors(colors.toArray(new Color[0]));
			
			if (source.isInFaceFront()) {
				// corner is in the front face, get it into the rear face
				addRotationAndApply(new Rotation(Axis.X, source.x, source.isInRowTop() ? Direction.CLOCKWISE : Direction.COUNTER_CLOCKWISE));
				addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
				addRotationAndApply(new Rotation(Axis.X, source.x, source.isInRowTop() ? Direction.COUNTER_CLOCKWISE : Direction.CLOCKWISE));
			}
			else if (source.isInFaceRear() && (source.x == cornerPosition.x && source.y == cornerPosition.y)) {
				// corner is in the rear face and is aligned with the appropriate front corner, solve it
				Cubie sourceCubie = cube.getCubie(source);
				if ((source.x == 0 && source.y == 0 && sourceCubie.bottomColor == Color.WHITE)
						|| (source.x == 2 && source.y == 0 && sourceCubie.rightColor == Color.WHITE)
						|| (source.x == 2 && source.y == 2 && sourceCubie.topColor == Color.WHITE)
						|| (source.x == 0 && source.y == 2 && sourceCubie.leftColor == Color.WHITE))
				{
					Axis axis = (source.x == source.y) ? Axis.X : Axis.Y;
					int section = (source.x == source.y) ? source.x : source.y;
					Direction direction1 = (source.isInRowTop()) ? Direction.CLOCKWISE : Direction.COUNTER_CLOCKWISE;
					Direction direction2 = (source.isInRowTop()) ? Direction.COUNTER_CLOCKWISE : Direction.CLOCKWISE;
					
					addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.COUNTER_CLOCKWISE));
					addRotationAndApply(new Rotation(axis, section, direction1));
					addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
					addRotationAndApply(new Rotation(axis, section, direction2));
				}
				else if ((source.x == 0 && source.y == 0 && sourceCubie.leftColor == Color.WHITE)
						|| (source.x == 2 && source.y == 0 && sourceCubie.bottomColor == Color.WHITE)
						|| (source.x == 2 && source.y == 2 && sourceCubie.rightColor == Color.WHITE)
						|| (source.x == 0 && source.y == 2 && sourceCubie.topColor == Color.WHITE))
				{
					Axis axis = (source.x == source.y) ? Axis.Y : Axis.X;
					int section = (source.x == source.y) ? source.y : source.x;
					Direction direction1 = (source.isInColumnLeft()) ? Direction.CLOCKWISE : Direction.COUNTER_CLOCKWISE;
					Direction direction2 = (source.isInColumnLeft()) ? Direction.COUNTER_CLOCKWISE : Direction.CLOCKWISE;
					
					addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
					addRotationAndApply(new Rotation(axis, section, direction1));
					addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.COUNTER_CLOCKWISE));
					addRotationAndApply(new Rotation(axis, section, direction2));
				}
				else if (sourceCubie.rearColor == Color.WHITE) {
					Axis axis = (source.x == source.y) ? Axis.X : Axis.Y;
					int section = (source.x == source.y) ? source.x : source.y;
					Direction direction1 = (source.isInRowBottom()) ? Direction.CLOCKWISE : Direction.COUNTER_CLOCKWISE;
					Direction direction2 = (source.isInRowBottom()) ? Direction.COUNTER_CLOCKWISE: Direction.CLOCKWISE;
					
					addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
					addRotationAndApply(new Rotation(axis, section, direction1));
					addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.COUNTER_CLOCKWISE));
					addRotationAndApply(new Rotation(axis, section, direction2));
					addRotationAndApply(new Rotation(axis, section, direction2));
					addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
					addRotationAndApply(new Rotation(axis, section, direction1));
					addRotationAndApply(new Rotation(axis, section, direction1));
					addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
					addRotationAndApply(new Rotation(axis, section, direction2));
				}
			}
			else if (source.isInFaceRear() && (source.x != cornerPosition.x || source.y != cornerPosition.y)) {
				// corner is in the rear face but is not aligned with the appropriate front corner, align the cornerd in both x and y
				if (source.x != cornerPosition.x && source.y != cornerPosition.y) {
					// corners are not aligned in either x or y
					addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
					addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, Direction.CLOCKWISE));
				}
				else if (source.x != cornerPosition.x || source.y != cornerPosition.y) {
					// corners are aligned in either x or y
					Direction direction = ((cornerPosition.isInColumnLeft() && source.isInRowTop()) || (cornerPosition.isInColumnRight() && source.isInRowBottom()))
							? Direction.COUNTER_CLOCKWISE
							: Direction.CLOCKWISE;
					addRotationAndApply(new Rotation(Axis.Z, RubiksCube.FACE_REAR, direction));
				}
			}
			
			cornerCubie = cube.getCubie(cornerPosition);
		}
	}
	
	/*********************************************************************************************************************************************************/

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
	
	private CubiePosition findCubiePositionWithColors(Color ... colors) {
		CubiePosition position = null;
		outerLoop:
			for (int x=0; x<cube.getSize(); x++) {
				for (int y=0; y<cube.getSize(); y++) {
					innerLoop:
						for (int z=0; z<cube.getSize(); z++) {
							position = new CubiePosition(x, y, z);
							List<Color> positionColors = cube.getVisibleColors(position);
							
							if (colors.length != positionColors.size()) continue;
							
							for (Color color : colors) {
								if (!positionColors.contains(color)) continue innerLoop;
							}
							
							break outerLoop;
						}
				}
			}
		return position;
	}
	
	private List<CubiePosition> getFrontEdgePositions() {
		List<CubiePosition> edges = new ArrayList<CubiePosition>();
		edges.add(EDGE_FRONT_TOP);
		edges.add(EDGE_FRONT_BOTTOM);
		edges.add(EDGE_FRONT_LEFT);
		edges.add(EDGE_FRONT_RIGHT);
		return edges;
	}
	
	private List<CubiePosition> getMiddleEdgePositions() {
		List<CubiePosition> edges = new ArrayList<CubiePosition>();
		edges.add(EDGE_MIDDLE_TOP_RIGHT);
		edges.add(EDGE_MIDDLE_BOTTOM_RIGHT);
		edges.add(EDGE_MIDDLE_TOP_LEFT);
		edges.add(EDGE_MIDDLE_BOTTOM_LEFT);
		return edges;
	}
	
	private List<CubiePosition> getRearEdgePositions() {
		List<CubiePosition> edges = new ArrayList<CubiePosition>();
		edges.add(EDGE_REAR_TOP);
		edges.add(EDGE_REAR_BOTTOM);
		edges.add(EDGE_REAR_LEFT);
		edges.add(EDGE_REAR_RIGHT);
		return edges;
	}
	
	private List<CubiePosition> getFrontCornerPositions() {
		List<CubiePosition> corners = new ArrayList<CubiePosition>();
		corners.add(CORNER_FRONT_BOTTOM_LEFT);
		corners.add(CORNER_FRONT_BOTTOM_RIGHT);
		corners.add(CORNER_FRONT_TOP_LEFT);
		corners.add(CORNER_FRONT_TOP_RIGHT);
		return corners;
	}
	
	private List<CubiePosition> getRearCornerPositions() {
		List<CubiePosition> corners = new ArrayList<CubiePosition>();
		corners.add(CORNER_REAR_BOTTOM_LEFT);
		corners.add(CORNER_REAR_BOTTOM_RIGHT);
		corners.add(CORNER_REAR_TOP_LEFT);
		corners.add(CORNER_REAR_TOP_RIGHT);
		return corners;
	}
	
}