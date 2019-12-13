package propra.imageconverter;

/**
 * @author Michael Koehler
 * Interface für die Datenformate, wenn ein Datenformat neu erzeugt wird, werden die 
 * Angaben im Header ueberprueft
 *
 */
public interface IFormat {
	
	/**
	 * liest den Header ein und speichert relevante Daten
	 */
	void readHeader() throws ConverterException;
	
	/**
	 * überprüft die Daten der Input-Datei, ob sie den Anforderungen entsprechen
	 */
	void checkDataOfHeader() throws ConverterException;
	
}
