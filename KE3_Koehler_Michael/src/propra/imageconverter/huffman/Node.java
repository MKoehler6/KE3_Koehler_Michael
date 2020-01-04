package propra.imageconverter.huffman;

/**
 * @author Michael Koehler
 * Klasse für Knoten des Huffman-Baumes 
 *
 */
public class Node {
	
	private Integer value; // wenn der Knoten ein Blatt ist, wird hier der Byte-Wert gespeichert
	Node left; // Kindknoten
	Node right; // Kindknoten
	Node parent; // Elternknoten
	private Double relativeFrequency; // relative Häufigkeit des Byte-Wertes in den Imagedaten
	private int pathToParent; // wenn dieser Knoten links am Elternknoten hängt: 0, ansonsten 1
	private boolean root = false; // der Wurzelknoten wird mit true gekennzeichnet
	
	/**
	 * Konstruktor für ein Blatt
	 */
	public Node(int value) {
		this.value = value;
	}
	
	/**
	 * Konstruktor für einen inneren Knoten
	 */
	public Node() {
		value = null;
	}
	
	public int getPathToParent() {
		return pathToParent;
	}

	public void setPathToParent(int pathToParent) {
		this.pathToParent = pathToParent;
	}

	public boolean isRoot() {
		return root;
	}

	public void setRoot(boolean root) {
		this.root = root;
	}

	public Integer getValue() {
		return value;
	}

	public Double getRelativeFrequency() {
		return relativeFrequency;
	}

	public void setRelativeFrequency(Double relativeFrequency) {
		this.relativeFrequency = relativeFrequency;
	}
	
}
