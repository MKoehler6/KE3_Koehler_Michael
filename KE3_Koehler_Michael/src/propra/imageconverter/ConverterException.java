package propra.imageconverter;

/**
 * @author Michael Köhler
 * zentrale Fehlerbehandlung des Programms, gibt Fehlermeldung aus und beendet Programm mit 
 * Fehlercode 123
 */
public class ConverterException extends Exception {
	
	public ConverterException(String message) {
		super(message);
//		System.err.println(message);
//		System.exit(123);
	}

}
