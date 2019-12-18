package propra.huffman;

public class Node {
	
	private Integer value;
	Node left;
	Node right;
	private Double relativeFrequency;
	
	public Node(int value) {
		this.value = value;
	}
	
	public Node() {
		value = null;
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
