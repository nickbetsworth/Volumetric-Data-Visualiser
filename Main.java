import java.io.IOException;

/*
 * Name: Nicholas Betsworth
 * All of the code found within this file is my own work 
 * (besides some of the base code we were provided with by Mark Jones)
 */

public class Main {

	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		VolumeData vd;
		
		try {
			vd = new VolumeData("CThead");
		} catch (IOException e) {
			System.out.println("File CThead was not found");
			return;
		}
		
		new UserInterface(vd);
	}

}
