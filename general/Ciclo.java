package general;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import javax.swing.JPanel;


public class Ciclo extends JPanel implements Runnable {
	
  private static final long serialVersionUID = 1L;
  private static final int PWIDTH = 500;       // size of panel
  private static final int PHEIGHT = 400;

  private Thread animator;                     // for the animation
  private volatile boolean running = false;    // stops the animation  
  private volatile boolean gameOver = false;   // for game termination
  private volatile boolean isPaused = false;   // for game pause
    
  // global variables for off-screen rendering
  private Graphics dbg;
  private Image dbImage = null;
  private long period;
  private double fpsStore[];
  private Font font;
  private FontMetrics metrics;
  private Juego juego;
  private String gameOverString = "Perdiste looser!!!!!";
   
  private DecimalFormat df = new DecimalFormat("0.##");  // 2 dp
  private DecimalFormat timedf = new DecimalFormat("0.####");  // 4 dp
  
  public Ciclo(Juego j, long period) {
    this.juego = j;
	setBackground(Color.white);
    setPreferredSize(new Dimension(PWIDTH, PHEIGHT));

    setFocusable(true);
    requestFocus();    // JPanel now receives key events
    readyForTermination();

    // create game components
    // ...

    // listen for mouse presses
    addMouseListener(new MouseAdapter() {
	  public void mousePressed(MouseEvent e) 
	  {testPress(e.getX( ), e.getY( )); }
    });
    
    //  set up message font
    font = new Font("SansSerif", Font.BOLD, 24);
    metrics = this.getFontMetrics(font);    
    
    
    // initialise timing elements
    fpsStore = new double[NUM_FPS];
    upsStore = new double[NUM_FPS];
    for (int i=0; i < NUM_FPS; i++) {
      fpsStore[i] = 0.0;
      upsStore[i] = 0.0;
    }
  }

  private void readyForTermination() {
    addKeyListener( new KeyAdapter(){
      // listen for esc, q, end, ctrl-c
      public void keyPressed(KeyEvent e){ 
    	int keyCode = e.getKeyCode( );
        if ((keyCode == KeyEvent.VK_ESCAPE) ||
            (keyCode == KeyEvent.VK_Q) ||
            (keyCode == KeyEvent.VK_END) ||
            ((keyCode == KeyEvent.VK_C) && e.isControlDown( )) ) {
          running = false;
        }
       }
     });
  }
  
  // is (x,y) important to the game?
  private void testPress(int x, int y) {
    if (!gameOver) {
      // do something
    }
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
  
    
  /* Repeatedly update, render, sleep */
  public void run1() {
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
    System.exit(0); 
  }
  
  /* Active Rendering */
  public void run2(){
    running = true;
    while(running) {
      gameUpdate();
      gameRender();
      paintScreen();

      try {
        Thread.sleep(20);  // sleep a bit
      }
      catch(InterruptedException ex){}
    }
    System.exit(0);
  }
  
  public void run3() {
  /* Repeatedly: update, render, sleep so loop takes close
     to period ms */
  
    long beforeTime, timeDiff, sleepTime;
    beforeTime = System.currentTimeMillis();

    running = true;
    while(running) {
      gameUpdate();
      gameRender();
      paintScreen();

      timeDiff = System.currentTimeMillis( ) - beforeTime;
      sleepTime = period - timeDiff;   // time left in this loop

      if (sleepTime <= 0)  // update/render took longer than period
        sleepTime = 5;    // sleep a bit anyway

      try {
        Thread.sleep(sleepTime);  // in ms
      }
      catch(InterruptedException ex){}

      beforeTime = System.currentTimeMillis( );
    }

    System.exit(0);
  }
  
  
  private static int MAX_FRAME_SKIPS = 5;
  // no. of frames that can be skipped in any one animation loop
  // i.e the games state is updated but not rendered

  private static final int NO_DELAYS_PER_YIELD = 16;
  /* Number of frames with a delay of 0 ms before the
     animation thread yields to other running threads. */


  /* Repeatedly update, render, sleep so loop takes close
  to period nsecs. Sleep inaccuracies are handled.
  The timing calculation use the Java 3D timer.
*/
  public void run() {
    long beforeTime, afterTime, timeDiff, sleepTime;
    long overSleepTime = 0L;
    int noDelays = 0;
    long excess = 0L;

    gameStartTime = System.nanoTime();    
    prevStatsTime = gameStartTime;
    beforeTime = gameStartTime;

    running = true;
    while(running) {
      gameUpdate();
      gameRender();
      paintScreen();

      afterTime = System.nanoTime();
      timeDiff = afterTime - beforeTime;
      sleepTime = (period - timeDiff) - overSleepTime;

      if (sleepTime > 0) {   // some time left in this cycle
        try {
          Thread.sleep(sleepTime/1000000L);  // nano -> ms
        }
        catch(InterruptedException ex){}
        overSleepTime =
              (System.nanoTime() - afterTime) - sleepTime;
      }
      else {    // sleepTime <= 0; frame took longer than the period
    	excess -= sleepTime;  // store excess time value
    	overSleepTime = 0L;

        if (++noDelays >= NO_DELAYS_PER_YIELD) {
          Thread.yield();   // give another thread a chance to run
          noDelays = 0;
        }
      }
      beforeTime = System.nanoTime();
      
      /* If frame animation is taking too long, update the game state
      without rendering it, to get the updates/sec nearer to
      the required FPS. */
      int skips = 0;
	  while((excess > period) && (skips < MAX_FRAME_SKIPS)) {
	    excess -= period;
	    gameUpdate( );      // update state but don't render
	    skips++;
	  }
	  
	  framesSkipped += skips;
      storeStats();
    }
    
    printStats();
    System.exit(0);
  }
  
  private void printStats() {
    System.out.println("Frame Count/Loss: " + frameCount +
                               " / " + totalFramesSkipped);
    System.out.println("Average FPS: " + df.format(averageFPS));
    System.out.println("Average UPS: " + df.format(averageUPS));
    System.out.println("Time Spent: " + timeSpentInGame + " secs");    
  }
  
  //used for gathering statistics
  // many, see "Swing Timer Animation" section, chapter 2
  private long gameStartTime;
  private long prevStatsTime;
  private int timeSpentInGame = 0;    // in seconds
	
  private long framesSkipped = 0L;
  private long totalFramesSkipped = 0L;
  private double upsStore[];
  private double averageUPS = 0.0;
  
  private static long MAX_STATS_INTERVAL = 1000L;
  // record stats every 1 second (roughly)

  //used for gathering statistics
  private long statsInterval = 0L;    // in ms
  private static int NUM_FPS = 10;
  private long frameCount = 0;
  private long totalElapsedTime = 0L;
  private int statsCount;
  private double averageFPS = 0.0;


	
	
  private void storeStats() {
	frameCount++;
	statsInterval += period;
	
	if (statsInterval >= MAX_STATS_INTERVAL) {
	  long timeNow = System.nanoTime();
	  timeSpentInGame = (int) ((timeNow - gameStartTime)/1000000000L);  // ns-->secs
	  this.juego.setTimeSpent(timeSpentInGame);
	
	  long realElapsedTime = timeNow - prevStatsTime;
	       // time since last stats collection
	  totalElapsedTime += realElapsedTime;
	
	  double timingError = (double)
	       (realElapsedTime-statsInterval) / statsInterval*100.0;
	
	  totalFramesSkipped += framesSkipped;
	
	  double actualFPS = 0;     // calculate the latest FPS and UPS
	  double actualUPS = 0;
	  if (totalElapsedTime > 0) {
	    actualFPS = (((double)frameCount / totalElapsedTime) *
	                                             1000000000L);
	    actualUPS = (((double)(frameCount + totalFramesSkipped) /
	                 totalElapsedTime) * 1000000000L);
	  }
	
	  // store the latest FPS and UPS
	  fpsStore[ (int)statsCount%NUM_FPS ] = actualFPS;
	  upsStore[ (int)statsCount%NUM_FPS ] = actualUPS;
	  statsCount = statsCount+1;
	
	  double totalFPS = 0.0;     // total the stored FPSs and UPSs
	  double totalUPS = 0.0;
	  for (int i=0; i < NUM_FPS; i++) {
	    totalFPS += fpsStore[i];
	    totalUPS += upsStore[i];
	  }
	
	  if (statsCount < NUM_FPS) { // obtain the average FPS and UPS
	    averageFPS = totalFPS/statsCount;
	    averageUPS = totalUPS/statsCount;
	  }
	  else {
	    averageFPS = totalFPS/NUM_FPS;
	    averageUPS = totalUPS/NUM_FPS;
	  }
	/*
	 System.out.println(
	   timedf.format( (double) statsInterval/1000000000L) + " " +
	   timedf.format((double) realElapsedTime/1000000000L)+"s "+
	   df.format(timingError) + "% " +
	   frameCount + "c " +
	   framesSkipped + "/" + totalFramesSkipped + " skip; " +
	   df.format(actualFPS) + " " + df.format(averageFPS)+" afps; " +
	   df.format(actualUPS) + " " + df.format(averageUPS)+" aups" );
	*/
	  framesSkipped = 0;
	  prevStatsTime = timeNow;
	  statsInterval = 0L;   // reset
	}
  }
  
  //actively render the buffer image to the screen
  private void paintScreen(){
    Graphics g;
    try {
      g = this.getGraphics( );  // get the panel's graphic context
      if ((g != null) && (dbImage != null))
        g.drawImage(dbImage, 0, 0, null);
      Toolkit.getDefaultToolkit( ).sync( );  // sync the display on some systems
      g.dispose( );
    }
    catch (Exception e)
    { System.out.println("Graphics context error: " + e);  }
  }


  private void gameUpdate() { 
	  if (!isPaused && !gameOver) {
    	// update game state ...
	}
  }  
  
  /* draw the current frame to an image buffer */
  private void gameRender() {
	// create the buffer
    if (dbImage == null){  
    	
      dbImage = createImage(PWIDTH, PHEIGHT);
      if (dbImage == null) {
        System.out.println("dbImage is null");
        return;
      } else
        dbg = dbImage.getGraphics( );
    }

    // clear the background
    dbg.setColor(Color.white);
    dbg.fillRect (0, 0, PWIDTH, PHEIGHT);

    // draw game elements
    dbg.setColor(Color.blue);
    dbg.setFont(font);
    

    //  report average FPS and UPS at top left
    dbg.drawString("Average FPS/UPS: " + df.format(averageFPS) +
             ", " + df.format(averageUPS), 20, 25);

    dbg.setColor(Color.black);

    
    if (gameOver)
      gameOverMessage(dbg);
  }
  
  public void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    if (dbImage != null)
      g.drawImage(dbImage, 0, 0, null);
  }

  private void gameOverMessage(Graphics g) { 
	// code to calculate x and y...
    g.drawString(gameOverString, 20, 20);
  } 

  public void resumeGame()
  // called when the JFrame is activated / deiconified
  {  isPaused = false;  }

  public void pauseGame()
  // called when the JFrame is deactivated / iconified
  { isPaused = true;   }

  public void stopGame()
  // called when the JFrame is closing
  {  running = false;   }  
  
}
