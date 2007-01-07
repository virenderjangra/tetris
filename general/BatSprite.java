//BatSprite.java
//Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th

/* The bat can move in a horizontal direction only, along the
floor. It is controlled by the arrow keys or mouse
presses. 

As the bat leaves one side of the panel, it appears on
the other side.

The bat is assigned a left-facing and right-facing set
of images (an ant), which are cycled throught.

The ant images come from the SpriteLib sprite library by 
Ari Feldman at http://www.arifeldman.com/games/spritelib.html
*/

package general;

public class BatSprite extends Sprite
{
private static double DURATION = 0.5;  // secs
 // total time to cycle through all the images

private static final int FLOOR_DIST = 41;   
   // distance of ant's top from the floor
private static final int XSTEP = 10;   
  // step distance for moving along x-axis


private int period;
/* in ms. The game's animation period used by the image
   cycling of the bat's left and right facing images. */


public BatSprite(int w, int h, ImagesLoader imsLd, int p) 
{ 
 super( w/2, h-FLOOR_DIST, w, h, imsLd, "leftBugs2"); 
                  // positioned at the bottom of the panel, near the center
 period = p;
 setStep(0,0);  // no movement
} // end of BatSprite()


public void moveLeft()
// start the ant moving left
{ 
 setStep(-XSTEP, 0);
 setImage("leftBugs2");
 loopImage(period, DURATION);   // cycle through the leftBugs2 images
} // end of moveLeft()


public void moveRight()
// start the ant moving right
{ 
 setStep(XSTEP, 0); 
 setImage("rightBugs2");
 loopImage(period, DURATION);  // cycle through the images
} // end of moveRight()


public void stayStill()
// stop the ant moving
{ setStep(0, 0); 
 stopLooping();
}


public void updateSprite() 
// have the bat wrap-around at the walls
{
 if ((locx+getWidth() <= 0) && (dx < 0))   // almost gone off lhs
   locx = getPWidth()-1;      // make it just visible on the right
 else if ((locx >= getPWidth()-1) && (dx > 0))  // almost gone off rhs
   locx = 1 - getWidth();     // make it just visible on the left

 super.updateSprite();
} // end of updateSprite()



public void mouseMove(int xCoord)
// start the ant moving based on a mouse click
{
if (xCoord < locx)  // click was to the left of the bat
  moveLeft();       // make the bat move left
else if (xCoord > (locx + getWidth()))  // click was to the right of the bat
  moveRight();      // make the bat move right
else
  stayStill();
}  // end of moveMove()

}  // end of BatSprite class
