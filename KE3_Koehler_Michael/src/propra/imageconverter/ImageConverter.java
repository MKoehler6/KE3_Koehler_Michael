package propra.imageconverter;

import java.util.Date;


/**
 * @author Michael Koehler
 * @throws ConverterException: zentrale Fehlerbehandlung in einer eigenen Klasse
 * Diese Klasse enthält die main-Methode, die die Kommandozeilenparameter 
 * an den ArgumentHandler weitergibt
 * mit startAction() der Klasse ArgumentHandler wird die entsprechende Klasse zur
 * Weiterverarbeitung aufgerufen
 *
 */
public class ImageConverter {
	
	public static void main(String[] args) throws ConverterException {
		
		try {
			startWithoutMain(args);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(123);
		}
		
	}
	
	public static void startWithoutMain(String[] args) throws ConverterException {
//		parst die Argumente und wertet sie aus
		ArgumentHandler argumentHandler = new ArgumentHandler(args); 
//		startet die auszufuehrende Aktion
		argumentHandler.startAction();
	}
	
}

