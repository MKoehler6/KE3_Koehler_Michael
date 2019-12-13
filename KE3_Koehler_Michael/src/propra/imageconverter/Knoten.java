package propra.imageconverter;

public class Knoten {
	
	private Integer value;
	Knoten left;
	Knoten right;
	
	public Knoten(int value) {
		this.value = value;
	}
	
	public Knoten() {
		value = null;
	}
	
	public Integer getValue() {
		return value;
	}

}
