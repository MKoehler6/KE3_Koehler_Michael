package propra.imageconverter;

import java.util.ArrayList;

public class ZumAusprobieren {
	
	int[] bits = {0,0,0,0,0,0,1,1, 0,0,0,0,0,0,0,1, 0,0,0,0,0,0,0,0, 1,0,0,1,1,0,1,1, 1,1,0,0,1,0,0,0, 
			0,1,0,1,0,0,0,1, 1,1,0,0,0,1,0,0, 1,1,0,0,1,0,1,0, 1,0,0,0,0,1,1,0, 0,1,1,1,1,1,1,1, 
			1,1,0,1,0,1,0,0};
	int counter = 0;
	ArrayList<Knoten> knotenArray = new ArrayList<>();
	

	public static void main(String[] args) {
//		System.out.println(Integer.toBinaryString(0x73));
		ZumAusprobieren za = new ZumAusprobieren();
		za.testBaumErstellen();
	}

	private void testBaumErstellen() {
		Knoten wurzel = new Knoten();
		knotenArray.add(wurzel);
		baumErstellenRek(wurzel);
		ausgabe();
	}


	private void baumErstellenRek(Knoten knoten) {
		if (counter++ <70) {
			int bit = bits[counter];
			
			if (bit == 0 && knoten.left == null) {
				Knoten neuerKnoten = new Knoten();
				knoten.left = neuerKnoten;
				knotenArray.add(neuerKnoten);
				baumErstellenRek(neuerKnoten);
				return;
			}
			if (bit == 0 && knoten.left != null && knoten.right == null) {
				Knoten neuerKnoten = new Knoten();
				knoten.right = neuerKnoten;
				knotenArray.add(neuerKnoten);
				baumErstellenRek(neuerKnoten);
				return;
			}
			if (bit == 1 && knoten.left == null) {
				counter++;
				int value = 0;
				for (double i = 0; i < 8; i++) {
					value = value + bits[counter + (int) i] * (int)(Math.pow(2.0, 7-i));
				}
				Knoten neuerKnoten = new Knoten(value);
				knoten.left = neuerKnoten;
				knotenArray.add(neuerKnoten);
				baumErstellenRek(knoten);
				return;
			}
			if (bit == 1 && knoten.left != null && knoten.right == null) {
				counter++;
				int value = 0;
				for (double i = 0; i < 8; i++) {
					value = value + bits[counter + (int) i] * (int)(Math.pow(2.0, 7-i));
				}
				Knoten neuerKnoten = new Knoten(value);
				knoten.right = neuerKnoten;
				knotenArray.add(neuerKnoten);
				baumErstellenRek(knoten);
				return;
			}
			if (knoten.left != null && knoten.right != null) return;
		}
	}

	private void ausgabe() {
		Integer value;
		for (Knoten k : knotenArray) {
			if (k.getValue() != null) value = k.getValue();
			else value = 1000;
			System.out.println("Neuer Knoten " + value);
		}
	}
}
