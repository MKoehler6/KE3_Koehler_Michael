package propra.huffman;

public class Node {
	
	private Integer value;
	Node left;
	Node right;
	
	public Node(int value) {
		this.value = value;
	}
	
	public Node() {
		value = null;
	}
	
	public Integer getValue() {
		return value;
	}

}
