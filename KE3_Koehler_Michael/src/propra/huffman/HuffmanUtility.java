package propra.huffman;

import java.io.BufferedInputStream;
import java.util.ArrayList;

public class HuffmanUtility {
	
//	int[] bits = {0,0,0,0,0,0,1,1, 0,0,0,0,0,0,0,1, 0,0,0,0,0,0,0,0, 1,0,0,1,1,0,1,1, 1,1,0,0,1,0,0,0, 
//			0,1,0,1,0,0,0,1, 1,1,0,0,0,1,0,0, 1,1,0,0,1,0,1,0, 1,0,0,0,0,1,1,0, 0,1,1,1,1,1,1,1, 
//			1,1,0,1,1,0,0,0, 0,0,0,0,1,1,0,0, 0,0,0,0,1,1,1,0, 0,0,0,0,1,0,1,1, 1,1,1,1,1,1,1,1};
	static int[] bits = {0,0,1,1,0,0,0,0, 0,0,0,1,1,0,0,0, 0,0,0,1,0,1,1,0, 0,0,0,0,1,0,1,1, 0,0,0,0,0,1,1,0};
	static int[] bilddaten = {0,1,0,0,1,0,0,0,1,0,1,1};
	static int counter = 0;
	static int counterDecode = 0;
	static ArrayList<Knoten> knotenArray = new ArrayList<>();
	static ArrayList<Node> nodeArray = new ArrayList<>();

	public static void testBaumErstellen() {
		Knoten wurzel = new Knoten();
		knotenArray.add(wurzel);// speichern des Knoten zur späteren Kontrollausgabe
		baumErstellenRek(wurzel);
		ausgabe();
		ausgabeBaumStruktur();
		decode(knotenArray.get(0));
	}

	public static void readHuffmanTree(BufferedInputStream bufferedInputStream) {
		Node root = new Node();
		nodeArray.add(root);// speichern des Knoten zur späteren Kontrollausgabe
		readHuffmanTreeRecursion(root, bufferedInputStream);
		ausgabeBaumStruktur();
	}
	
	public static void readHuffmanTreeRecursion(Node node, BufferedInputStream bufferedInputStream) {
//		linker Zweig
		counter++;
		int bit = bits[counter];
					
		if (bit == 0) {
			Node newNode = new Node();
			node.left = newNode; // neuer innerer Knoten wird links angehängt
			nodeArray.add(newNode); // speichern des Knoten zur späteren Kontrollausgabe
			readHuffmanTreeRecursion(newNode, bufferedInputStream);
		} else {
			int value = 0;
			counter++;
			for (double i = 0; i < 8; i++) {
				value = value + bits[counter + (int) i] * (int)(Math.pow(2.0, 7-i));
			}
			counter += 8;
			Node newNode = new Node(value);
			node.left = newNode; // Blatt mit Wert wird links angehängt
			nodeArray.add(newNode); // speichern des Knoten zur späteren Kontrollausgabe
		}
		
//		rechter Zweig
		bit = bits[counter];
		if (bit == 0) {
			Node newNode = new Node();
			node.right = newNode; // neuer innerer Knoten wird rechts angehängt
			nodeArray.add(newNode); // speichern des Knoten zur späteren Kontrollausgabe
			readHuffmanTreeRecursion(newNode, bufferedInputStream);
		} else {
			int value = 0;
			counter++;
			for (double i = 0; i < 8; i++) {
				value = value + bits[counter + (int) i] * (int)(Math.pow(2.0, 7-i));
			}
			counter += 8;
			Node newNode = new Node(value);
			node.right = newNode; // Blatt mit Wert wird rechts angehängt
			nodeArray.add(newNode); // speichern des Knoten zur späteren Kontrollausgabe
		}
	}
	
	public static byte[] decodeHuffman (BufferedInputStream bufferedInputStream, int imageWidth) {
		byte[] line = new byte[imageWidth];
		Node root = nodeArray.get(0);
		decodeHuffmanRecursion(bufferedInputStream, root);
		return line;
	}

	public static void decodeHuffmanRecursion(BufferedInputStream bufferedInputStream, Node node) {
		while (counterDecode <= bilddaten.length) {
			if (node.left == null && node.right == null) {
				System.out.println(node.getValue());
				node = nodeArray.get(0);
				continue;
			}
			if (counterDecode == bilddaten.length) break;
			if (bilddaten[counterDecode] == 0) {
				node = node.left;
				counterDecode++;
				continue;
			}
			if (bilddaten[counterDecode] == 1) {
				node = node.right;
				counterDecode++;
			}
		}
		
	}
	
//	************************************************************************************

	public static void baumErstellenRek(Knoten knoten) {
//		linker Zweig
		counter++;
		int bit = bits[counter];
					
		if (bit == 0) {
			Knoten neuerKnoten = new Knoten();
			knoten.left = neuerKnoten; // neuer innerer Knoten wird links angehängt
			knotenArray.add(neuerKnoten); // speichern des Knoten zur späteren Kontrollausgabe
			baumErstellenRek(neuerKnoten);
		} else {
			int value = 0;
			counter++;
			for (double i = 0; i < 8; i++) {
				value = value + bits[counter + (int) i] * (int)(Math.pow(2.0, 7-i));
			}
			counter += 8;
			Knoten neuerKnoten = new Knoten(value);
			knoten.left = neuerKnoten; // Blatt mit Wert wird links angehängt
			knotenArray.add(neuerKnoten); // speichern des Knoten zur späteren Kontrollausgabe
		}
		
//		rechter Zweig
		bit = bits[counter];
		if (bit == 0) {
			Knoten neuerKnoten = new Knoten();
			knoten.right = neuerKnoten; // neuer innerer Knoten wird rechts angehängt
			knotenArray.add(neuerKnoten); // speichern des Knoten zur späteren Kontrollausgabe
			baumErstellenRek(neuerKnoten);
		} else {
			int value = 0;
			counter++;
			for (double i = 0; i < 8; i++) {
				value = value + bits[counter + (int) i] * (int)(Math.pow(2.0, 7-i));
			}
			counter += 8;
			Knoten neuerKnoten = new Knoten(value);
			knoten.right = neuerKnoten; // Blatt mit Wert wird rechts angehängt
			knotenArray.add(neuerKnoten); // speichern des Knoten zur späteren Kontrollausgabe
		}
	}

	public static void ausgabe() {
		Integer value;
		for (Knoten k : knotenArray) {
			if (k.getValue() != null) value = k.getValue();
			else value = 1000;
			System.out.println("Neuer Knoten " + value);
		}
	}
	public static void ausgabeBaumStruktur() {
		Knoten aktuell = knotenArray.get(0);
		System.out.println("Wurzel 1000");
		ausgabeBaumStrukturRek(aktuell);
	}

	public static void ausgabeBaumStrukturRek(Knoten k) {
		Integer value;
		if (k.left == null && k.right == null) return;
		if (k.left != null) {
			if (k.left.getValue() != null) value = k.left.getValue();
			else value = 1000;
			System.out.println("links " + value);
			ausgabeBaumStrukturRek(k.left);
		}
		if (k.right != null) {
			if (k.right.getValue() != null) value = k.right.getValue();
			else value = 1000;
			System.out.println("rechts " + value);
			ausgabeBaumStrukturRek(k.right);
		}
	}
	
	public static void decode(Knoten wurzel) {
		Knoten aktuell = wurzel;
		decodeRek(aktuell);
	}

	public static void decodeRek(Knoten k) {
		while (counterDecode <= bilddaten.length) {
			if (k.left == null && k.right == null) {
				System.out.println(k.getValue());
				k = knotenArray.get(0);
				continue;
			}
			if (counterDecode == bilddaten.length) break;
			if (bilddaten[counterDecode] == 0) {
				k = k.left;
				counterDecode++;
				continue;
			}
			if (bilddaten[counterDecode] == 1) {
				k = k.right;
				counterDecode++;
			}
		}
		
	}

}
