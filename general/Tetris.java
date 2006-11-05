package general;

public class Tetris {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Ciclo ciclo = new Ciclo();
		
		ciclo.GamePanel();
		ciclo.addNotify();
		ciclo.run();
		
		
		
		
		ciclo.stopGame();
		

	}

}
