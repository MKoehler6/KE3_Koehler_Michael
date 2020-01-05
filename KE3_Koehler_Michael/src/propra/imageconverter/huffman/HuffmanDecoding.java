package propra.imageconverter.huffman;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import propra.imageconverter.ConverterException;

/**
 * @author Michael Koehler
 * In dieser Klasse sind Methoden zur Dekodierung von Huffman-Dateien
 *
 */
public class HuffmanDecoding {
//	der public int counterAllBitsOfTree wird von ConverterPropraToTga und CopyCompressDecompressPropraFile 
//	abgefragt, um die Länge des kodierten Huffman-Baumes am Anfang des Datensegments zu bestimmen
	public int counterAllBitsOfTree = 0; 
	private int counterBitsInOneByte; // zählt bis 8 und zeigt damit an, dass ein Byte voll ist
	private int bitsRemainingInByte = 0; // übrig gebliebene Bits, wenn Byte noch nicht vollständig verarbeitet
	private int nextByte; // das nächste Byte, dass aus der Datei eingelesen wird
	private int[] bitArray; // Array für die 8 Bits eines Bytes
	private byte[] line; // Array für die dekodierten Pixel einer Bildlinie
	private ArrayList<Node> nodeArray = new ArrayList<>(); // zum Speichern der Knoten, nur der Index 0 mit 
//	dem Wurzelknoten wird später gebraucht, die anderen Knoten werden für eine evtl. Kontrollausgabe gespeichert

	/**
	 * beginnt das Einlesen des kodierten Baumes und ruft dann die Rekursion zum weiteren Einlesen auf
	 */
	public void readHuffmanTree(File inputFile) throws ConverterException {
		try {
			FileInputStream fileInputStream = new FileInputStream(inputFile);
			BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
//			Header überspringen
			bufferedInputStream.skip(28);
			readNextBit(bufferedInputStream); // lies erstes Bit: das ist die Wurzel, ist immer 0
			Node root = new Node();
			nodeArray.add(root);// speichern der Wurzel, wird dann zum Dekodieren einer Bildzeile gebraucht
			readHuffmanTreeRecursion(root, bufferedInputStream); // Aufrufen der Rekursion
			bufferedInputStream.close();
			fileInputStream.close();
		} catch (FileNotFoundException e) {
			throw new ConverterException("Datei kann nicht gefunden werden");
		} catch (IOException e) {
			throw new ConverterException("Beim Lesen der Datei ist ein Fehler aufgetreten");
		}
		bitsRemainingInByte = 8-counterBitsInOneByte; // Anzahl verbleibende Bits im letzten Byte des Baumes, diese gehören
//		dann schon zu den kodierten Bilddaten
	}
	
	/**
	 * Rekursion zum Aufbau des Huffman-Baumes
	 */
	public void readHuffmanTreeRecursion(Node node, BufferedInputStream bufferedInputStream) throws ConverterException {
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
			Node newNode = new Node(value); // neues Blatt mit einem Value
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
			Node newNode = new Node(value); // neues Blatt mit einem Value
			node.right = newNode; // Blatt mit Wert wird rechts angehängt
			nodeArray.add(newNode); // speichern des Knoten zur späteren Kontrollausgabe
		}
	}
	
	/**
	 * Lesen des nächsten Bits des kodierten Huffman-Baumes
	 */
	private int readNextBit(BufferedInputStream bufferedInputStream) throws ConverterException {
		int bit;
		if (counterBitsInOneByte == 0) { // ein neues Byte muss eingelesen werden
			try {
				nextByte = bufferedInputStream.read();
				String bitString = Integer.toBinaryString(nextByte); // umwandeln des Bytes in BinaryString
				bitArray = new int [8];
				for (int i = 0; i < bitString.length(); i++) { // Auslesen der Nullen und Einsen aus dem BinaryString
					bitArray[7-i] = Integer.parseInt(bitString.substring(bitString.length()-1-i, bitString.length()-i));
				}
			} catch (IOException e) {
				throw new ConverterException("Beim Lesen der Datei ist ein Fehler aufgetreten");
			}
		}
		bit = bitArray[counterBitsInOneByte];
		counterBitsInOneByte++;
		if (counterBitsInOneByte == 8) {
			counterBitsInOneByte = 0; // das letzte Byte wurde vollständig abgearbeitet, das nächste Byte muss 
//			eingelesen werden
		}
		counterAllBitsOfTree++;
		return bit;
	}
	
	/**
	 * wird von ConverterPropraToTga und CopyCompressDecompressPropraFile aufgerufen zum Dekodieren einer 
	 * Bildzeile, die Bildbreite wird übergeben um zu wissen, wieviele Bytes die dekodierte Bildzeile haben muss
	 */
	public byte[] decodeHuffman (BufferedInputStream bufferedInputStream, int imageWidth) throws ConverterException {
			line = new byte[imageWidth*3];
			Node root = nodeArray.get(0); // der Wurzelknoten des Huffman-Baumes
			decodeHuffmanLine(bufferedInputStream, root, imageWidth);
			bitsRemainingInByte = 8-counterBitsInOneByte;
		return line;
	}

	/**
	 * Dekodieren einer Huffman-kodierten Bildzeile 
	 */
	private void decodeHuffmanLine(BufferedInputStream bufferedInputStream, Node node, int imageWidth) throws ConverterException {
		int pixelOfLineDecoded = 0; // Zähler für die dekodierten Bytes
		int bit;

		while (pixelOfLineDecoded < imageWidth*3) {
			if (node.left == null && node.right == null) { // ein Blatt wurde erreicht
				line[pixelOfLineDecoded] = node.getValue().byteValue(); // der Value wird ausgelesen
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
	/**
	 * Lesen des nächsten Bits
	 */
	private int readNextBitForDecoding(BufferedInputStream bufferedInputStream) throws ConverterException {
		
		int bit;
//		übrig gebliebene Bits aus dem letzten Byte nach dem Huffmann-Baum oder der letzten Zeile werden verarbeitet
		if (bitsRemainingInByte > 0 && bitsRemainingInByte < 8) {
			bit = bitArray[counterBitsInOneByte];
			counterBitsInOneByte++;
			bitsRemainingInByte--; // wenn 0, dann ist das Einlesen übrig gebliebener Bits beendet
			if (counterBitsInOneByte == 8) {
				counterBitsInOneByte = 0;
				
			}
			return bit;
		}
//		ab hier beginnt das Einlesen neuer Bytes aus der Datei
		if (counterBitsInOneByte == 0) { // ein neues Byte muss eingelesen werden
			try {
				nextByte = bufferedInputStream.read();
				String bitString = Integer.toBinaryString(nextByte); // umwandeln des Bytes in BinaryString
				bitArray = new int [8];
				if (nextByte != -1) { // Dateiende noch nicht erreicht
					for (int i = 0; i < bitString.length(); i++) { // Auslesen der Nullen und Einsen aus dem BinaryString
						bitArray[7-i] = Integer.parseInt(bitString.substring(bitString.length()-1-i, bitString.length()-i));
					}
				}
			} catch (IOException e) {
				throw new ConverterException("Beim Lesen der Datei ist ein Fehler aufgetreten");
			}
		}
		bit = bitArray[counterBitsInOneByte]; // das nächste Bit
		counterBitsInOneByte++;
		if (counterBitsInOneByte == 8) {
			counterBitsInOneByte = 0; // das letzte Byte wurde vollständig abgearbeitet, das nächste Byte muss 
//			eingelesen werden
		}
		return bit;
	}
}
