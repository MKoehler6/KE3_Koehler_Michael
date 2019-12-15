package propra.huffman;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import propra.imageconverter.ConverterException;

public class HuffmanUtility {
	
//	int[] bits = {0,0,0,0,0,0,1,1, 0,0,0,0,0,0,0,1, 0,0,0,0,0,0,0,0, 1,0,0,1,1,0,1,1, 1,1,0,0,1,0,0,0, 
//			0,1,0,1,0,0,0,1, 1,1,0,0,0,1,0,0, 1,1,0,0,1,0,1,0, 1,0,0,0,0,1,1,0, 0,1,1,1,1,1,1,1, 
//			1,1,0,1,1,0,0,0, 0,0,0,0,1,1,0,0, 0,0,0,0,1,1,1,0, 0,0,0,0,1,0,1,1, 1,1,1,1,1,1,1,1};
	static int[] bits = {0,0,1,1,0,0,0,0, 0,0,0,1,1,0,0,0, 0,0,0,1,0,1,1,0, 0,0,0,0,1,0,1,1, 0,0,0,0,0,1,1,0};
	static int[] bilddaten = {0,1,0,0,1,0,0,0,1,0,1,1};
	static int counter = 0;
	static int counterDecode = 0;
	static int counterAllBitsOfTree;
	static int counterBitsInOneByte;
	static int nextByte;
	static int[] bitArray;
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

	public static int readHuffmanTree(File inputFile) throws ConverterException {
		try {
			FileInputStream fileInputStream = new FileInputStream(inputFile);
			BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
			bufferedInputStream.skip(28);
			int bit = readNextBit(bufferedInputStream); // lies erstes Bit: das ist die Wurzel, ist immer 0
			Node root = new Node();
			nodeArray.add(root);// speichern des Knoten zur späteren Kontrollausgabe
			readHuffmanTreeRecursion(root, bufferedInputStream);
			ausgabeBaumStruktur();
		} catch (FileNotFoundException e) {
			throw new ConverterException("Datei kann nicht gefunden werden");
		} catch (IOException e) {
			throw new ConverterException("Beim Lesen der Datei ist ein Fehler aufgetreten");
		}
		return counterAllBitsOfTree;
	}
	
	public static void readHuffmanTreeRecursion(Node node, BufferedInputStream bufferedInputStream) throws ConverterException {
//		linker Zweig
		counter++;
		int bit = readNextBit(bufferedInputStream);
					
		if (bit == 0) {
			Node newNode = new Node();
			node.left = newNode; // neuer innerer Knoten wird links angehängt
			nodeArray.add(newNode); // speichern des Knoten zur späteren Kontrollausgabe
			readHuffmanTreeRecursion(newNode, bufferedInputStream);
		} else {
			int value = 0;
			counter++;
			for (double i = 0; i < 8; i++) {
				value = value + readNextBit(bufferedInputStream) * (int)(Math.pow(2.0, 7-i));
			}
			counter += 8;
			Node newNode = new Node(value);
			node.left = newNode; // Blatt mit Wert wird links angehängt
			nodeArray.add(newNode); // speichern des Knoten zur späteren Kontrollausgabe
		}
		
//		rechter Zweig
		bit = readNextBit(bufferedInputStream);
		if (bit == 0) {
			Node newNode = new Node();
			node.right = newNode; // neuer innerer Knoten wird rechts angehängt
			nodeArray.add(newNode); // speichern des Knoten zur späteren Kontrollausgabe
			readHuffmanTreeRecursion(newNode, bufferedInputStream);
		} else {
			int value = 0;
			counter++;
			for (double i = 0; i < 8; i++) {
				value = value + readNextBit(bufferedInputStream) * (int)(Math.pow(2.0, 7-i));
			}
			counter += 8;
			Node newNode = new Node(value);
			node.right = newNode; // Blatt mit Wert wird rechts angehängt
			nodeArray.add(newNode); // speichern des Knoten zur späteren Kontrollausgabe
		}
	}
	
	private static int readNextBit(BufferedInputStream bufferedInputStream) throws ConverterException {
		int bit;
		if (counterBitsInOneByte == 0) {
			try {
				nextByte = bufferedInputStream.read();
				System.out.println(nextByte);
				String bitString = Integer.toBinaryString(nextByte);
				bitArray = new int [8];
				for (int i = 0; i < bitString.length(); i++) {
					bitArray[7-i] = Integer.parseInt(bitString.substring(bitString.length()-1-i, bitString.length()-i));
				}
			} catch (IOException e) {
				throw new ConverterException("Beim Lesen der Datei ist ein Fehler aufgetreten");
			}
		}
		bit = bitArray[counterBitsInOneByte];
		counterBitsInOneByte++;
		if (counterBitsInOneByte == 8) counterBitsInOneByte = 0;
		for (int i = 0; i < bitArray.length; i++) {
			System.out.print(bitArray[i]);
		}
		System.out.println();
		System.out.println(bit);
		return bit;
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
		Node aktuell = nodeArray.get(0);
		System.out.println("Wurzel 1000");
		ausgabeBaumStrukturRek(aktuell);
	}

	public static void ausgabeBaumStrukturRek(Node k) {
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
