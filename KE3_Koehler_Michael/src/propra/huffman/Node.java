package propra.huffman;

public class Node {
	
	private Integer value;
	Node left;
	Node right;
	Node parent;
	private Double relativeFrequency;
	private int pathToParent;
	private boolean root = false;
	
	public Node(int value) {
		this.value = value;
	}
	
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
