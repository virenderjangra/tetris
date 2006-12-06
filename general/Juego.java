package general;

import java.awt.Container;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class Juego extends JFrame implements WindowListener {
	
	private static int DEFAULT_FPS = 80;
	
	//private static final long serialVersionUID = 1L;
    
	private Ciclo ciclo;        
    private JTextField jtfBox;   // displays no.of boxes used
    private JTextField jtfTime;  // displays time spent in game
	
    public Juego(long period) {
      super("Eze Test");      
      makeGUI(period);
      
      addWindowListener( this );
      pack();
      setResizable(false);
      setVisible(true);
	}
    
    private void makeGUI(long period)
    {
      Container c = getContentPane();    // default BorderLayout used

      this.ciclo = new Ciclo(this, period);
      c.add(this.ciclo, "Center");

      JPanel ctrls = new JPanel();   // a row of textfields
      ctrls.setLayout( new BoxLayout(ctrls, BoxLayout.X_AXIS));

      jtfBox = new JTextField("Boxes used: 0");
      jtfBox.setEditable(false);
      ctrls.add(jtfBox);

      jtfTime = new JTextField("Time Spent: 0 secs");
      jtfTime.setEditable(false);
      ctrls.add(jtfTime);

      c.add(ctrls, "South");
    }
    
    public void setBoxNumber(int no)
    {  jtfBox.setText("Boxes used: " + no);  }

    public void setTimeSpent(long t)
    {  jtfTime.setText("Time Spent: " + t + " secs"); }
    
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
    
    public void windowClosed(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}
    
    
	public static void main(String args[])
    {
      int fps = DEFAULT_FPS;
      if (args.length != 0)
        fps = Integer.parseInt(args[0]);

      long period = (long) 1000.0/fps;
      System.out.println("fps: " + fps + "; period: " +period+ " ms");

      new Juego(period*1000000L);    // ms --> nanosecs
    }

}
