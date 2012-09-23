package com.xargsgrep;

import static javax.media.opengl.GL.GL_COLOR_BUFFER_BIT;
import static javax.media.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static javax.media.opengl.GL.GL_DEPTH_TEST;
import static javax.media.opengl.GL.GL_LEQUAL;
import static javax.media.opengl.GL.GL_NICEST;
import static javax.media.opengl.GL2.GL_QUADS;
import static javax.media.opengl.GL2ES1.GL_PERSPECTIVE_CORRECTION_HINT;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_SMOOTH;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.FPSAnimator;

@SuppressWarnings("serial")
public class JOGLRubiksCube extends GLCanvas implements GLEventListener, KeyListener, MouseListener {
	
	private static final String TITLE = "JOGL 2.0 Rubik's Cube";
	
	private static final int CANVAS_WIDTH  = 640;
	private static final int CANVAS_HEIGHT = 480;
	private static final int FPS = 60;
	
	private static final float ZERO_F = 0.0f;
	private static final float ONE_F  = 1.0f;
	private static final float TWO_F  = 2.0f;
	private static final float CUBLET_GAP_F  = 0.1f; // gap between cubelets
	
	private final float CUBELET_TRANSLATION_UNIT = TWO_F + CUBLET_GAP_F;
	
	private static final float DEFAULT_CAMERA_ANGLE_X = 45.0f;
	private static final float DEFAULT_CAMERA_ANGLE_Y = 45.0f;
	private static final float DEFAULT_ZOOM = -18.0f;
	
	private static final int SECTION_ROTATE_STEP_DEGREES = 90;
	private static final int CAMERA_ROTATE_STEP_DEGREES  = 5;
	
	private static final int MIN_ZOOM = -40;
	private static final int MAX_ZOOM = -10;
	
	// camera controls
	private static final int LEFT_KEY  = 37; // rotate around x -
	private static final int UP_KEY    = 38; // rotate around x +
	private static final int RIGHT_KEY = 39; // rotate around y -
											 // +shift rotate around z -
	private static final int DOWN_KEY  = 40; // rotate around y +
											 // +shift rotate around z +
	private static final int R_KEY     = 82; // reset camera
										 	 // +shift reset camera and cube state
	
	// cube column controls
	private static final int Q_KEY = 81; // rotate right around x +
										 // +shift rotate around x -
	private static final int W_KEY = 87; // rotate middle around x +
										 // +shift rotate around x -
	private static final int E_KEY = 69; // rotate left around x +
										 // +shift rotate around x -
	
	// cube row controls
	private static final int A_KEY = 65; // rotate top around y +
										 // +shift rotate around y -
	private static final int S_KEY = 83; // rotate middle around y +
										 // +shift rotate around y -
	private static final int D_KEY = 68; // rotate bottom around y +
										 // +shift rotate around y -
	
	// cube face controls
	private static final int Z_KEY = 90; // rotate front around z +
										 // +shift rotate around z -
	private static final int X_KEY = 88; // rotate middle around z +
										 // +shift rotate around z -
	private static final int C_KEY = 67; // rotate rear around z +
										 // +shift rotate around z -
	
	// bits for specifying faces on the cubelets
	private final int FACE_CUBELET_FRONT  = 1;
	private final int FACE_CUBELET_REAR   = 2;
	private final int FACE_CUBELET_LEFT   = 4;
	private final int FACE_CUBELET_RIGHT  = 8;
	private final int FACE_CUBELET_TOP    = 16;
	private final int FACE_CUBELET_BOTTOM = 32;
	
	// bits for specifying column/row/face sections of main cube
	private final int SECTION_COLUMN_LEFT   = 1;
	private final int SECTION_COLUMN_MIDDLE = 2;
	private final int SECTION_COLUMN_RIGHT  = 4;
	private final int SECTION_ROW_TOP       = 8;
	private final int SECTION_ROW_MIDDLE    = 16;
	private final int SECTION_ROW_BOTTOM    = 32;
	private final int SECTION_FACE_FRONT    = 64;
	private final int SECTION_FACE_MIDDLE   = 128;
	private final int SECTION_FACE_REAR     = 256;
	
	private GLU glu;
	
	private float cameraAngleX = DEFAULT_CAMERA_ANGLE_X;
	private float cameraAngleY = DEFAULT_CAMERA_ANGLE_Y;
	private float cameraAngleZ = ZERO_F;
	
	private float columnRightAngleX  = ZERO_F;
	private float columnMiddleAngleX = ZERO_F;
	private float columnLeftAngleX   = ZERO_F;
	
	private float rowTopAngleY    = ZERO_F;
	private float rowMiddleAngleY = ZERO_F;
	private float rowBottomAngleY = ZERO_F;

	private float faceFrontAngleZ  = ZERO_F;
	private float faceMiddleAngleZ = ZERO_F;
	private float faceRearAngleZ   = ZERO_F;
	
	private int rotatingSection = 0;
	private float angularVelocity = 5.0f;
	
	private float zoom = DEFAULT_ZOOM;
	
	private int mouseX = 0;
	private int mouseY = 0;
	
	private Cubelet[][][] cubelets;
	
	private enum Color {
		WHITE  { @Override public void glApply(GL2 gl) { gl.glColor3f(ONE_F, ONE_F, ONE_F);    } },
		YELLOW { @Override public void glApply(GL2 gl) { gl.glColor3f(ONE_F, ONE_F, ZERO_F);   } },
		GREEN  { @Override public void glApply(GL2 gl) { gl.glColor3f(ZERO_F, ONE_F, ZERO_F);  } },
		ORANGE { @Override public void glApply(GL2 gl) { gl.glColor3f(ONE_F, ONE_F/2, ZERO_F); } },
		BLUE   { @Override public void glApply(GL2 gl) { gl.glColor3f(ZERO_F, ZERO_F, ONE_F);  } },
		RED    { @Override public void glApply(GL2 gl) { gl.glColor3f(ONE_F, ZERO_F, ZERO_F);  } };
		
		public abstract void glApply(GL2 gl);
	};

	private class Cubelet {
		private Color frontColor = Color.WHITE;
		private Color rearColor = Color.YELLOW;
		private Color topColor = Color.GREEN;
		private Color bottomColor = Color.BLUE;
		private Color leftColor = Color.RED;
		private Color rightColor = Color.ORANGE;
		
		public Cubelet() { }
		
		public Cubelet(Color front, Color rear, Color top, Color bottom, Color left, Color right) {
			this.frontColor = front;
			this.rearColor = rear;
			this.topColor = top;
			this.bottomColor = bottom;
			this.leftColor = left;
			this.rightColor = right;
		}
	}
	
	public JOGLRubiksCube() {
		cubelets = new Cubelet[3][3][3];
		resetCubeState();
	}
	
	@Override
	public void init(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		glu = new GLU();
		gl.glClearColor(ZERO_F, ZERO_F, ZERO_F, ZERO_F);
		gl.glClearDepth(ONE_F); 
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
		gl.glShadeModel(GL_SMOOTH);
	}
	
	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		GL2 gl = drawable.getGL().getGL2();
	      
		if (height == 0) height = 1;
		float aspect = (float) width/height;
		
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluPerspective(45.0, aspect, 0.1, 100.0);
			 
		gl.glMatrixMode(GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		updateRotationAngles();
		drawCube(drawable.getGL().getGL2());
	}
	
	private void drawCube(GL2 gl) {
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();
		
		// camera transformations
		gl.glTranslatef(ZERO_F, ZERO_F, zoom);
		gl.glRotatef(cameraAngleX, ONE_F, ZERO_F, ZERO_F);
		gl.glRotatef(cameraAngleY, ZERO_F, ONE_F, ZERO_F);
		gl.glRotatef(cameraAngleZ, ZERO_F, ZERO_F, ONE_F);
		
		for (int x=0; x<3; x++) {
			for (int y=0; y<3; y++) {
				for (int z=0; z<3; z++) {
					gl.glPushMatrix();
						gl.glRotatef((x == 0) ? columnLeftAngleX : ((x == 1) ? columnMiddleAngleX : columnRightAngleX), ONE_F, ZERO_F, ZERO_F);
						gl.glRotatef((y == 0) ? rowBottomAngleY  : ((y == 1) ? rowMiddleAngleY    : rowTopAngleY),      ZERO_F, ONE_F, ZERO_F);
						gl.glRotatef((z == 0) ? faceFrontAngleZ  : ((z == 1) ? faceMiddleAngleZ   : faceRearAngleZ),    ZERO_F, ZERO_F, ONE_F);
						// center the cube at (0,0,0)
						gl.glTranslatef((x-1)*CUBELET_TRANSLATION_UNIT, (y-1)*CUBELET_TRANSLATION_UNIT, (z-1)*-CUBELET_TRANSLATION_UNIT);
						
						int visibleFaces = (x == 0) ? FACE_CUBELET_LEFT   : ((x == 2) ? FACE_CUBELET_RIGHT : 0);
						visibleFaces    |= (y == 0) ? FACE_CUBELET_BOTTOM : ((y == 2) ? FACE_CUBELET_TOP   : 0);
						visibleFaces    |= (z == 0) ? FACE_CUBELET_FRONT  : ((z == 2) ? FACE_CUBELET_REAR  : 0);
						
						drawCubelet(gl, visibleFaces, cubelets[x][y][z]);
					gl.glPopMatrix();
				}
			}
		}
	}
	
	private void drawCubelet(GL2 gl, int visibleFaces, Cubelet cubelet) {
		gl.glBegin(GL_QUADS);
		
		// top face
		gl.glColor3f(ZERO_F, ZERO_F, ZERO_F);
		if ((visibleFaces & FACE_CUBELET_TOP) > 0) cubelet.topColor.glApply(gl);
		gl.glVertex3f(ONE_F, ONE_F, -ONE_F);
		gl.glVertex3f(-ONE_F, ONE_F, -ONE_F);
		gl.glVertex3f(-ONE_F, ONE_F, ONE_F);
		gl.glVertex3f(ONE_F, ONE_F, ONE_F);
	 
		// bottom face
		gl.glColor3f(ZERO_F, ZERO_F, ZERO_F);
		if ((visibleFaces & FACE_CUBELET_BOTTOM) > 0) cubelet.bottomColor.glApply(gl);
		gl.glVertex3f(ONE_F, -ONE_F, ONE_F);
		gl.glVertex3f(-ONE_F, -ONE_F, ONE_F);
		gl.glVertex3f(-ONE_F, -ONE_F, -ONE_F);
		gl.glVertex3f(ONE_F, -ONE_F, -ONE_F);
			 
		// front face
		gl.glColor3f(ZERO_F, ZERO_F, ZERO_F);
		if ((visibleFaces & FACE_CUBELET_FRONT) > 0) cubelet.frontColor.glApply(gl);
		gl.glVertex3f(ONE_F, ONE_F, ONE_F);
		gl.glVertex3f(-ONE_F, ONE_F, ONE_F);
		gl.glVertex3f(-ONE_F, -ONE_F, ONE_F);
		gl.glVertex3f(ONE_F, -ONE_F, ONE_F);
			 
		// rear face
		gl.glColor3f(ZERO_F, ZERO_F, ZERO_F);
		if ((visibleFaces & FACE_CUBELET_REAR) > 0) cubelet.rearColor.glApply(gl);
		gl.glVertex3f(ONE_F, -ONE_F, -ONE_F);
		gl.glVertex3f(-ONE_F, -ONE_F, -ONE_F);
		gl.glVertex3f(-ONE_F, ONE_F, -ONE_F);
		gl.glVertex3f(ONE_F, ONE_F, -ONE_F);
			 
		// left face
		gl.glColor3f(ZERO_F, ZERO_F, ZERO_F);
		if ((visibleFaces & FACE_CUBELET_LEFT) > 0) cubelet.leftColor.glApply(gl);
		gl.glVertex3f(-ONE_F, ONE_F, ONE_F);
		gl.glVertex3f(-ONE_F, ONE_F, -ONE_F);
		gl.glVertex3f(-ONE_F, -ONE_F, -ONE_F);
		gl.glVertex3f(-ONE_F, -ONE_F, ONE_F);
	 
		// right face
		gl.glColor3f(ZERO_F, ZERO_F, ZERO_F);
		if ((visibleFaces & FACE_CUBELET_RIGHT) > 0) cubelet.rightColor.glApply(gl);
		gl.glVertex3f(ONE_F, ONE_F, -ONE_F);
		gl.glVertex3f(ONE_F, ONE_F, ONE_F);
		gl.glVertex3f(ONE_F, -ONE_F, ONE_F);
		gl.glVertex3f(ONE_F, -ONE_F, -ONE_F);
			
		gl.glEnd();
	}
	
	private void updateRotationAngles() {
		if ((rotatingSection & SECTION_COLUMN_LEFT) == SECTION_COLUMN_LEFT) {
			columnLeftAngleX += angularVelocity;
			if (columnLeftAngleX % SECTION_ROTATE_STEP_DEGREES == 0) {
				rotatingSection = 0;
				columnLeftAngleX = 0;
				swapColorsXRotation(0);
			}
		}
		else if ((rotatingSection & SECTION_COLUMN_MIDDLE) == SECTION_COLUMN_MIDDLE) {
			columnMiddleAngleX += angularVelocity;
			if (columnMiddleAngleX % SECTION_ROTATE_STEP_DEGREES == 0) {
				rotatingSection = 0;
				columnMiddleAngleX = 0;
				swapColorsXRotation(1);
			}
		}
		else if ((rotatingSection & SECTION_COLUMN_RIGHT) == SECTION_COLUMN_RIGHT) {
			columnRightAngleX += angularVelocity;
			if (columnRightAngleX % SECTION_ROTATE_STEP_DEGREES == 0) {
				rotatingSection = 0;
				columnRightAngleX = 0;	
				swapColorsXRotation(2);
			}
		}
		
		else if ((rotatingSection & SECTION_ROW_BOTTOM) == SECTION_ROW_BOTTOM) {
			rowBottomAngleY += angularVelocity;
			if (rowBottomAngleY % SECTION_ROTATE_STEP_DEGREES == 0) {
				rotatingSection = 0;
				rowBottomAngleY = 0;
				swapColorsYRotation(0);
			}
		}
		else if ((rotatingSection & SECTION_ROW_MIDDLE) == SECTION_ROW_MIDDLE) {
			rowMiddleAngleY += angularVelocity;
			if (rowMiddleAngleY % SECTION_ROTATE_STEP_DEGREES == 0) {
				rotatingSection = 0;
				rowMiddleAngleY = 0;
				swapColorsYRotation(1);
			}
		}
		
		else if ((rotatingSection & SECTION_ROW_TOP) == SECTION_ROW_TOP) {
			rowTopAngleY += angularVelocity;
			if (rowTopAngleY % SECTION_ROTATE_STEP_DEGREES == 0) {
				rotatingSection = 0;
				rowTopAngleY = 0;
				swapColorsYRotation(2);
			}
		}
		else if ((rotatingSection & SECTION_FACE_FRONT) == SECTION_FACE_FRONT) {
			faceFrontAngleZ += angularVelocity;
			if (faceFrontAngleZ % SECTION_ROTATE_STEP_DEGREES == 0) {
				rotatingSection = 0;
				faceFrontAngleZ = 0;
				swapColorsZRotation(0);
			}
		}
		else if ((rotatingSection & SECTION_FACE_MIDDLE) == SECTION_FACE_MIDDLE) {
			faceMiddleAngleZ += angularVelocity;
			if (faceMiddleAngleZ % SECTION_ROTATE_STEP_DEGREES == 0) {
				rotatingSection = 0;
				faceMiddleAngleZ = 0;
				swapColorsZRotation(1);
			}
		}
		else if ((rotatingSection & SECTION_FACE_REAR) == SECTION_FACE_REAR) {
			faceRearAngleZ += angularVelocity;
			if (faceRearAngleZ % SECTION_ROTATE_STEP_DEGREES == 0) {
				rotatingSection = 0;
				faceRearAngleZ = 0;
				swapColorsZRotation(2);
			}
		}
	}
	
	private void swapColorsXRotation(int x) {
		Cubelet[][][] copy = copyCubeState(cubelets);
		for (int i=0, ir=2; i<3; i++, ir--) {
			copy[x][2][i].topColor    = (angularVelocity > 0) ? cubelets[x][ir][2].rearColor  : cubelets[x][i][0].frontColor;
			copy[x][0][i].bottomColor = (angularVelocity > 0) ? cubelets[x][ir][0].frontColor : cubelets[x][i][2].rearColor;
			copy[x][i][0].frontColor  = (angularVelocity > 0) ? cubelets[x][2][i].topColor    : cubelets[x][0][ir].bottomColor;
			copy[x][i][2].rearColor   = (angularVelocity > 0) ? cubelets[x][0][i].bottomColor : cubelets[x][2][ir].topColor;
		}
		for (int y=0, yr=2; y<3; y++, yr--) {
			for (int z=0, zr=2; z<3; z++, zr--) {
				copy[x][y][z].leftColor  = (angularVelocity > 0) ? cubelets[x][zr][y].leftColor  : cubelets[x][z][yr].leftColor;
				copy[x][y][z].rightColor = (angularVelocity > 0) ? cubelets[x][zr][y].rightColor : cubelets[x][z][yr].rightColor;
			}
		}
		cubelets = copy;
	}
	
	private void swapColorsYRotation(int y) {	
		Cubelet[][][] copy = copyCubeState(cubelets);
		for (int i=0, ir=2; i<3; i++, ir--) {
			copy[0][y][i].leftColor  = (angularVelocity > 0) ? cubelets[i][y][2].rearColor   : cubelets[ir][y][0].frontColor;
			copy[2][y][i].rightColor = (angularVelocity > 0) ? cubelets[i][y][0].frontColor  : cubelets[ir][y][2].rearColor;
			copy[i][y][0].frontColor = (angularVelocity > 0) ? cubelets[0][y][ir].leftColor  : cubelets[2][y][i].rightColor;
			copy[i][y][2].rearColor  = (angularVelocity > 0) ? cubelets[2][y][ir].rightColor : cubelets[0][y][i].leftColor;
		}
		for (int x=0, xr=2; x<3; x++, xr--) {
			for (int z=0, zr=2; z<3; z++, zr--) {
				copy[x][y][z].topColor    = (angularVelocity > 0) ? cubelets[z][y][xr].topColor    : cubelets[zr][y][x].topColor;
				copy[x][y][z].bottomColor = (angularVelocity > 0) ? cubelets[z][y][xr].bottomColor : cubelets[zr][y][x].bottomColor;
			}
		}
		cubelets = copy;
	}
	
	private void swapColorsZRotation(int z) {
		Cubelet[][][] copy = copyCubeState(cubelets);
		for (int i=0, ir=2; i<3; i++, ir--) {
			copy[i][2][z].topColor    = (angularVelocity > 0) ? cubelets[2][ir][z].rightColor  : cubelets[0][i][z].leftColor;
			copy[i][0][z].bottomColor = (angularVelocity > 0) ? cubelets[0][ir][z].leftColor   : cubelets[2][i][z].rightColor;
			copy[0][i][z].leftColor   = (angularVelocity > 0) ? cubelets[i][2][z].topColor     : cubelets[ir][0][z].bottomColor;
			copy[2][i][z].rightColor  = (angularVelocity > 0) ? cubelets[i][0][z].bottomColor  : cubelets[ir][2][z].topColor;
		}
		for (int x=0, xr=2; x<3; x++, xr--) {
			for (int y=0, yr=2; y<3; y++, yr--) {
				copy[x][y][z].frontColor = (angularVelocity > 0) ? cubelets[y][xr][z].frontColor : cubelets[yr][x][z].frontColor;
				copy[x][y][z].rearColor  = (angularVelocity > 0) ? cubelets[y][xr][z].rearColor  : cubelets[yr][x][z].rearColor;
			}
		}
		cubelets = copy;
	}
	
	@Override
	public void dispose(GLAutoDrawable drawable) { }
	
	@Override
	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
			case UP_KEY:
				cameraAngleX -= CAMERA_ROTATE_STEP_DEGREES;
				break;
			case DOWN_KEY:
				cameraAngleX += CAMERA_ROTATE_STEP_DEGREES;
				break;
			case LEFT_KEY:
				if (e.isShiftDown()) cameraAngleZ += CAMERA_ROTATE_STEP_DEGREES;
				else cameraAngleY -= CAMERA_ROTATE_STEP_DEGREES;
				break;
			case RIGHT_KEY:
				if (e.isShiftDown()) cameraAngleZ -= CAMERA_ROTATE_STEP_DEGREES;
				else cameraAngleY += CAMERA_ROTATE_STEP_DEGREES;
				break;
			case Q_KEY:
				if (rotatingSection == 0) {
					rotatingSection |= SECTION_COLUMN_LEFT;
					angularVelocity = e.isShiftDown() ? -Math.abs(angularVelocity) : Math.abs(angularVelocity);
				}
				break;
			case W_KEY:
				if (rotatingSection == 0) {
					rotatingSection |= SECTION_COLUMN_MIDDLE;
					angularVelocity = e.isShiftDown() ? -Math.abs(angularVelocity) : Math.abs(angularVelocity);
				}
				break;
			case E_KEY:
				if (rotatingSection == 0) {
					rotatingSection |= SECTION_COLUMN_RIGHT;
					angularVelocity = e.isShiftDown() ? -Math.abs(angularVelocity) : Math.abs(angularVelocity);
				}
				break;
			case A_KEY:
				if (rotatingSection == 0) {
					rotatingSection |= SECTION_ROW_TOP;
					angularVelocity = e.isShiftDown() ? -Math.abs(angularVelocity) : Math.abs(angularVelocity);
				}
				break;
			case S_KEY:
				if (rotatingSection == 0) {
					rotatingSection |= SECTION_ROW_MIDDLE;
					angularVelocity = e.isShiftDown() ? -Math.abs(angularVelocity) : Math.abs(angularVelocity);
				}
				break;
			case D_KEY:
				if (rotatingSection == 0) {
					rotatingSection |= SECTION_ROW_BOTTOM;
					angularVelocity = e.isShiftDown() ? -Math.abs(angularVelocity) : Math.abs(angularVelocity);
				}
				break;
			case Z_KEY:
				if (rotatingSection == 0) {
					rotatingSection |= SECTION_FACE_FRONT;
					angularVelocity = e.isShiftDown() ? -Math.abs(angularVelocity) : Math.abs(angularVelocity);
				}
				break;
			case X_KEY:
				if (rotatingSection == 0) {
					rotatingSection |= SECTION_FACE_MIDDLE;
					angularVelocity = e.isShiftDown() ? -Math.abs(angularVelocity) : Math.abs(angularVelocity);
				}
				break;
			case C_KEY:
				if (rotatingSection == 0) {
					rotatingSection |= SECTION_FACE_REAR;
					angularVelocity = e.isShiftDown() ? -Math.abs(angularVelocity) : Math.abs(angularVelocity);
				}
				break;
			case R_KEY:
				cameraAngleX = DEFAULT_CAMERA_ANGLE_X;
				cameraAngleY = DEFAULT_CAMERA_ANGLE_Y;
				cameraAngleZ = ZERO_F;
				zoom = DEFAULT_ZOOM;
				if (e.isShiftDown()) {
					columnRightAngleX = ZERO_F;
					columnMiddleAngleX = ZERO_F;
					columnLeftAngleX = ZERO_F;
					rowTopAngleY = ZERO_F;
					rowMiddleAngleY = ZERO_F;
					rowBottomAngleY = ZERO_F;
					faceFrontAngleZ = ZERO_F;
					faceMiddleAngleZ = ZERO_F;
					faceRearAngleZ = ZERO_F;
					resetCubeState();
				}
				break;
		}
	}
	
	private void resetCubeState() {
		for (int x=0; x<3; x++) {
			for (int y=0; y<3; y++) {
				for (int z=0; z<3; z++) {
					cubelets[x][y][z] = new Cubelet();
				}
			}
		}
	}
	
	private Cubelet[][][] copyCubeState(Cubelet[][][] src) {
		Cubelet[][][] dest = new Cubelet[3][3][3];
		for (int x=0; x<3; x++) {
			for (int y=0; y<3; y++) {
				for (int z=0; z<3; z++) {
					Cubelet other = src[x][y][z];
					dest[x][y][z] = new Cubelet(other.frontColor, other.rearColor, other.topColor, other.bottomColor, other.leftColor, other.rightColor);
				}
			}
		}
		return dest;
	}
	
	@Override
	public void mouseWheelMoved(MouseEvent e) {
		zoom += 2*e.getWheelRotation();
		if (zoom > MAX_ZOOM) zoom = MAX_ZOOM;
		if (zoom < MIN_ZOOM) zoom = MIN_ZOOM;
	}
	
	@Override
	public void mouseDragged(MouseEvent e) {
		final int buffer = 2;
		
		if (e.getX() < mouseX-buffer) cameraAngleY -= CAMERA_ROTATE_STEP_DEGREES;
		else if (e.getX() > mouseX+buffer) cameraAngleY += CAMERA_ROTATE_STEP_DEGREES;
		
		if (e.getY() < mouseY-buffer) cameraAngleX -= CAMERA_ROTATE_STEP_DEGREES;
		else if (e.getY() > mouseY+buffer) cameraAngleX += CAMERA_ROTATE_STEP_DEGREES;
		
		mouseX = e.getX();
		mouseY = e.getY();
	}
	
	@Override public void keyReleased(KeyEvent e) { }
	@Override public void keyTyped(KeyEvent e) { }
	@Override public void mouseClicked(MouseEvent e) { }
	@Override public void mouseEntered(MouseEvent e) { }
	@Override public void mouseExited(MouseEvent e) { }
	@Override public void mousePressed(MouseEvent e) { }
	@Override public void mouseReleased(MouseEvent e) { }
	@Override public void mouseMoved(MouseEvent e) { }
	
	public static void main(String[] args) {
		GLProfile glp = GLProfile.getDefault();
		GLCapabilities caps = new GLCapabilities(glp);
		GLWindow window = GLWindow.create(caps);
		 
		final FPSAnimator animator = new FPSAnimator(window, FPS, true);
		 
		window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowDestroyNotify(WindowEvent e) {
				new Thread() {
					@Override
					public void run() {
						animator.stop();
						System.exit(0);
					}
				}.start();
			};
		});
		 
		JOGLRubiksCube cube = new JOGLRubiksCube();
		window.addGLEventListener(cube);
		window.addKeyListener(cube);
		window.addMouseListener(cube);
		window.setSize(CANVAS_WIDTH, CANVAS_HEIGHT);
		window.setTitle(TITLE);
		window.setVisible(true);
		animator.start();
	}
	
}