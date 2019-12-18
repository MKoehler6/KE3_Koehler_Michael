package propra.huffman;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ZumAusprobieren {
	
//	int[] bits = {0,0,0,0,0,0,1,1, 0,0,0,0,0,0,0,1, 0,0,0,0,0,0,0,0, 1,0,0,1,1,0,1,1, 1,1,0,0,1,0,0,0, 
//			0,1,0,1,0,0,0,1, 1,1,0,0,0,1,0,0, 1,1,0,0,1,0,1,0, 1,0,0,0,0,1,1,0, 0,1,1,1,1,1,1,1, 
//			1,1,0,1,1,0,0,0, 0,0,0,0,1,1,0,0, 0,0,0,0,1,1,1,0, 0,0,0,0,1,0,1,1, 1,1,1,1,1,1,1,1};
	int[] bits = {0,0,1,1,0,0,0,0, 0,0,0,1,1,0,0,0, 0,0,0,1,0,1,1,0, 0,0,0,0,1,0,1,1, 0,0,0,0,0,1,1,0};
	int[] bilddaten = {0,1,0,0,1,0,0,0,1,0,1,1};
	int[] bytesOfImage = {23,56,45,38,23,45,23,78,89,67,14,26,14,13,56,23,23,55,67,69,45,45,23,14,16,18};
	int[] treeAndEncodedImageData = new int[41];
	HashMap<Integer,Double> byteValuesWithCounter = new HashMap<>();
	ArrayList<Node> nodeArrayForCreateTree = new ArrayList<>();
	ArrayList<Node> CopyNodeArrayForCreateTree = new ArrayList<>();
	HashMap<Integer,ArrayList<Integer>> codeBook = new HashMap<>();
	int counter = 0;
	int counterDecode = 0;
	int counterEncode = 0;
	ArrayList<Knoten> knotenArray = new ArrayList<>();
	

	public static void main(String[] args) {
//		System.out.println(Integer.toBinaryString(0));
//		System.out.println(Integer.valueOf(255).byteValue());
		ZumAusprobieren za = new ZumAusprobieren();
		za.haeufigkeitsverteilung();
//		za.testBaumErstellen();
	}

	private void haeufigkeitsverteilung() {
		for (int i = 0; i < bytesOfImage.length; i++) {
			int b = bytesOfImage[i];
			if (!byteValuesWithCounter.containsKey(b)) {
				byteValuesWithCounter.put(b, 1.0);
			} else {
				byteValuesWithCounter.put(b, byteValuesWithCounter.get(b) + 1);
			}
		}
		Set<Integer> set1 = byteValuesWithCounter.keySet();
//		relative Häufigkeit ausrechnen und speichern, Knoten erstellen und Wert und Häufigkeit speichern
		for (Integer integer : set1) {
			byteValuesWithCounter.put(integer, byteValuesWithCounter.get(integer)/bytesOfImage.length);
			System.out.println(integer + " " + byteValuesWithCounter.get(integer));
			Node node = new Node(integer);
			node.setRelativeFrequency(byteValuesWithCounter.get(integer));
			nodeArrayForCreateTree.add(node);
			CopyNodeArrayForCreateTree.add(node);
		}
		sortNodeArray();
		for (int i = 0; i < nodeArrayForCreateTree.size(); i++) {
			System.out.println(nodeArrayForCreateTree.get(i).getRelativeFrequency() + " " + nodeArrayForCreateTree.get(i).getValue());
		}
		createHuffmanTree();
		for (int i = 0; i < nodeArrayForCreateTree.size(); i++) {
			System.out.println("* " + nodeArrayForCreateTree.get(i).getRelativeFrequency());
		}
		ausgabeBaumStruktur(nodeArrayForCreateTree);
		writeCodeOfTreeInImage();
		for (int i = 0; i < treeAndEncodedImageData.length; i++) {
			System.out.print(treeAndEncodedImageData[i] + ",");
		}
		writeCodeBook();
		System.out.println();
		for (Integer integer : codeBook.keySet()) {
			System.out.println(integer + " " + codeBook.get(integer));
		}
	}
	
	private void writeCodeOfTreeInImage() {
		Node root = nodeArrayForCreateTree.get(0);
		root.setRoot(true); // root wird markiert, braucht man später zur Erstellung des CodeBooks
		writeCodeOfTreeInImageRecursion(root);
	}
	
	private void writeCodeOfTreeInImageRecursion(Node node) {
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
	
	private void writeNextBit(int bit) {
		treeAndEncodedImageData[counterEncode] = bit;
		counterEncode++;
	}
	
	private void writeValue(Node node) {
//		Kopieren des Arrays bitCode, um es mit dem Value in der HashMap zu speichern 
		ArrayList<Integer> bitCode = new ArrayList<>();
		codeBook.put(node.getValue(), bitCode);
		treeAndEncodedImageData[counterEncode] = node.getValue();
		counterEncode++;
	}
	
	private void writeCodeBook() {
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

	private void createHuffmanTree() {
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
			Node temp = nodeArrayForCreateTree.get(i);
			nodeArrayForCreateTree.set(i, nodeArrayForCreateTree.get(indexMin));
			nodeArrayForCreateTree.set(indexMin, temp);
		}
	}
	
	
//	********************************************************************************

	private void testBaumErstellen() {
		Knoten wurzel = new Knoten();
		knotenArray.add(wurzel);// speichern des Knoten zur späteren Kontrollausgabe
		baumErstellenRek(wurzel);
		ausgabe();
//		ausgabeBaumStruktur(knotenArray);
		decode(knotenArray.get(0));
	}


	private void baumErstellenRek(Knoten knoten) {
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

	private void ausgabe() {
		Integer value;
		for (Knoten k : knotenArray) {
			if (k.getValue() != null) value = k.getValue();
			else value = 1000;
			System.out.println("Neuer Knoten " + value);
		}
	}
	private void ausgabeBaumStruktur(ArrayList<Node> kArray) {
		Node aktuell = kArray.get(0);
		System.out.println("Wurzel 1000");
		ausgabeBaumStrukturRek(aktuell);
	}

	private void ausgabeBaumStrukturRek(Node k) {
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
	
	private void decode(Knoten wurzel) {
		Knoten aktuell = wurzel;
		decodeRek(aktuell);
	}

	private void decodeRek(Knoten k) {
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
