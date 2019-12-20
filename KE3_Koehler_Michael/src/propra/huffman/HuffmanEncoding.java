package propra.huffman;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import propra.imageconverter.ConverterException;

public class HuffmanEncoding {
	
	private static HashMap<Integer,Double> byteValuesWithCounter = new HashMap<>();
//	ArrayList zum Aufbau des Huffman-Baumes, hier werden die Knoten nach und nach rausgelöscht
	private static ArrayList<Node> nodeArrayForCreateTree = new ArrayList<>();
//	ArrayList zum Erstellen des CodeBooks
	private static ArrayList<Node> CopyNodeArrayForCreateTree = new ArrayList<>();
	private static ArrayList<Integer> encodedHuffmanTreeArrayList = new ArrayList<>();
	private static HashMap<Integer,ArrayList<Integer>> codeBook = new HashMap<>();
	static int counterBitsInOneByte;
	static int bitsRemainingInByte = 0; // übrig gebliebene Bits, wenn Byte noch nicht vollständig verarbeitet
	static boolean writeHuffmanTreeFirst = true;

	public static byte[] createHuffmanTreeAndCodeBook(File inputFile, int imageWidth, int imageHeight) throws ConverterException {
		try {
			FileInputStream fileInputStream = new FileInputStream(inputFile);
			BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
			bufferedInputStream.skip(18);
			long imageSizeInBytes = imageWidth * imageHeight * 3;
			for (long i = 0; i < imageSizeInBytes; i++) {
				int singleByte = bufferedInputStream.read();
				if (!byteValuesWithCounter.containsKey(singleByte)) {
					byteValuesWithCounter.put(singleByte, 1.0);
				} else {
					byteValuesWithCounter.put(singleByte, byteValuesWithCounter.get(singleByte) + 1);
				}
			}
			Set<Integer> set1 = byteValuesWithCounter.keySet();
	//		relative Häufigkeit ausrechnen und speichern, Knoten erstellen und Wert und Häufigkeit speichern
			for (Integer integer : set1) {
				byteValuesWithCounter.put(integer, byteValuesWithCounter.get(integer)/imageSizeInBytes);
				System.out.println(integer + " " + byteValuesWithCounter.get(integer));
				Node node = new Node(integer);
				node.setRelativeFrequency(byteValuesWithCounter.get(integer));
				nodeArrayForCreateTree.add(node);
				CopyNodeArrayForCreateTree.add(node);
			}
			sortNodeArray();
			createHuffmanTree();
			for (int i = 0; i < nodeArrayForCreateTree.size(); i++) {
				System.out.println("* " + nodeArrayForCreateTree.get(i).getRelativeFrequency());
			}
	//		ausgabeBaumStruktur(nodeArrayForCreateTree);
			writeCodeOfTreeInImage();
			for (int i = 0; i < encodedHuffmanTreeArrayList.size(); i++) {
				System.out.print(encodedHuffmanTreeArrayList.get(i) + ",");
			}
			System.out.println();
			writeCodeBook();
			for (Integer integer : codeBook.keySet()) {
				System.out.println(integer + " " + codeBook.get(integer));
				bufferedInputStream.close();
				fileInputStream.close();
			}
		} catch (FileNotFoundException e) {
			throw new ConverterException("Datei kann nicht gefunden werden");
		} catch (IOException e) {
			throw new ConverterException("Beim Lesen der Datei ist ein Fehler aufgetreten");
		}
		byte[] encodedHuffmanTree = new byte[encodedHuffmanTreeArrayList.size()];
		for (int i = 0; i < encodedHuffmanTree.length; i++) {
			encodedHuffmanTree[i] = encodedHuffmanTreeArrayList.get(i).byteValue();
		}
		return encodedHuffmanTree;
	}
	
	private static void writeCodeOfTreeInImage() {
		Node root = nodeArrayForCreateTree.get(0);
		root.setRoot(true); // root wird markiert, braucht man später zur Erstellung des CodeBooks
		writeCodeOfTreeInImageRecursion(root);
	}
	
	private static void writeCodeOfTreeInImageRecursion(Node node) {
		if (node.left == null && node.right == null) {
			writeNextBit(1);
			writeValue(node);
		} else {
			writeNextBit(0);
		}
//		linker Teilbaum
		if (node.left != null) {
			writeCodeOfTreeInImageRecursion(node.left);
		}
//		rechter Teilbaum
		if (node.right != null) {
			writeCodeOfTreeInImageRecursion(node.right);
		}
	}
	
	private static void writeNextBit(int bit) {
		
		encodedHuffmanTreeArrayList.add(bit);
	}
	
	private static void writeValue(Node node) {
//		Kopieren des Arrays bitCode, um es mit dem Value in der HashMap zu speichern 
		ArrayList<Integer> bitCode = new ArrayList<>();
		int bit;
		Integer value = node.getValue();
		codeBook.put(value, bitCode);
		for (int i = 7; i >= 0; i--) {
			bit = value/(int)(Math.pow(2, i));
			value = value - bit*(int)(Math.pow(2, i));
			encodedHuffmanTreeArrayList.add(bit);
			System.out.print(bit + ";");
		}
		System.out.println();
	}
	
	private static void writeCodeBook() {
		for (Node node : CopyNodeArrayForCreateTree) {
			ArrayList<Integer> bitCode = new ArrayList<>();
			Node current = node;
			while (!current.isRoot()) {
				bitCode.add(0, current.getPathToParent());
				current = current.parent;
			}
			codeBook.put(node.getValue(),bitCode);
		}
	}

	private static void createHuffmanTree() {
		while (nodeArrayForCreateTree.size() > 1) {
			Node node1 = nodeArrayForCreateTree.get(0);
			Node node2 = nodeArrayForCreateTree.get(1);
			Node newNode = new Node();
			newNode.setRelativeFrequency(node1.getRelativeFrequency() + node2.getRelativeFrequency());
			newNode.left = node1;
			newNode.right = node2;
			node1.setPathToParent(0); // hier wird gespeichert, ob der Knoten links oder rechts am
			node2.setPathToParent(1); // Elternknoten hängt, dient später der Erstellung des CodeBooks
			node1.parent = newNode;	// der Elternknoten wird gespeichert
			node2.parent = newNode;
			nodeArrayForCreateTree.remove(node1);
			nodeArrayForCreateTree.remove(node2);
			nodeArrayForCreateTree.add(newNode);
			sortNodeArray();
		}
	}

	private static void sortNodeArray() {
		for (int i = 0; i < nodeArrayForCreateTree.size()-1; i++) {
			double min = nodeArrayForCreateTree.get(i).getRelativeFrequency();
			int indexMin = i;
			for (int j = i+1; j < nodeArrayForCreateTree.size(); j++) {
				if(nodeArrayForCreateTree.get(j).getRelativeFrequency() < min) {
					min = nodeArrayForCreateTree.get(j).getRelativeFrequency();
					indexMin = j;
				}
			}
			Node temp = nodeArrayForCreateTree.get(i);
			nodeArrayForCreateTree.set(i, nodeArrayForCreateTree.get(indexMin));
			nodeArrayForCreateTree.set(indexMin, temp);
		}
	}
	
	public static byte[] writeHuffmanEncodedPixel(byte[] inputLine) {
		if (writeHuffmanTreeFirst) {
			writeHuffmanTreeFirst = false;
			return writeHuffmanTree();
		}
		byte[] outputLine = new byte[3];
		
		return outputLine;
	}
	
	private static byte[] writeHuffmanTree() {
		int byteValue = 0;
		counterBitsInOneByte = 0;
		ArrayList<Integer> huffmanTree = new ArrayList<>();
		for (int i = 0; i < encodedHuffmanTreeArrayList.size(); i++) {
			byteValue = byteValue + encodedHuffmanTreeArrayList.get(i) * (int)(Math.pow(2, 7-counterBitsInOneByte));
			counterBitsInOneByte++;
			if (counterBitsInOneByte == 8) {
				counterBitsInOneByte = 0;
				huffmanTree.add(byteValue);
			}
		}
		bitsRemainingInByte = 8 - counterBitsInOneByte;
		byte[] huffmanTreeByteArray = new byte[huffmanTree.size()];
		for (int i = 0; i < huffmanTreeByteArray.length; i++) {
			huffmanTreeByteArray[i] = huffmanTree.get(i).byteValue();
		}
		return huffmanTreeByteArray;
	}
	

}
