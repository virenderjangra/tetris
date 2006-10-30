package general;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;


public class Ciclo extends JPanel implements Runnable {
	
  private static final long serialVersionUID = 1L;
  private static final int PWIDTH = 500;       // size of panel
  private static final int PHEIGHT = 400;

  private Thread animator;                     // for the animation
  private volatile boolean running = false;    // stops the animation  
  private volatile boolean gameOver = false;   // for game termination


  public void GamePanel() {
	setBackground(Color.white);                // white background
    setPreferredSize( new Dimension(PWIDTH, PHEIGHT));
    //
    // create game components
    // ...
  }


  /* Wait for the JPanel to be added to the
  JFrame/JApplet before starting. */
  public void addNotify() {
    super.addNotify();     // creates the peer
    startGame();           // start the thread
  }

  
  private void startGame()
  // initialise and start the thread
  {
    if (animator == null || !running) {
      animator = new Thread(this);
      animator.start();
    }
  }
  
  
  public void stopGame() {  
	running = false;   
  }


  /* Repeatedly update, render, sleep */
  public void run() {
    running = true;
    while(running) {
      gameUpdate();
      gameRender();
      repaint();

      try {
        Thread.sleep(20);  // sleep a bit
      }
      catch(final InterruptedException ex){}
    }    
    //  so enclosing JFrame/JApplet exits
    System.exit(0); 
  }


  private void gameUpdate() { 
    if (!gameOver){
    	// update game state ...
	}
  }
  
  
  private void gameRender() {	  
		
  }
  
  // more methods, explained later...
}
