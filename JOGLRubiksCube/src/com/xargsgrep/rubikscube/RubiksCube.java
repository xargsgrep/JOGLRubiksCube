/*
 * Ahsan Rabbani <ahsan@xargsgrep.com>
 */

package com.xargsgrep.rubikscube;

import com.xargsgrep.rubikscube.Rotation.Axis;

/*
 * Represents a Rubik's Cube using a 3-dimensional array of Cubies. This implementation supports cubes of any size.
 * Rotations are applied using the Rotation class.
 */
public class RubiksCube {
	
	// constants specifying individual sections of a 3x3x3 cube
	public static final int COLUMN_LEFT   = 0;
	public static final int COLUMN_MIDDLE = 1;
	public static final int COLUMN_RIGHT  = 2;
	public static final int ROW_BOTTOM    = 0;
	public static final int ROW_MIDDLE    = 1;
	public static final int ROW_TOP       = 2;
	public static final int FACE_FRONT    = 0;
	public static final int FACE_MIDDLE   = 1;
	public static final int FACE_REAR     = 2;

	private final int size;
	private Cubie[][][] state;

	public RubiksCube(int size) {
		this.size = size;
		this.state = new Cubie[size][size][size];
		resetState();
	}
	
	public RubiksCube(Cubie[][][] state) {
		this.size = state.length;
		this.state = state;
	}
	
	public int getSize() {
		return size;
	}
	
	public Cubie[][][] getState() {
		return state;
	}
	
	public Cubie getCubie(int x, int y, int z) {
		return state[x][y][z];
	}
	
	// returns integer denoting which faces on the specified cubie are visible
	public int getVisibleFaces(int x, int y, int z) {
		int lastIdx = size-1;
		int visibleFaces = (x == 0) ? Cubie.FACELET_LEFT   : ((x == lastIdx) ? Cubie.FACELET_RIGHT : 0);
		visibleFaces    |= (y == 0) ? Cubie.FACELET_BOTTOM : ((y == lastIdx) ? Cubie.FACELET_TOP   : 0);
		visibleFaces    |= (z == 0) ? Cubie.FACELET_FRONT  : ((z == lastIdx) ? Cubie.FACELET_REAR  : 0);
		return visibleFaces;
	}
	
	public void applyRotation(Rotation rotation) {
		if (rotation.getSection() >= size)
			throw new RuntimeException("Specified rotation section is out of bounds: " + rotation.getSection());
			
		if (rotation.getAxis() == Axis.X)
			applyXRotation(rotation);
		else if (rotation.getAxis() == Axis.Y)
			applyYRotation(rotation);
		else if (rotation.getAxis() == Axis.Z)
			applyZRotation(rotation);
	}
	
	public void resetState() {
		for (int x=0; x<size; x++) {
			for (int y=0; y<size; y++) {
				for (int z=0; z<size; z++) {
					state[x][y][z] = new Cubie();
				}
			}
		}
	}
	
	public RubiksCube getCopy() {
		return new RubiksCube(copyState());
	}
	
	private void applyXRotation(Rotation rotation) {
		int x = rotation.getSection();
		int j = size-1;
		
		Cubie[][][] copy = copyState();
		for (int i=0, ir=size-1; i<size; i++, ir--) {
			copy[x][j][i].topColor    = rotation.isClockwise() ? state[x][i][0].frontColor   : state[x][ir][j].rearColor;
			copy[x][0][i].bottomColor = rotation.isClockwise() ? state[x][i][j].rearColor    : state[x][ir][0].frontColor;
			copy[x][i][0].frontColor  = rotation.isClockwise() ? state[x][0][ir].bottomColor : state[x][j][i].topColor;
			copy[x][i][j].rearColor   = rotation.isClockwise() ? state[x][j][ir].topColor    : state[x][0][i].bottomColor;
		}
		for (int y=0, yr=size-1; y<size; y++, yr--) {
			for (int z=0, zr=size-1; z<size; z++, zr--) {
				copy[x][y][z].leftColor  = rotation.isClockwise() ? state[x][z][yr].leftColor  : state[x][zr][y].leftColor;
				copy[x][y][z].rightColor = rotation.isClockwise() ? state[x][z][yr].rightColor : state[x][zr][y].rightColor;
			}
		}
		state = copy;
	}
	
	private void applyYRotation(Rotation rotation) {	
		int y = rotation.getSection();
		int j = size-1;
		
		Cubie[][][] copy = copyState();
		for (int i=0, ir=size-1; i<size; i++, ir--) {
			copy[0][y][i].leftColor  = rotation.isClockwise() ? state[ir][y][0].frontColor :  state[i][y][j].rearColor;
			copy[j][y][i].rightColor = rotation.isClockwise() ? state[ir][y][j].rearColor  : state[i][y][0].frontColor;
			copy[i][y][0].frontColor = rotation.isClockwise() ? state[j][y][i].rightColor  : state[0][y][ir].leftColor;
			copy[i][y][j].rearColor  = rotation.isClockwise() ? state[0][y][i].leftColor   : state[j][y][ir].rightColor;
		}
		for (int x=0, xr=size-1; x<size; x++, xr--) {
			for (int z=0, zr=size-1; z<size; z++, zr--) {
				copy[x][y][z].topColor    = rotation.isClockwise() ? state[zr][y][x].topColor    : state[z][y][xr].topColor;
				copy[x][y][z].bottomColor = rotation.isClockwise() ? state[zr][y][x].bottomColor : state[z][y][xr].bottomColor;
			}
		}
		state = copy;
	}
	
	private void applyZRotation(Rotation rotation) {
		int z = rotation.getSection();
		int j = size-1;
		
		Cubie[][][] copy = copyState();
		for (int i=0, ir=size-1; i<size; i++, ir--) {
			copy[i][j][z].topColor    = rotation.isClockwise() ? state[0][i][z].leftColor    : state[j][ir][z].rightColor;
			copy[i][0][z].bottomColor = rotation.isClockwise() ? state[j][i][z].rightColor   : state[0][ir][z].leftColor;
			copy[0][i][z].leftColor   = rotation.isClockwise() ? state[ir][0][z].bottomColor : state[i][j][z].topColor;
			copy[j][i][z].rightColor  = rotation.isClockwise() ? state[ir][j][z].topColor    : state[i][0][z].bottomColor;
		}
		for (int x=0, xr=size-1; x<size; x++, xr--) {
			for (int y=0, yr=size-1; y<size; y++, yr--) {
				copy[x][y][z].frontColor = rotation.isClockwise() ? state[yr][x][z].frontColor : state[y][xr][z].frontColor;
				copy[x][y][z].rearColor  = rotation.isClockwise() ? state[yr][x][z].rearColor  : state[y][xr][z].rearColor;
			}
		}
		state = copy;
	}
	
	private Cubie[][][] copyState() {
		Cubie[][][] dest = new Cubie[size][size][size];
		for (int x=0; x<size; x++) {
			for (int y=0; y<size; y++) {
				for (int z=0; z<size; z++) {
					dest[x][y][z] = state[x][y][z].getCopy();
				}
			}
		}
		return dest;
	}

}
