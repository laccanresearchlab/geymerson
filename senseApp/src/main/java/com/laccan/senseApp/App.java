package com.laccan.senseApp;

public class App {
	public static void main (String[] args) {		
		@SuppressWarnings("unused")
		Sense sense = new Sense("serial@/dev/ttyUSB1:57600");
	}
}