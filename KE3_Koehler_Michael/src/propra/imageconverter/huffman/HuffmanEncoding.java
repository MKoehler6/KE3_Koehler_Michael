package propra.imageconverter.huffman;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import propra.imageconverter.ConverterException;

/**
 * @author Michael Koehler
 * In dieser Klasse sind die Methoden zur Erstellung des Huffman-Baumes und des CodeBooks 
 * für die Huffman-Kodierung.
 * Außerdem ist hier die Methode zur Huffman-Kodierung einer Bildzeile.
 */
public class HuffmanEncoding {
	
	private HashMap<Integer,Double> byteValuesWithCounter = new HashMap<>();
//	ArrayList zum Aufbau des Huffman-Baumes, hier werden die Knoten nach und nach rausgelöscht
	private ArrayList<Node> nodeArrayForCreateTree = new ArrayList<>();
//	ArrayList zum Erstellen des CodeBooks
	private ArrayList<Node> copyNodeArrayForCreateTree = new ArrayList<>();
	private ArrayList<Integer> encodedHuffmanTreeArrayList = new ArrayList<>(); // die kodierten Bits des Baumes
	private HashMap<Integer,ArrayList<Integer>> codeBook = new HashMap<>(); // das CodeBook
	int counterBitsInOneByte;
	public ArrayList<Integer> bitArrayForOneByte = new ArrayList<>();
	int bitsRemainingInByte = 0; // übrig gebliebene Bits, wenn Byte noch nicht vollständig verarbeitet
	boolean writeHuffmanTreeFirst = true; // damit am Anfang des Datensegmentes der kodierte Baum geschrieben wird

	/**
	 * Das übergebene Input-File wird durchlaufen und die vorkommenden Bytes werden zusammen mit der relativen
	 * Häufigkeit in der HashMap byteValuesWithCounter gespeichert. Für jeden Bytewert wird ein Knoten Node
	 * erstellt und der Byte-Value und die relative Häufigkeit in ihm gespeichert. Diese Knoten werden in der ArrayList
	 * nodeArrayForCreateTree und copyNodeArrayForCreateTree gespeichert.
	 * Anschließend wird der Huffman-Baum erstellt und die kodierte Bitfolge in encodedHuffmanTreeArrayList gespeichert.
	 * Zum Schluss wird das CodeBook erstellt: jedes Byte-Value wird zusammen mit seinem Bitcode in der 
	 * HashMap codeBook gespeichert.
	 */
	public void createHuffmanTreeAndCodeBook(File inputFile, int imageWidth, int imageHeight, String type) throws ConverterException {
		try {
			FileInputStream fileInputStream = new FileInputStream(inputFile);
			BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
//			überspringen des Headers
			System.err.println(type);
			if (type.equals("propra")) bufferedInputStream.skip(28);
			if (type.equals("tga")) bufferedInputStream.skip(18);
//			bufferedInputStream.skip(18);
			long imageSizeInBytes = imageWidth * imageHeight * 3;
//			in der HashMap byteValuesWithCounter werden die Bytes mit der Häufigkeit gespeichert
			for (long i = 0; i < imageSizeInBytes; i++) {
				int singleByte = bufferedInputStream.read();
				if (!byteValuesWithCounter.containsKey(singleByte)) {
//					Wert speichern, wenn noch nicht vorhanden, mit Häufigkeit 1
					byteValuesWithCounter.put(singleByte, 1.0);
				} else {
//					wenn Wert schon vorhanden, Häufigkeit um eins erhöhen
					byteValuesWithCounter.put(singleByte, byteValuesWithCounter.get(singleByte) + 1);
				}
			}
//			Erstellt eine Liste aller Keys (die Byte-Werte) aus der HashMap byteValuesWithCounter
			Set<Integer> set1 = byteValuesWithCounter.keySet();
//			relative Häufigkeit ausrechnen und speichern, Knoten erstellen und Wert und relative Häufigkeit speichern
			for (Integer integer : set1) {
				byteValuesWithCounter.put(integer, byteValuesWithCounter.get(integer)/imageSizeInBytes);
				Node node = new Node(integer);
				node.setRelativeFrequency(byteValuesWithCounter.get(integer));
				nodeArrayForCreateTree.add(node);
				copyNodeArrayForCreateTree.add(node);
			}
//			einfuegen eines Platzhalters, wenn in den Bilddaten nur ein und dasselbe Byte vorkommt
			if (set1.size() == 1) {
				int platzhalter = 0;
				Node node = new Node(platzhalter);
				node.setRelativeFrequency(0.0);
				nodeArrayForCreateTree.add(node);
				copyNodeArrayForCreateTree.add(node);
			}
			
			sortNodeArray();
			createHuffmanTree();
			writeCodeOfTreeInArray();
			writeCodeBook();
			bufferedInputStream.close();
			fileInputStream.close();
		} catch (FileNotFoundException e) {
			throw new ConverterException("Datei kann nicht gefunden werden");
		} catch (IOException e) {
			throw new ConverterException("Beim Lesen der Datei ist ein Fehler aufgetreten");
		}
	}
	
	/**
	 * startet die Rekursion zum Durchlaufen des Huffman-Baumes
	 */
	private void writeCodeOfTreeInArray() {
		Node root = nodeArrayForCreateTree.get(0); // im Array nodeArrayForCreateTree ist nur noch dieser eine Knoten: 
													// die Wurzel des Baumes
		root.setRoot(true); // root wird markiert, braucht man später zur Erstellung des CodeBooks
		writeCodeOfTreeInArrayRecursion(root);
	}
	/**
	 * Rekursion zum Durchlaufen des Huffman-Baumes und zum Schreiben der Bitfolge
	 * des kodierten Huffman-Baumes in die ArrayList encodedHuffmanTreeArrayList
	 */
	private void writeCodeOfTreeInArrayRecursion(Node node) {
		if (node.left == null && node.right == null) { // Blatt ist erreicht
			writeNextBit(1);
			writeValue(node);
		} else {
			writeNextBit(0);
		}
//		linker Teilbaum
		if (node.left != null) {
			writeCodeOfTreeInArrayRecursion(node.left);
		}
//		rechter Teilbaum
		if (node.right != null) {
			writeCodeOfTreeInArrayRecursion(node.right);
		}
	}
	
	/**
	 * schreibt ein Bit in die ArrayList encodedHuffmanTreeArrayList
	 */
	private void writeNextBit(int bit) { 
		encodedHuffmanTreeArrayList.add(bit);
	}
	
	/**
	 * schreibt den Byte-Value des Blattes node als Bits in die ArrayList encodedHuffmanTreeArrayList
	 */
	private void writeValue(Node node) { 
		ArrayList<Integer> bitCode = new ArrayList<>();
		int bit;
		Integer value = node.getValue();
//		der Value wird in der HashMap codeBook zusammen mit einer leeren Arraylist bitCode gespeichert
		codeBook.put(value, bitCode); 
//		Umwandlung des Values in Bits und speichern in der ArrayList encodedHuffmanTreeArrayList
		for (int i = 7; i >= 0; i--) {
			bit = value/(int)(Math.pow(2, i));
			value = value - bit*(int)(Math.pow(2, i));
			encodedHuffmanTreeArrayList.add(bit);
		}
	}
	
	/**
	 * Das CodeBook wird erstellt
	 */
	private void writeCodeBook() {
		for (Node node : copyNodeArrayForCreateTree) { // Array mit den Blättern
			ArrayList<Integer> bitCode = new ArrayList<>();
			Node current = node;
//			es wird vom Blatt bis nach oben zur Wurzel gelaufen, wenn linkes Blatt wird eine 0, wenn
//			rechtes Blatt wird eine 1 vorne ins Array bitCode eingefügt
			while (!current.isRoot()) {
				bitCode.add(0, current.getPathToParent());
				current = current.parent;
			}
//			der Bitcode wird im CodeBook gespeichert
			codeBook.put(node.getValue(),bitCode);
		}
	}

	/**
	 * Der Baum wird von unten her aufgebaut, indem man die 2 Knoten mit der geringsten relativen Häufigkeit
	 * zu einem neuen Teilbaum zusammenfasst und in dessen Wurzel die Summe der relativen Häufigkeiten
	 * einträgt, die neue Wurzel wird im Array gespeichert und die beiden Blätter gelöscht,
	 * dies geschieht solange, bis im Array nur noch ein Knoten übrig ist, dann ist der Baum erstellt
	 */
	private void createHuffmanTree() {
		while (nodeArrayForCreateTree.size() > 1) {
//			nimmt die ersten beiden Knoten im sortierten Array (die mit den kleinsten Häufigkeiten)
			Node node1 = nodeArrayForCreateTree.get(0);
			Node node2 = nodeArrayForCreateTree.get(1);
			Node newNode = new Node();
			newNode.setRelativeFrequency(node1.getRelativeFrequency() + node2.getRelativeFrequency());
			newNode.left = node1;
			newNode.right = node2;
			node1.setPathToParent(0); // Hier wird gespeichert, ob der Knoten links oder rechts am
			node2.setPathToParent(1); // Elternknoten hängt, dies dient später der Erstellung des CodeBooks.
			node1.parent = newNode;	// der Elternknoten wird gespeichert
			node2.parent = newNode; // der Elternknoten wird gespeichert
			nodeArrayForCreateTree.remove(node1);
			nodeArrayForCreateTree.remove(node2);
			nodeArrayForCreateTree.add(newNode);
			sortNodeArray();
		}
	}

	/**
	 * Sortieren des Arrays der Blattknoten nach den relativen Häufigkeiten, kleinste Werte zuerst
	 * Das Array wird von vorne durchlaufen und das Rest-Array wird nach kleineren Werten durchsucht, 
	 * wenn ein kleinerer Wert gefunden: Tausch mit dem aktuellen Wert
	 */
	private void sortNodeArray() {
		for (int i = 0; i < nodeArrayForCreateTree.size()-1; i++) {
			double min = nodeArrayForCreateTree.get(i).getRelativeFrequency();
			int indexMin = i;
			for (int j = i+1; j < nodeArrayForCreateTree.size(); j++) {
				if(nodeArrayForCreateTree.get(j).getRelativeFrequency() < min) {
					min = nodeArrayForCreateTree.get(j).getRelativeFrequency();
					indexMin = j;
				}
			}
//			Tausch des gefundenen kleineren Wertes mit dem aktuellen Wert
			Node temp = nodeArrayForCreateTree.get(i);
			nodeArrayForCreateTree.set(i, nodeArrayForCreateTree.get(indexMin));
			nodeArrayForCreateTree.set(indexMin, temp);
		}
	}
	
	/**
	 * diese Methode übernimmt die Huffman-Kodierung einer Zeile
	 */
	public byte[] writeEncodedPixelInOutputLine(byte[] inputLine) {
		ArrayList<Integer> outputLineArrayList = new ArrayList<>();
//		an den Anfang des Datensegments wird erst der Huffmann-Tree geschrieben
		if (writeHuffmanTreeFirst) { 
			writeHuffmanTreeFirst = false;
			for (int i = 0; i < encodedHuffmanTreeArrayList.size(); i++) {
				bitArrayForOneByte.add(encodedHuffmanTreeArrayList.get(i));
				if (bitArrayForOneByte.size() == 8) {
					outputLineArrayList.add(toByteValue(bitArrayForOneByte));
					bitArrayForOneByte.clear();
				}
			}
		}
//		übrig gebliebene Bits in bitArrayForOneByte aus dem letzten Aufruf der Methode  
//		bzw. vom Schreiben des Huffman-Baumes sind noch vorhanden und werden jetzt berücksichtigt
		for (int i = 0; i < inputLine.length; i++) {
//			aus dem CodeBook wird der BitCode für den Byte-Wert herausgesucht
			ArrayList<Integer> bitCodeForValue = codeBook.get(Byte.toUnsignedInt(inputLine[i]));
//			der Bitcode wird byteweise in die outputLineArrayList geschrieben
//			übrig gebliebene Bits in bitArrayForOneByte bleiben dort bis zum nächsten Durchlauf
			for (int j = 0; j < bitCodeForValue.size(); j++) {
				bitArrayForOneByte.add(bitCodeForValue.get(j));
				if (bitArrayForOneByte.size() == 8) {
					outputLineArrayList.add(toByteValue(bitArrayForOneByte));
					bitArrayForOneByte.clear();
				}
			}
		}
		byte[] outputLine = new byte[outputLineArrayList.size()];
//		die Integer-ArrayList wird in eine byte-ArrayList übertragen
		for (int i = 0; i < outputLine.length; i++) {
			outputLine[i] = outputLineArrayList.get(i).byteValue();
		}
		return outputLine;
	}
	
	/**
	 * Umwandlung einer ArrayList mit Bits in einen Byte-Value (als int)
	 */
	public int toByteValue(ArrayList<Integer> bitArrayForOneByte) {
		int byteValue = 0;
		for (int i = 0; i < 8; i++) {
			byteValue = byteValue + bitArrayForOneByte.get(i) * (int)(Math.pow(2, 7-i));
		}
		return byteValue;
	}

}
