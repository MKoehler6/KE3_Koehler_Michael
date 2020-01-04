package propra.imageconverter;

/**
 * @author Michael Köhler
 * zentrale Fehlerbehandlung des Programms, erzeugt Exception mit individueller Message
 * diese wird hochgereicht bis in die main-Methode, dort wird die Fehlermeldung dann ausgegeben
 * und das Programm mit Fehlercode 123 beendet
 */
public class ConverterException extends Exception {
	
	public ConverterException(String message) {
		super(message);
	}

}
