# JOGL Rubik's Cube

## Description
A basic OpenGL Rubik's Cube implementation written in Java using JOGL 2.0. The size of the cube can be specified with the first argument (default is 3). While any cube size can be rendered and scrambled, there are only enough controls to manipulate a 3x3x3 cube.

## Controls
#### Camera
Mouse drag:  Rotate around cube
Mouse wheel: Zoom in/out
Left:  Rotate around x-
Up:    Rotate around x+
Right: Rotate around y-
       Hold shift to rotate around z-
Down:  Rotate around y+
	   Hold shift to rotate around z+
R:     Reset camera view
       Hold shift to reset camera view and cube state

Note: Holding shift for the controls below will perform the reverse rotation
#### Column controls
Q:     Rotate left around around x+
W:     Rotate middle around x+
E:     Rotate left around x+

#### Row controls
A:     Rotate top around y+
S:     Rotate middle around y+
D:     Rotate bottom around y+

#### Face controls
Z:     Rotate front around z+
X:     Rotate middle around z+
C:     Rotate rear around z+

#### Other
J:     Toggle cube scramble
B:     Toggle cube solution
