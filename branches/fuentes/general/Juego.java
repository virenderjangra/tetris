package general;

import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JTextField;

public final class Juego extends JFrame {
	
    
	private static final long serialVersionUID = 1L;
    private Ciclo ciclo;
    private static int DEFAULT_FPS = 80;    
    private JTextField jtfBox;   // displays no.of boxes used
    private JTextField jtfTime;  // displays time spent in game

	
    public Juego(long l) {
    	this.ciclo = new Ciclo(this, l);
	}	

	public static void main(String args[])
    {
      int fps = DEFAULT_FPS;
      if (args.length != 0)
        fps = Integer.parseInt(args[0]);

      long period = (long) 1000.0/fps;
      System.out.println("fps: " + fps + "; period: " +period+ " ms");

      new Juego(period*1000000L);    // ms --> nanosecs
    }

	public void setTimeSpent(long t)
    {  		
		//jtfTime.setText("Time Spent: " + t + " secs");
    }
	
	public void windowActivated(WindowEvent e)
    { ciclo.resumeGame(); }

    public void windowDeactivated(WindowEvent e)
    {  ciclo.pauseGame();  }

    public void windowDeiconified(WindowEvent e)
    {  ciclo.resumeGame();  }

    public void windowIconified(WindowEvent e)
    {  ciclo.pauseGame(); }

    public void windowClosing(WindowEvent e)
   {  ciclo.stopGame();  }

}
