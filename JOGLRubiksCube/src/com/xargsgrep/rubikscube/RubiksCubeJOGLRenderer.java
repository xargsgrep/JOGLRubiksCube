/*
 * Ahsan Rabbani <ahsan@xargsgrep.com>
 */

package com.xargsgrep.rubikscube;

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
import com.xargsgrep.rubikscube.Cubie.Color;
import com.xargsgrep.rubikscube.Rotation.Axis;
import com.xargsgrep.rubikscube.Rotation.Direction;

/*
 * Renders a Rubik's Cube using the JOGL 2.0 library. The size of the cube can be specified with
 * the first argument (default is 3). While any size cube can be rendered and scrambled, there are
 * only enough controls to manipulate a 3x3x3 cube.
 */
@SuppressWarnings("serial")
public class RubiksCubeJOGLRenderer extends GLCanvas implements GLEventListener, KeyListener, MouseListener {
	
	private static final String TITLE = "JOGL 2.0 Rubik's Cube";
	
	private static final int CANVAS_WIDTH  = 640;
	private static final int CANVAS_HEIGHT = 480;
	private static final int FPS = 60;
	
	private static final float ZERO_F = 0.0f;
	private static final float ONE_F  = 1.0f;
	private static final float TWO_F  = 2.0f;
	private static final float CUBIE_GAP_F = 0.1f; // gap between cubies
	private static final float CUBIE_TRANSLATION_FACTOR = TWO_F + CUBIE_GAP_F;
	
	private static final float DEFAULT_CAMERA_ANGLE_X = 45.0f;
	private static final float DEFAULT_CAMERA_ANGLE_Y = 45.0f;
	private static final float DEFAULT_ZOOM = -18.0f;
	
	private static final int SECTION_ROTATE_STEP_DEGREES = 90;
	private static final int CAMERA_ROTATE_STEP_DEGREES  = 5;
	
	private static final int MIN_ZOOM = -80;
	private static final int MAX_ZOOM = -10;
	
	private GLU glu;
	
	private float cameraAngleX = DEFAULT_CAMERA_ANGLE_X;
	private float cameraAngleY = DEFAULT_CAMERA_ANGLE_Y;
	private float cameraAngleZ = ZERO_F;
	private float zoom         = DEFAULT_ZOOM;
	
	private float[] columnAnglesX;
	private float[] rowAnglesY;
	private float[] faceAnglesZ;
	
	private int rotatingSectionX = -1;
	private int rotatingSectionY = -1;
	private int rotatingSectionZ = -1;
	private float angularVelocity = 5.0f; // speed and direction of rotating sections
	
	private int mouseX = 0;
	private int mouseY = 0;
	
	private RubiksCube rubiksCube;
	private boolean scramble = false;

	public RubiksCubeJOGLRenderer(int size) {
		rubiksCube = new RubiksCube(size);
		this.columnAnglesX = new float[size];
		this.rowAnglesY = new float[size];
		this.faceAnglesZ = new float[size];
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
		drawRubiksCube(drawable.getGL().getGL2());
	}
	
	private void drawRubiksCube(GL2 gl) {
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();
		
		// camera transformations
		gl.glTranslatef(ZERO_F, ZERO_F, zoom);
		gl.glRotatef(cameraAngleX, ONE_F, ZERO_F, ZERO_F);
		gl.glRotatef(cameraAngleY, ZERO_F, ONE_F, ZERO_F);
		gl.glRotatef(cameraAngleZ, ZERO_F, ZERO_F, ONE_F);
		
		int lastIdx = rubiksCube.getSize()-1;
		for (int x=0; x<rubiksCube.getSize(); x++) {
			for (int y=0; y<rubiksCube.getSize(); y++) {
				for (int z=0; z<rubiksCube.getSize(); z++) {
					gl.glPushMatrix();
					
					gl.glRotatef(columnAnglesX[x], ONE_F, ZERO_F, ZERO_F);
					gl.glRotatef(rowAnglesY[y], ZERO_F, ONE_F, ZERO_F);
					gl.glRotatef(faceAnglesZ[z], ZERO_F, ZERO_F, ONE_F);
					
					// bottom-left-front corner of cube is (0,0,0) so we need to center it at the origin
					float t = (float) lastIdx/2;
					gl.glTranslatef((x-t)*CUBIE_TRANSLATION_FACTOR, (y-t)*CUBIE_TRANSLATION_FACTOR, -(z-t)*CUBIE_TRANSLATION_FACTOR);
					
					drawCubie(gl, rubiksCube.getVisibleFaces(x, y, z), rubiksCube.getCubie(x, y, z));
						
					gl.glPopMatrix();
				}
			}
		}
	}
	
	private void drawCubie(GL2 gl, int visibleFaces, Cubie cubie) {
		gl.glBegin(GL_QUADS);
		
		// top face
		gl.glColor3f(ZERO_F, ZERO_F, ZERO_F);
		if ((visibleFaces & Cubie.FACELET_TOP) > 0) glApplyColor(gl, cubie.topColor);
		gl.glVertex3f(ONE_F, ONE_F, -ONE_F);
		gl.glVertex3f(-ONE_F, ONE_F, -ONE_F);
		gl.glVertex3f(-ONE_F, ONE_F, ONE_F);
		gl.glVertex3f(ONE_F, ONE_F, ONE_F);
	 
		// bottom face
		gl.glColor3f(ZERO_F, ZERO_F, ZERO_F);
		if ((visibleFaces & Cubie.FACELET_BOTTOM) > 0) glApplyColor(gl, cubie.bottomColor);
		gl.glVertex3f(ONE_F, -ONE_F, ONE_F);
		gl.glVertex3f(-ONE_F, -ONE_F, ONE_F);
		gl.glVertex3f(-ONE_F, -ONE_F, -ONE_F);
		gl.glVertex3f(ONE_F, -ONE_F, -ONE_F);
			 
		// front face
		gl.glColor3f(ZERO_F, ZERO_F, ZERO_F);
		if ((visibleFaces & Cubie.FACELET_FRONT) > 0) glApplyColor(gl, cubie.frontColor);
		gl.glVertex3f(ONE_F, ONE_F, ONE_F);
		gl.glVertex3f(-ONE_F, ONE_F, ONE_F);
		gl.glVertex3f(-ONE_F, -ONE_F, ONE_F);
		gl.glVertex3f(ONE_F, -ONE_F, ONE_F);
			 
		// rear face
		gl.glColor3f(ZERO_F, ZERO_F, ZERO_F);
		if ((visibleFaces & Cubie.FACELET_REAR) > 0) glApplyColor(gl, cubie.rearColor);
		gl.glVertex3f(ONE_F, -ONE_F, -ONE_F);
		gl.glVertex3f(-ONE_F, -ONE_F, -ONE_F);
		gl.glVertex3f(-ONE_F, ONE_F, -ONE_F);
		gl.glVertex3f(ONE_F, ONE_F, -ONE_F);
			 
		// left face
		gl.glColor3f(ZERO_F, ZERO_F, ZERO_F);
		if ((visibleFaces & Cubie.FACELET_LEFT) > 0) glApplyColor(gl, cubie.leftColor);
		gl.glVertex3f(-ONE_F, ONE_F, ONE_F);
		gl.glVertex3f(-ONE_F, ONE_F, -ONE_F);
		gl.glVertex3f(-ONE_F, -ONE_F, -ONE_F);
		gl.glVertex3f(-ONE_F, -ONE_F, ONE_F);
	 
		// right face
		gl.glColor3f(ZERO_F, ZERO_F, ZERO_F);
		if ((visibleFaces & Cubie.FACELET_RIGHT) > 0) glApplyColor(gl, cubie.rightColor);
		gl.glVertex3f(ONE_F, ONE_F, -ONE_F);
		gl.glVertex3f(ONE_F, ONE_F, ONE_F);
		gl.glVertex3f(ONE_F, -ONE_F, ONE_F);
		gl.glVertex3f(ONE_F, -ONE_F, -ONE_F);
			
		gl.glEnd();
	}
	
	private void glApplyColor(GL2 gl, Color color) {
		switch (color) {
			case WHITE:
				gl.glColor3f(ONE_F, ONE_F, ONE_F);
				break;
			case YELLOW:
				gl.glColor3f(ONE_F, ONE_F, ZERO_F);
				break;
			case GREEN:
				gl.glColor3f(ZERO_F, ONE_F, ZERO_F);
				break;
			case ORANGE:
				gl.glColor3f(ONE_F, ONE_F/2, ZERO_F);
				break;
			case BLUE:
				gl.glColor3f(ZERO_F, ZERO_F, ONE_F);
				break;
			case RED:
				gl.glColor3f(ONE_F, ZERO_F, ZERO_F);
				break;
		}
	}
	
	private void updateRotationAngles() {
		if (scramble) {
			Random random = new Random();
			int section = random.nextInt(rubiksCube.getSize());
			Axis axis = Axis.values()[random.nextInt(Axis.values().length)];
			rotateSection(section, axis, (Math.random() < 0.5));
		}
		
		Direction direction = (angularVelocity > 0) ? Direction.COUNTER_CLOCKWISE : Direction.CLOCKWISE;
		if (rotatingSectionX >= 0) {
			columnAnglesX[rotatingSectionX] += angularVelocity;
			if (columnAnglesX[rotatingSectionX] % SECTION_ROTATE_STEP_DEGREES == 0) {
				columnAnglesX[rotatingSectionX] = 0;
				rubiksCube.applyRotation(new Rotation(Axis.X, rotatingSectionX, direction));
				rotatingSectionX = -1;
			}
		}
		else if (rotatingSectionY >= 0) {
			rowAnglesY[rotatingSectionY] += angularVelocity;
			if (rowAnglesY[rotatingSectionY] % SECTION_ROTATE_STEP_DEGREES == 0) {
				rowAnglesY[rotatingSectionY] = 0;
				rubiksCube.applyRotation(new Rotation(Axis.Y, rotatingSectionY, direction));
				rotatingSectionY = -1;
			}
		}
		else if (rotatingSectionZ >= 0) {
			faceAnglesZ[rotatingSectionZ] += angularVelocity;
			if (faceAnglesZ[rotatingSectionZ] % SECTION_ROTATE_STEP_DEGREES == 0) {
				faceAnglesZ[rotatingSectionZ] = 0;
				rubiksCube.applyRotation(new Rotation(Axis.Z, rotatingSectionZ, direction));
				rotatingSectionZ = -1;
			}
		}
	}
	
	private void rotateSection(int section, Axis axis, boolean reverse) {
		// make sure nothing is currently rotating
		if (!isRotating()) {
			if (axis == Axis.X) rotatingSectionX = section;
			if (axis == Axis.Y) rotatingSectionY = section;
			if (axis == Axis.Z) rotatingSectionZ = section;
			angularVelocity = reverse ? -Math.abs(angularVelocity) : Math.abs(angularVelocity);
		}
	}
	
	private boolean isRotating() {
		return rotatingSectionX + rotatingSectionY + rotatingSectionZ > -3;
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
				rotateSection(RubiksCube.COLUMN_LEFT, Axis.X, e.isShiftDown()); break;
			case KeyEvent.VK_W:
				rotateSection(RubiksCube.COLUMN_MIDDLE, Axis.X, e.isShiftDown()); break;
			case KeyEvent.VK_E:
				rotateSection(RubiksCube.COLUMN_RIGHT, Axis.X, e.isShiftDown()); break;
			case KeyEvent.VK_A:
				rotateSection(RubiksCube.ROW_BOTTOM, Axis.Y, e.isShiftDown()); break;
			case KeyEvent.VK_S:
				rotateSection(RubiksCube.ROW_MIDDLE, Axis.Y, e.isShiftDown()); break;
			case KeyEvent.VK_D:
				rotateSection(RubiksCube.ROW_TOP, Axis.Y, e.isShiftDown()); break;
			case KeyEvent.VK_Z:
				rotateSection(RubiksCube.FACE_FRONT, Axis.Z, e.isShiftDown()); break;
			case KeyEvent.VK_X:
				rotateSection(RubiksCube.FACE_MIDDLE, Axis.Z, e.isShiftDown()); break;
			case KeyEvent.VK_C:
				rotateSection(RubiksCube.FACE_REAR, Axis.Z, e.isShiftDown()); break;
			case KeyEvent.VK_J:
				scramble = !scramble; break;
			case KeyEvent.VK_B:
				// TODO: solve
			case KeyEvent.VK_R:
				cameraAngleX = DEFAULT_CAMERA_ANGLE_X;
				cameraAngleY = DEFAULT_CAMERA_ANGLE_Y;
				cameraAngleZ = ZERO_F;
				zoom = DEFAULT_ZOOM;
				if (e.isShiftDown()) {
					columnAnglesX = new float[rubiksCube.getSize()];
					rowAnglesY = new float[rubiksCube.getSize()];
					faceAnglesZ = new float[rubiksCube.getSize()];
					rubiksCube.resetState();
				}
				break;
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
		 
		int size = (args.length == 1) ? Integer.parseInt(args[0]) : 3;
		RubiksCubeJOGLRenderer cube = new RubiksCubeJOGLRenderer(size);
		window.addGLEventListener(cube);
		window.addKeyListener(cube);
		window.addMouseListener(cube);
		window.setSize(CANVAS_WIDTH, CANVAS_HEIGHT);
		window.setTitle(TITLE);
		window.setVisible(true);
		animator.start();	
	}
	
}