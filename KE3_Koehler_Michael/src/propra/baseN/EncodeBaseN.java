package propra.baseN;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import propra.imageconverter.ConverterException;

/**
 * @author Michael Koehler
 * Eine Instanz dieser Klasse kodiert eine beliebige Input-Datei
 * und haengt die Endung base-n oder base-32 an
 * Grundlage der Dekodierung ist das Alphabet, das als Parameter uebergeben wurde, bzw. bei 
 * base-32 ist das Alphabet 0123456789ABCDEFGHIJKLMNOPQRSTUV
 * Moeglich sind folgende Alphabet-Längen: 2,4,8,16,32,64
 */
public class EncodeBaseN {
	
	private File inputFile; 
	private File outputFile;
	private String alphabet;
	private int bitsProZeichen;
	private boolean baseN;
	private HashMap<Integer, String> map; // zum Speichern des Alphabets

	public EncodeBaseN(String inputPath, String outputPath, int typeOfEncoding, String alphabet, boolean baseN) throws ConverterException {
		inputFile = new File(inputPath);
		outputFile = new File(outputPath);
		this.alphabet = alphabet;
		this.baseN = baseN;
		switch (typeOfEncoding) {
			case 2: bitsProZeichen = 1;
				break;
			case 4: bitsProZeichen = 2;
				break;
			case 8: bitsProZeichen = 3;
				break;
			case 16: bitsProZeichen = 4;
				break;
			case 32: bitsProZeichen = 5;
				break;
			case 64: bitsProZeichen = 6;
				break;
			default:
				throw new ConverterException("Ungültiger Kodierungs-Typ");
		}
//		Speichern des Alphabets in der Map
		map = new HashMap<Integer, String>();
		for (int i = 0; i < alphabet.length(); i++) {
			map.put(i, alphabet.charAt(i) + "");
		}
		encode();
	}
	
	/**
	 * kodiert die Input-Datei
	 */
	public void encode() throws ConverterException {
		try {
			FileInputStream fileInputStream = new FileInputStream(inputFile);
			BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
			FileWriter fileWriter = new FileWriter(outputFile);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			
//			schreibe Kodierungsalphabet in Output-Datei
			if (baseN) bufferedWriter.write(alphabet + "\n");
			
			int zeichenInput;
			int zeichenOutput = 0;
			int zaehlerBitsZeichenOutput = 0;
//			ein Byte wird gelesen
			while ((zeichenInput = bufferedInputStream.read()) != -1) {
//				Bit für Bit wird gelesen und in den Ausgabe-Integer geschrieben
				for (int i = 0; i < 8; i++) {
					int faktor = zeichenInput >>> 7-i;
					faktor = faktor&0x1;
					zeichenOutput += faktor * (int)Math.pow(2, bitsProZeichen-1-zaehlerBitsZeichenOutput);
					zaehlerBitsZeichenOutput ++;
					if (zaehlerBitsZeichenOutput == bitsProZeichen) {
//						das zum Integer gehoerige Zeichen wird ermittelt
						bufferedWriter.write(map.get(zeichenOutput));
						zaehlerBitsZeichenOutput = 0;
						zeichenOutput = 0;
					}
				}
			}
//			wenn das letzte Zeichen nicht mit Bits voll geworden ist, wird es jetzt noch geschrieben
			if (zaehlerBitsZeichenOutput != 0) {
				bufferedWriter.write(map.get(zeichenOutput));
			}
			
			bufferedWriter.flush();
			bufferedWriter.close(); 
			bufferedInputStream.close();
		} catch (FileNotFoundException e) {
			throw new ConverterException("Datei kann nicht gefunden werden");
		} catch (IOException e) {
			throw new ConverterException("Beim Lesen der Datei ist ein Fehler aufgetreten");
		}
	}

}
