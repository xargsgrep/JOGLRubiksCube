/*
 * Ahsan Rabbani <ahsan@xargsgrep.com>
 */

package com.xargsgrep.rubikscube;

/*
 * Represents a smaller cube (aka cubie/cubelet) in a Rubik's Cube.
 */
public class Cubie {
	
	// bits denoting faces of the cubie
	public static final int FACELET_FRONT  = (1 << 0);
	public static final int FACELET_REAR   = (1 << 1);
	public static final int FACELET_LEFT   = (1 << 2);
	public static final int FACELET_RIGHT  = (1 << 3);
	public static final int FACELET_TOP    = (1 << 4);
	public static final int FACELET_BOTTOM = (1 << 5);
	
	public enum Color { WHITE, YELLOW, GREEN, ORANGE, BLUE, RED; };
	
	Color frontColor = Color.WHITE;
	Color rearColor = Color.YELLOW;
	Color topColor = Color.GREEN;
	Color bottomColor = Color.BLUE;
	Color leftColor = Color.RED;
	Color rightColor = Color.ORANGE;
	
	public Cubie() { }
	
	public Cubie(Color front, Color rear, Color top, Color bottom, Color left, Color right) {
		this.frontColor = front;
		this.rearColor = rear;
		this.topColor = top;
		this.bottomColor = bottom;
		this.leftColor = left;
		this.rightColor = right;
	}
	
	public Cubie getCopy() {
		return new Cubie(frontColor, rearColor, topColor, bottomColor, leftColor, rightColor);
	}

}
