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
	public static int counterAllBitsOfTree = 0;
	static int counterBitsInOneByte;
	static int bitsRemainingInByte = 0; // übrig gebliebene Bits, wenn Byte noch nicht vollständig verarbeitet
	static int nextByte;
	static int[] bitArray;
	static byte[] line; // Array für die dekodierten Pixel einer Bildlinie
	static ArrayList<Node> nodeArray = new ArrayList<>();

	public static void readHuffmanTree(File inputFile) throws ConverterException {
		try {
			FileInputStream fileInputStream = new FileInputStream(inputFile);
			BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
			bufferedInputStream.skip(28);
			int bit = readNextBit(bufferedInputStream); // lies erstes Bit: das ist die Wurzel, ist immer 0
			Node root = new Node();
			nodeArray.add(root);// speichern des Knoten zur späteren Kontrollausgabe
			readHuffmanTreeRecursion(root, bufferedInputStream);
//			ausgabeBaumStruktur();
			bufferedInputStream.close();
			fileInputStream.close();
		} catch (FileNotFoundException e) {
			throw new ConverterException("Datei kann nicht gefunden werden");
		} catch (IOException e) {
			throw new ConverterException("Beim Lesen der Datei ist ein Fehler aufgetreten");
		}
		bitsRemainingInByte = 8-counterBitsInOneByte;
	}
	
	public static void readHuffmanTreeRecursion(Node node, BufferedInputStream bufferedInputStream) throws ConverterException {
//		linker Zweig
		int bit = readNextBit(bufferedInputStream);
					
		if (bit == 0) {
			Node newNode = new Node();
			node.left = newNode; // neuer innerer Knoten wird links angehängt
			nodeArray.add(newNode); // speichern des Knoten zur späteren Kontrollausgabe
			readHuffmanTreeRecursion(newNode, bufferedInputStream);
		} else {
			int value = 0;
			for (double i = 0; i < 8; i++) {
				value = value + readNextBit(bufferedInputStream) * (int)(Math.pow(2.0, 7-i));
			}
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
			for (double i = 0; i < 8; i++) {
				value = value + readNextBit(bufferedInputStream) * (int)(Math.pow(2.0, 7-i));
			}
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
		counterAllBitsOfTree++;
		return bit;
	}
	
	public static byte[] decodeHuffman (BufferedInputStream bufferedInputStream, int imageWidth) throws ConverterException {
			line = new byte[imageWidth*3];
			Node root = nodeArray.get(0);
			decodeHuffmanLine(bufferedInputStream, root, imageWidth);
			bitsRemainingInByte = 8-counterBitsInOneByte;
		return line;
	}

	public static void decodeHuffmanLine(BufferedInputStream bufferedInputStream, Node node, int imageWidth) throws ConverterException {
		int pixelOfLineDecoded = 0;
		int bit;

		while (pixelOfLineDecoded < imageWidth*3) {
			if (node.left == null && node.right == null) {
				line[pixelOfLineDecoded] = node.getValue().byteValue();
				pixelOfLineDecoded++;
				node = nodeArray.get(0); // es beginnt wieder an der Wurzel
				continue;
			}
			bit = readNextBitForDecoding(bufferedInputStream);
			if (bit == 0) {
				node = node.left;
				continue;
			}
			if (bit == 1) {
				node = node.right;
				continue;
			}
		}
		
	}
	private static int readNextBitForDecoding(BufferedInputStream bufferedInputStream) throws ConverterException {
		
		int bit;
//		übrig gebliebene Bits aus dem letzten Byte nach dem Huffmann-Baum oder der Zeile werden verarbeitet
		if (bitsRemainingInByte > 0 && bitsRemainingInByte < 8) {
			bit = bitArray[counterBitsInOneByte];
			counterBitsInOneByte++;
			bitsRemainingInByte--;
			if (counterBitsInOneByte == 8) {
				counterBitsInOneByte = 0;
				
			}
			return bit;
		}
//		ab hier beginnt das Einlesen neuer Bytes aus der Datei
		if (counterBitsInOneByte == 0) {
			try {
				nextByte = bufferedInputStream.read();
				String bitString = Integer.toBinaryString(nextByte);
				bitArray = new int [8];
				if (nextByte != -1) {
					for (int i = 0; i < bitString.length(); i++) {
						bitArray[7-i] = Integer.parseInt(bitString.substring(bitString.length()-1-i, bitString.length()-i));
					}
				}
			} catch (IOException e) {
				throw new ConverterException("Beim Lesen der Datei ist ein Fehler aufgetreten");
			}
		}
		bit = bitArray[counterBitsInOneByte];
		counterBitsInOneByte++;
		if (counterBitsInOneByte == 8) counterBitsInOneByte = 0;
		return bit;
	}
}
