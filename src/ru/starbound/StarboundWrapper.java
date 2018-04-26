package ru.starbound;
import ru.starbound.server.Server;

public class StarboundWrapper {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		try {
			System.out.printf("Start server\n");
			Thread t = new Thread(new Server(21024));
			t.start();
			t.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.printf("Stop server\n");
	}

}
