/*
 * Ahsan Rabbani <ahsan@xargsgrep.com>
 */

/*
 * Represents a single rotation of a Rubik's Cube. Specifies axis, section, and direction.
 */
package com.xargsgrep.rubikscube;

public class Rotation {
	
	public enum Axis { X, Y, Z; }
	public enum Direction { CLOCKWISE, COUNTER_CLOCKWISE; }
	
	Axis axis;
	int section;
	Direction direction;
	
	public Rotation(Axis axis, int section, Direction direction) {
		this.axis = axis;
		this.section = section;
		this.direction = direction;
	}

	public int getSection() {
		return section;
	}

	public Axis getAxis() {
		return axis;
	}

	public Direction getDirection() {
		return direction;
	}
	
}
