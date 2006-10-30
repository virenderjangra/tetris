package Logica;

public class Posicion {
	private int x;
	private int y;
	
	public int getEjeX(){
		return x;
	}

	public int getEjeY(){
		return y;
	}
	
	public Posicion(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	public void moverEjeX(int x){
		this.x = x;
	}

	public void moverEjeY(int y){
		this.y = y;
	}

	public void mover(int x, int y){
		this.x = x;
		this.y = y;
	}

}
