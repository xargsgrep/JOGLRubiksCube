/*
 * Ahsan Rabbani <ahsan@xargsgrep.com>
 */

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

import java.util.Random;

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
	private static final float CUBELET_GAP_F = 0.1f; // gap between cubelets
	
	private final float CUBELET_TRANSLATION_FACTOR = TWO_F + CUBELET_GAP_F;
	
	private static final float DEFAULT_CAMERA_ANGLE_X = 45.0f;
	private static final float DEFAULT_CAMERA_ANGLE_Y = 45.0f;
	private static final float DEFAULT_ZOOM = -18.0f;
	
	private static final int SECTION_ROTATE_STEP_DEGREES = 90;
	private static final int CAMERA_ROTATE_STEP_DEGREES  = 5;
	
	private static final int MIN_ZOOM = -40;
	private static final int MAX_ZOOM = -10;
	
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
	private float zoom         = DEFAULT_ZOOM;
	
	private float[] columnAnglesX;
	private float[] rowAnglesY;
	private float[] faceAnglesZ;
	
	private int rotatingSection = 0;
	private float angularVelocity = 5.0f; // speed and direction of rotating sections
	
	private int mouseX = 0;
	private int mouseY = 0;
	
	private Cubelet[][][] cubelets;
	private int cubeUnits = 3;
	private boolean scramble = false;
	
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
		cubelets = new Cubelet[cubeUnits][cubeUnits][cubeUnits];
		columnAnglesX = new float[cubeUnits];
		rowAnglesY = new float[cubeUnits];
		faceAnglesZ = new float[cubeUnits];
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
		drawCompositeCube(drawable.getGL().getGL2());
	}
	
	private void drawCompositeCube(GL2 gl) {
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();
		
		// camera transformations
		gl.glTranslatef(ZERO_F, ZERO_F, zoom);
		gl.glRotatef(cameraAngleX, ONE_F, ZERO_F, ZERO_F);
		gl.glRotatef(cameraAngleY, ZERO_F, ONE_F, ZERO_F);
		gl.glRotatef(cameraAngleZ, ZERO_F, ZERO_F, ONE_F);
		
		for (int x=0; x<cubeUnits; x++) {
			for (int y=0; y<cubeUnits; y++) {
				for (int z=0; z<cubeUnits; z++) {
					gl.glPushMatrix();
					
					gl.glRotatef(columnAnglesX[x], ONE_F, ZERO_F, ZERO_F);
					gl.glRotatef(rowAnglesY[y], ZERO_F, ONE_F, ZERO_F);
					gl.glRotatef(faceAnglesZ[z], ZERO_F, ZERO_F, ONE_F);
					
					// internal representation of cube has (0,0,0) at the bottom-left-front so we need to center it
					float t = (cubeUnits-1)/2;
					gl.glTranslatef((x-t)*CUBELET_TRANSLATION_FACTOR, (y-t)*CUBELET_TRANSLATION_FACTOR, -(z-t)*CUBELET_TRANSLATION_FACTOR);
					
					int visibleFaces = (x == 0) ? FACE_CUBELET_LEFT   : ((x == cubeUnits-1) ? FACE_CUBELET_RIGHT : 0);
					visibleFaces    |= (y == 0) ? FACE_CUBELET_BOTTOM : ((y == cubeUnits-1) ? FACE_CUBELET_TOP   : 0);
					visibleFaces    |= (z == 0) ? FACE_CUBELET_FRONT  : ((z == cubeUnits-1) ? FACE_CUBELET_REAR  : 0);
					
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
		if (scramble) {
			Random random = new Random();
			rotateSection(new Double(Math.pow(2, random.nextInt(9))).intValue(), (Math.random() < 0.5));
		}
		
		if ((rotatingSection & SECTION_COLUMN_LEFT) == SECTION_COLUMN_LEFT) {
			columnAnglesX[0] += angularVelocity;
			if (columnAnglesX[0] % SECTION_ROTATE_STEP_DEGREES == 0) {
				rotatingSection = 0;
				columnAnglesX[0] = 0;
				swapColorsXRotation(0);
			}
		}
		else if ((rotatingSection & SECTION_COLUMN_MIDDLE) == SECTION_COLUMN_MIDDLE) {
			columnAnglesX[1] += angularVelocity;
			if (columnAnglesX[1] % SECTION_ROTATE_STEP_DEGREES == 0) {
				rotatingSection = 0;
				columnAnglesX[1] = 0;
				swapColorsXRotation(1);
			}
		}
		else if ((rotatingSection & SECTION_COLUMN_RIGHT) == SECTION_COLUMN_RIGHT) {
			columnAnglesX[2] += angularVelocity;
			if (columnAnglesX[2] % SECTION_ROTATE_STEP_DEGREES == 0) {
				rotatingSection = 0;
				columnAnglesX[2] = 0;	
				swapColorsXRotation(2);
			}
		}
		
		else if ((rotatingSection & SECTION_ROW_BOTTOM) == SECTION_ROW_BOTTOM) {
			rowAnglesY[0] += angularVelocity;
			if (rowAnglesY[0] % SECTION_ROTATE_STEP_DEGREES == 0) {
				rotatingSection = 0;
				rowAnglesY[0] = 0;
				swapColorsYRotation(0);
			}
		}
		else if ((rotatingSection & SECTION_ROW_MIDDLE) == SECTION_ROW_MIDDLE) {
			rowAnglesY[1] += angularVelocity;
			if (rowAnglesY[1] % SECTION_ROTATE_STEP_DEGREES == 0) {
				rotatingSection = 0;
				rowAnglesY[1] = 0;
				swapColorsYRotation(1);
			}
		}
		
		else if ((rotatingSection & SECTION_ROW_TOP) == SECTION_ROW_TOP) {
			rowAnglesY[2] += angularVelocity;
			if (rowAnglesY[2] % SECTION_ROTATE_STEP_DEGREES == 0) {
				rotatingSection = 0;
				rowAnglesY[2] = 0;
				swapColorsYRotation(2);
			}
		}
		else if ((rotatingSection & SECTION_FACE_FRONT) == SECTION_FACE_FRONT) {
			faceAnglesZ[0] += angularVelocity;
			if (faceAnglesZ[0] % SECTION_ROTATE_STEP_DEGREES == 0) {
				rotatingSection = 0;
				faceAnglesZ[0] = 0;
				swapColorsZRotation(0);
			}
		}
		else if ((rotatingSection & SECTION_FACE_MIDDLE) == SECTION_FACE_MIDDLE) {
			faceAnglesZ[1] += angularVelocity;
			if (faceAnglesZ[1] % SECTION_ROTATE_STEP_DEGREES == 0) {
				rotatingSection = 0;
				faceAnglesZ[1] = 0;
				swapColorsZRotation(1);
			}
		}
		else if ((rotatingSection & SECTION_FACE_REAR) == SECTION_FACE_REAR) {
			faceAnglesZ[2] += angularVelocity;
			if (faceAnglesZ[2] % SECTION_ROTATE_STEP_DEGREES == 0) {
				rotatingSection = 0;
				faceAnglesZ[2] = 0;
				swapColorsZRotation(2);
			}
		}	
	}
	
	private void swapColorsXRotation(int x) {
		Cubelet[][][] copy = copyCubeState(cubelets);
		int j = cubeUnits-1;
		for (int i=0, ir=cubeUnits-1; i<cubeUnits; i++, ir--) {
			copy[x][j][i].topColor    = (angularVelocity > 0) ? cubelets[x][ir][j].rearColor  : cubelets[x][i][0].frontColor;
			copy[x][0][i].bottomColor = (angularVelocity > 0) ? cubelets[x][ir][0].frontColor : cubelets[x][i][j].rearColor;
			copy[x][i][0].frontColor  = (angularVelocity > 0) ? cubelets[x][j][i].topColor    : cubelets[x][0][ir].bottomColor;
			copy[x][i][j].rearColor   = (angularVelocity > 0) ? cubelets[x][0][i].bottomColor : cubelets[x][j][ir].topColor;
		}
		for (int y=0, yr=cubeUnits-1; y<cubeUnits; y++, yr--) {
			for (int z=0, zr=cubeUnits-1; z<cubeUnits; z++, zr--) {
				copy[x][y][z].leftColor  = (angularVelocity > 0) ? cubelets[x][zr][y].leftColor  : cubelets[x][z][yr].leftColor;
				copy[x][y][z].rightColor = (angularVelocity > 0) ? cubelets[x][zr][y].rightColor : cubelets[x][z][yr].rightColor;
			}
		}
		cubelets = copy;
	}
	
	private void swapColorsYRotation(int y) {	
		Cubelet[][][] copy = copyCubeState(cubelets);
		int j = cubeUnits-1;
		for (int i=0, ir=cubeUnits-1; i<cubeUnits; i++, ir--) {
			copy[0][y][i].leftColor  = (angularVelocity > 0) ? cubelets[i][y][j].rearColor   : cubelets[ir][y][0].frontColor;
			copy[j][y][i].rightColor = (angularVelocity > 0) ? cubelets[i][y][0].frontColor  : cubelets[ir][y][j].rearColor;
			copy[i][y][0].frontColor = (angularVelocity > 0) ? cubelets[0][y][ir].leftColor  : cubelets[j][y][i].rightColor;
			copy[i][y][j].rearColor  = (angularVelocity > 0) ? cubelets[j][y][ir].rightColor : cubelets[0][y][i].leftColor;
		}
		for (int x=0, xr=cubeUnits-1; x<cubeUnits; x++, xr--) {
			for (int z=0, zr=cubeUnits-1; z<cubeUnits; z++, zr--) {
				copy[x][y][z].topColor    = (angularVelocity > 0) ? cubelets[z][y][xr].topColor    : cubelets[zr][y][x].topColor;
				copy[x][y][z].bottomColor = (angularVelocity > 0) ? cubelets[z][y][xr].bottomColor : cubelets[zr][y][x].bottomColor;
			}
		}
		cubelets = copy;
	}
	
	private void swapColorsZRotation(int z) {
		Cubelet[][][] copy = copyCubeState(cubelets);
		int j = cubeUnits-1;
		for (int i=0, ir=cubeUnits-1; i<cubeUnits; i++, ir--) {
			copy[i][j][z].topColor    = (angularVelocity > 0) ? cubelets[j][ir][z].rightColor  : cubelets[0][i][z].leftColor;
			copy[i][0][z].bottomColor = (angularVelocity > 0) ? cubelets[0][ir][z].leftColor   : cubelets[j][i][z].rightColor;
			copy[0][i][z].leftColor   = (angularVelocity > 0) ? cubelets[i][j][z].topColor     : cubelets[ir][0][z].bottomColor;
			copy[j][i][z].rightColor  = (angularVelocity > 0) ? cubelets[i][0][z].bottomColor  : cubelets[ir][j][z].topColor;
		}
		for (int x=0, xr=cubeUnits-1; x<cubeUnits; x++, xr--) {
			for (int y=0, yr=cubeUnits-1; y<cubeUnits; y++, yr--) {
				copy[x][y][z].frontColor = (angularVelocity > 0) ? cubelets[y][xr][z].frontColor : cubelets[yr][x][z].frontColor;
				copy[x][y][z].rearColor  = (angularVelocity > 0) ? cubelets[y][xr][z].rearColor  : cubelets[yr][x][z].rearColor;
			}
		}
		cubelets = copy;
	}
	
	
	private void resetCubeState() {
		for (int x=0; x<cubeUnits; x++) {
			for (int y=0; y<cubeUnits; y++) {
				for (int z=0; z<cubeUnits; z++) {
					cubelets[x][y][z] = new Cubelet();
				}
			}
		}
	}
	
	private Cubelet[][][] copyCubeState(Cubelet[][][] src) {
		Cubelet[][][] dest = new Cubelet[cubeUnits][cubeUnits][cubeUnits];
		for (int x=0; x<cubeUnits; x++) {
			for (int y=0; y<cubeUnits; y++) {
				for (int z=0; z<cubeUnits; z++) {
					Cubelet other = src[x][y][z];
					dest[x][y][z] = new Cubelet(other.frontColor, other.rearColor, other.topColor, other.bottomColor, other.leftColor, other.rightColor);
				}
			}
		}
		return dest;
	}
	
	@Override
	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
			case KeyEvent.VK_UP:
				cameraAngleX -= CAMERA_ROTATE_STEP_DEGREES;
				break;
			case KeyEvent.VK_DOWN:
				cameraAngleX += CAMERA_ROTATE_STEP_DEGREES;
				break;
			case KeyEvent.VK_LEFT:
				if (e.isShiftDown()) cameraAngleZ += CAMERA_ROTATE_STEP_DEGREES;
				else cameraAngleY -= CAMERA_ROTATE_STEP_DEGREES;
				break;
			case KeyEvent.VK_RIGHT:
				if (e.isShiftDown()) cameraAngleZ -= CAMERA_ROTATE_STEP_DEGREES;
				else cameraAngleY += CAMERA_ROTATE_STEP_DEGREES;
				break;
			case KeyEvent.VK_Q:
				rotateSection(SECTION_COLUMN_LEFT, e.isShiftDown());
				break;
			case KeyEvent.VK_W:
				rotateSection(SECTION_COLUMN_MIDDLE, e.isShiftDown());
				break;
			case KeyEvent.VK_E:
				rotateSection(SECTION_COLUMN_RIGHT, e.isShiftDown());
				break;
			case KeyEvent.VK_A:
				rotateSection(SECTION_ROW_TOP, e.isShiftDown());
				break;
			case KeyEvent.VK_S:
				rotateSection(SECTION_ROW_MIDDLE, e.isShiftDown());
				break;
			case KeyEvent.VK_D:
				rotateSection(SECTION_ROW_BOTTOM, e.isShiftDown());
				break;
			case KeyEvent.VK_Z:
				rotateSection(SECTION_FACE_FRONT, e.isShiftDown());
				break;
			case KeyEvent.VK_X:
				rotateSection(SECTION_FACE_MIDDLE, e.isShiftDown());
				break;
			case KeyEvent.VK_C:
				rotateSection(SECTION_FACE_REAR, e.isShiftDown());
				break;
			case KeyEvent.VK_J:
				scramble = !scramble;
				break;
			case KeyEvent.VK_R:
				cameraAngleX = DEFAULT_CAMERA_ANGLE_X;
				cameraAngleY = DEFAULT_CAMERA_ANGLE_Y;
				cameraAngleZ = ZERO_F;
				zoom = DEFAULT_ZOOM;
				if (e.isShiftDown()) {
					columnAnglesX = new float[cubeUnits];
					rowAnglesY = new float[cubeUnits];
					faceAnglesZ = new float[cubeUnits];
					resetCubeState();
				}
				break;
		}
	}
	
	private void rotateSection(int section, boolean reverse) {
		if (rotatingSection == 0) {
			rotatingSection |= section;
			angularVelocity = reverse ? -Math.abs(angularVelocity) : Math.abs(angularVelocity);
		}
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
	
	@Override public void dispose(GLAutoDrawable drawable) { }
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