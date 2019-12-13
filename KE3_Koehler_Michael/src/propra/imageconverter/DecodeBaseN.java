package propra.imageconverter;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author Michael Koehler
 * Eine Instanz dieser Klasse dekodiert eine beliebige Input-Datei mit der Endung base-n oder base-32, 
 * Grundlage der Dekodierung ist das Alphabet, was sich in der ersten Zeile der Datei befindet, bzw. bei 
 * base-32 ist das Alphabet 0123456789ABCDEFGHIJKLMNOPQRSTUV
 * Moeglich sind folgende Alphabet-Längen: 2,4,8,16,32,64
 */
public class DecodeBaseN {
	
	private File inputFile; 
	private File outputFile;
	private String alphabet;
	private int bitsProZeichen;
	private int typeOfDecoding;
	private HashMap<String, Integer> map; // zum Speichern des Alphabets

	public DecodeBaseN(String inputPath, String outputPath, int typeOfDecoding, String alphabet) throws ConverterException {
		inputFile = new File(inputPath);
		outputFile = new File(outputPath);
		this.alphabet = alphabet;
		this.typeOfDecoding = typeOfDecoding;
		decode();
	}
	
	/**
	 * dekodiert die Input-Datei
	 */
	public void decode() throws ConverterException {
		try {
			FileReader fileReader = new FileReader(inputFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
			
//			für base-n Dekodierung Alphabet in der ersten Zeile lesen
			if (typeOfDecoding == 0) {
				alphabet = bufferedReader.readLine();
				typeOfDecoding = alphabet.length();
			}
			
			switch (typeOfDecoding) {
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
					bufferedOutputStream.close();
					bufferedReader.close();
					throw new ConverterException("Ungültiger Kodierungs-Typ");
				
			}
//			Speichern des Alphabets in der Map
			map = new HashMap<String, Integer>();
			for (int i = 0; i < alphabet.length(); i++) {
				map.put(alphabet.charAt(i) + "", i);
			}
			
			int zeichenInput;
			int zeichenOutput = 0;
			int zaehlerBitsZeichenOutput = 0;
//			ein Byte wird gelesen
			while ((zeichenInput = bufferedReader.read()) != -1) {
//			der zum Zeichen gehoerige int wird ermittelt
				zeichenInput = map.get(Character.toString(zeichenInput));
//			Bit für Bit wird gelesen und in das Ausgabe-Byte geschrieben
				for (int i = 0; i < bitsProZeichen; i++) {
					int faktor = zeichenInput >>> bitsProZeichen-1-i;
					faktor = faktor&0x1;
					zeichenOutput += faktor * (int)Math.pow(2, 7-zaehlerBitsZeichenOutput);
					zaehlerBitsZeichenOutput ++;
					if (zaehlerBitsZeichenOutput == 8) {
						bufferedOutputStream.write(zeichenOutput);
						zaehlerBitsZeichenOutput = 0;
						zeichenOutput = 0;
					}
				}
			}

			bufferedOutputStream.flush();
			bufferedOutputStream.close(); 
			bufferedReader.close();
		} catch (FileNotFoundException e) {
			throw new ConverterException("Datei kann nicht gefunden werden");
		} catch (IOException e) {
			throw new ConverterException("Beim Lesen der Datei ist ein Fehler aufgetreten");
		}
	}

}
