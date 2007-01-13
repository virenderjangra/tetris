package general;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import java.text.DecimalFormat;


public class Ciclo extends JPanel implements Runnable {
	  
  private static final int PWIDTH = 500;       // size of panel
  private static final int PHEIGHT = 400;
  
  private static long MAX_STATS_INTERVAL = 1000L;
  // record stats every 1 second (roughly)
  
  private static final int NO_DELAYS_PER_YIELD = 16;
  /* Number of frames with a delay of 0 ms before the
     animation thread yields to other running threads. */
  
  private static int MAX_FRAME_SKIPS = 5;
  // no. of frames that can be skipped in any one animation loop
  // i.e the games state is updated but not rendered

  private static int NUM_FPS = 10;
  // number of FPS values stored to get an average
  
  //used for gathering statistics
  private long statsInterval = 0L;    // in ms  
  private long prevStatsTime;
  private long totalElapsedTime = 0L;
  private long gameStartTime;
  private int timeSpentInGame = 0;    // in seconds
  
  private long frameCount = 0;  
  private double fpsStore[];
  private int statsCount;
  private double averageFPS = 0.0;  
	
  private long framesSkipped = 0L;
  private long totalFramesSkipped = 0L;
  private double upsStore[];
  private double averageUPS = 0.0;
  
  private DecimalFormat df = new DecimalFormat("0.##");  // 2 dp
  private DecimalFormat timedf = new DecimalFormat("0.####");  // 4 dp  

  //image and clip loader information files
  private static final String IMS_INFO = "imsInfo.txt";
  private static final String SNDS_FILE = "clipsInfo.txt";
  
  private Thread animator;                     // for the animation
  private volatile boolean running = false;    // stops the animation  
  private volatile boolean isPaused = false;   // for game pause
      
  private long period;
  
  private ClipsLoader clipsLoader;
  
  private BatSprite bat;        // the sprites
  
  
  /* el juego*/
  private Juego juego;  
  //TODO: ver porque no usa el gameOverString 
  private String gameOverString = "Perdiste looser!!!!!";
  
  // used at game termination
  private volatile boolean gameOver = false;   // for game termination
  private Font font;
  private FontMetrics metrics;
  
  // global variables for off-screen rendering
  private Graphics dbg;
  private Image dbImage = null;
  
  
  public Ciclo(Juego j, long period) {
	
	this.juego = j;
	this.period = period;  
	  
	setBackground(Color.white);
    setPreferredSize(new Dimension(PWIDTH, PHEIGHT));

    setFocusable(true);
    requestFocus();    // JPanel now receives key events

    //TODO: por ahora este método no lo uso mas porque queda
    //lo mismo implementado en processKey()
    //readyForTermination();

    // create game components
    // ...

    // listen for key presses
    addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e)
      {processKey(e);}
    });   
    
    
    // listen for mouse presses
    addMouseListener(new MouseAdapter() {
	  public void mousePressed(MouseEvent e) 
	  {testPress(e.getX( ), e.getY( )); }
    });
    
    //  set up message font
    font = new Font("SansSerif", Font.BOLD, 24);
    metrics = this.getFontMetrics(font);    
    
    ImagesLoader imsLoader = new ImagesLoader(IMS_INFO);
    //bgImage = ...
    clipsLoader = new ClipsLoader(SNDS_FILE);
    
    // create game sprites
    bat = new BatSprite(PWIDTH, PHEIGHT, imsLoader,(int)(period/1000000L));  		
    		
    // initialise timing elements
    if (Juego.isIndDebugMode()) {      
      fpsStore = new double[NUM_FPS];
      upsStore = new double[NUM_FPS];
      for (int i=0; i < NUM_FPS; i++) {
        fpsStore[i] = 0.0;
        upsStore[i] = 0.0;
      }
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
  
  private void processKey(KeyEvent e) {
    int keyCode = e.getKeyCode();
    
    // termination keys
    if ((keyCode == KeyEvent.VK_ESCAPE) ||
        (keyCode == KeyEvent.VK_Q) ||
        (keyCode == KeyEvent.VK_END) ||
        ((keyCode == KeyEvent.VK_C) && e.isControlDown( )) )
      running = false;
    
    // game-play keys
    if (!isPaused && !gameOver) {
      if (keyCode == KeyEvent.VK_LEFT)
    	  bat.moveLeft();
      else if (keyCode == KeyEvent.VK_RIGHT)
    	  bat.moveRight();
      else if (keyCode == KeyEvent.VK_DOWN)
    	  bat.stayStill();    
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
  
  public void resumeGame()
  // called when the JFrame is activated / deiconified
  {  isPaused = false;  }
  
  public void pauseGame()
  // called when the JFrame is deactivated / iconified
  { isPaused = true;   }
  
  public void stopGame()
  // called when the JFrame is closing
  {  running = false;   }
  

  //TODO: compleatar este metodo
  // is (x,y) important to the game?
  private void testPress(int x, int y) {
    if (!isPaused && !gameOver) {
      // do something
    }
  }
  
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
      
      if (Juego.isIndDebugMode())
        storeStats();
    }
    
    if (Juego.isIndDebugMode())
      printStats();
    
    System.exit(0);
  }
  
  private void gameUpdate() { 
    if (!isPaused && !gameOver) {
      // update game state ...
      bat.updateSprite();
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
    bat.drawSprite(dbg);
    
    //  report average FPS and UPS at top left
    if (Juego.isIndDebugMode())
      dbg.drawString("Average FPS/UPS: " + df.format(averageFPS) + ", " + df.format(averageUPS), 20, 25); 

    dbg.setColor(Color.black);
    
    if (gameOver)
      gameOverMessage(dbg);
  }
  
  private void gameOverMessage(Graphics g) { 
  // code to calculate x and y...
    g.drawString(gameOverString, 20, 20);
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
	
      //double timingError = (double)(realElapsedTime-statsInterval) / statsInterval*100.0;
	
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
  
  private void printStats() {
    System.out.println("Frame Count/Loss: " + frameCount + " / " + totalFramesSkipped);
    System.out.println("Average FPS: " + df.format(averageFPS));
    System.out.println("Average UPS: " + df.format(averageUPS));
    System.out.println("Time Spent: " + timeSpentInGame + " secs");    
  }
  
  
  /*
  public void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    if (dbImage != null)
      g.drawImage(dbImage, 0, 0, null);
  }
  */
  //private static final long serialVersionUID = 1L;
}
