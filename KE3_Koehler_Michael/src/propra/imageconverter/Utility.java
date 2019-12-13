package propra.imageconverter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * @author Michael Koehler
 * Diese Klasse enthaelt static Methoden, die im gesamten Programm immer wieder gebraucht werden
 * Methoden für Komprimierung, Dekomprimierung, Pruefsummenberechnung, Hashwert-Berechnung zur Kontrolle
 * und Methoden fuer das Byte-Handling
 */
public class Utility {
	
	/**
	 * dekomprimiert eine Bildlinie, zu diesem Zweck wird die Bildbreite uebergeben, damit die Methode
	 * weiss, wie lang eine Bildlinie sein muss
	 */
	public static byte[] uncompressInputLine(BufferedInputStream bufferedInputStream, int imageWidth) throws IOException{
		int pixelInLine = 0; // Zaehler für die Bytes der dekomprimierten Linie
		int controlByte = 0;
		byte[] singlePixel = null;
		int repetitionCounter;
		int dataCounter;
		byte[] byteArray = null;
		int pixelProLineUncompressed = imageWidth;
		byte[] line = new byte[pixelProLineUncompressed*3];
		while (pixelInLine < pixelProLineUncompressed) {
			controlByte = bufferedInputStream.read(); // erstes Steuerbyte in Zeile
			if (controlByte >>> 7 == 0) { // Datenzaehler
				dataCounter = (controlByte & 0x7f)+1;
				byteArray = bufferedInputStream.readNBytes(dataCounter*3);
				for (int i = 0; i < byteArray.length; i++) {
					line[pixelInLine*3 + i] = byteArray[i];
				}
				pixelInLine += dataCounter;
			}
			if (controlByte >>> 7 == 1) { // Wiederholungszaehler
				repetitionCounter = (controlByte & 0x7f)+1;
				singlePixel = bufferedInputStream.readNBytes(3);
				for (int i = 0; i < repetitionCounter; i++) {
					for (int j = 0; j < 3; j++) {
						line[pixelInLine*3 + i*3 + j] = singlePixel[j];
					}
				}
				pixelInLine += repetitionCounter;
			}
		}
		return line;
	}
	
	/**
	 * komprimiert eine Bildlinie
	 * die Codestruktur muss stellenweise noch verbessert werden, leider fehlte mir die Zeit dafuer
	 */
	public static byte[] compressOutputLine(byte[] line, int imageWidth) {
		
		ArrayList<Byte> lineCompressedArray = new ArrayList<>();
		ArrayList<Byte> pixelDifferentArray = new ArrayList<>(); // zum Zwischenspeichern der sich unterscheidenden Pixel
		int repetitionCounter = 0;
		int dataCounter = 0;
		int pixelInLineCounter = 0;
		boolean pixelEqual = false;
		boolean pixelDifferent = false;
		int controlByte;
		byte[] firstPixel = new byte[3];
		byte[] secondPixel = new byte[3];
		
		while (pixelInLineCounter < imageWidth-1) {
//			ersten und zweiten Pixel lesen
			firstPixel[0] = line[pixelInLineCounter*3];
			firstPixel[1] = line[pixelInLineCounter*3 + 1];
			firstPixel[2] = line[pixelInLineCounter*3 + 2];
			secondPixel[0] = line[pixelInLineCounter*3 + 3];
			secondPixel[1] = line[pixelInLineCounter*3 + 4];
			secondPixel[2] = line[pixelInLineCounter*3 + 5];
			
			if (firstPixel[0] == secondPixel[0] && firstPixel[1] == secondPixel[1] && firstPixel[2] == secondPixel[2]) {
//				Pixel gleich
				pixelEqual = true;
			}
			if (!(firstPixel[0] == secondPixel[0] && firstPixel[1] == secondPixel[1] && firstPixel[2] == secondPixel[2])) {
//				Pixel unterschiedlich
				pixelDifferent = true;
			}
			
			if (pixelEqual) {
//				zaehle die gleichen Pixel
				while (repetitionCounter < 127 && pixelInLineCounter + repetitionCounter < imageWidth-1 &&
						line[repetitionCounter*3 + pixelInLineCounter*3] == line[repetitionCounter*3 + pixelInLineCounter*3 + 3] &&
						line[repetitionCounter*3 + pixelInLineCounter*3 + 1] == line[repetitionCounter*3 + pixelInLineCounter*3 + 4] &&
						line[repetitionCounter*3 + pixelInLineCounter*3 + 2] == line[repetitionCounter*3 + pixelInLineCounter*3 + 5]){
					repetitionCounter++;
				}
//				schreibe Steuerbyte fuer Wiederholung und das Pixel
				controlByte = 128 + repetitionCounter;
				lineCompressedArray.add((byte)controlByte);
				lineCompressedArray.add(firstPixel[0]);
				lineCompressedArray.add(firstPixel[1]);
				lineCompressedArray.add(firstPixel[2]);
				
				pixelInLineCounter+=repetitionCounter+1;
				
//				schreibe letzten Pixel in der Zeile bzw. im Block, wenn am Ende noch ein einzelnes Pixel steht
				if (pixelInLineCounter == imageWidth-1) {
					lineCompressedArray.add((byte)0); // Steuerbyte
					lineCompressedArray.add(line[pixelInLineCounter*3]);
					lineCompressedArray.add(line[pixelInLineCounter*3 + 1]);
					lineCompressedArray.add(line[pixelInLineCounter*3 + 2]);
				}
				
				repetitionCounter = 0;
				pixelEqual = false;
				
			}
			
			if (pixelDifferent) {
//				zaehle die unterschiedlichen Pixel
				while ((dataCounter < 127) && (pixelInLineCounter + dataCounter < imageWidth-1) &&
						!(line[dataCounter*3 + pixelInLineCounter*3] == line[dataCounter*3 + pixelInLineCounter*3 + 3] &&
						line[dataCounter*3 + pixelInLineCounter*3 + 1] == line[dataCounter*3 + pixelInLineCounter*3 + 4] &&
						line[dataCounter*3 + pixelInLineCounter*3 + 2] == line[dataCounter*3 + pixelInLineCounter*3 + 5])){
//					schreibe Pixel in ein Array zum zwischenspeichern
					pixelDifferentArray.add(line[dataCounter*3 + pixelInLineCounter*3]);
					pixelDifferentArray.add(line[dataCounter*3 + pixelInLineCounter*3 + 1]);
					pixelDifferentArray.add(line[dataCounter*3 + pixelInLineCounter*3 + 2]);
					dataCounter++;
				}
//				am Ende der Zeile oder eines 128-Blocks schreibe das letzte Pixel:
				if ((dataCounter == 127)&&(pixelInLineCounter + dataCounter < imageWidth-1)
						&& (line[dataCounter*3 + pixelInLineCounter*3] == line[dataCounter*3 + pixelInLineCounter*3 + 3] 
						&& line[dataCounter*3 + pixelInLineCounter*3 + 1] == line[dataCounter*3 + pixelInLineCounter*3 + 4] 
						&& line[dataCounter*3 + pixelInLineCounter*3 + 2] == line[dataCounter*3 + pixelInLineCounter*3 + 5])) {
					
				} else if ((dataCounter == 127) || (pixelInLineCounter + dataCounter == imageWidth-1)) {
					pixelDifferentArray.add(line[dataCounter*3 + pixelInLineCounter*3]);
					pixelDifferentArray.add(line[dataCounter*3 + pixelInLineCounter*3 + 1]);
					pixelDifferentArray.add(line[dataCounter*3 + pixelInLineCounter*3 + 2]);
					dataCounter++;
				} 
				if ((dataCounter == 128) && (pixelInLineCounter + dataCounter == imageWidth-1)) {
					pixelDifferentArray.add((byte)0);
					pixelDifferentArray.add(line[dataCounter*3 + pixelInLineCounter*3]);
					pixelDifferentArray.add(line[dataCounter*3 + pixelInLineCounter*3 + 1]);
					pixelDifferentArray.add(line[dataCounter*3 + pixelInLineCounter*3 + 2]);
				}

//				schreibe Steuerbyte fuer Datenzaehler und die zwischengespeicherten unterschiedlichen Pixel
				controlByte = dataCounter-1;
				lineCompressedArray.add((byte)controlByte);
				for (int i = 0; i < pixelDifferentArray.size(); i++) {
					lineCompressedArray.add(pixelDifferentArray.get(i));
				}
				pixelDifferentArray.clear();
				pixelInLineCounter+=dataCounter;
				dataCounter = 0;
				pixelDifferent = false;
			}
			
		}
//		uebertrage die komprimierten Pixel in ein byte-Array
		byte[] lineCompressedByteArray = new byte[lineCompressedArray.size()];
		for (int i = 0; i < lineCompressedByteArray.length; i++) {
			lineCompressedByteArray[i] = lineCompressedArray.get(i);
		}
		return lineCompressedByteArray;
	}
	
	/**
	 * berechnet Pruefsumme einer Bildlinie
	 */
	public static long[] calculateCheckSumOfLine(byte[] line, long[] anbn, long numberOfLine) {
		long an = anbn[0];
		long bn = anbn[1];
		long offset = line.length * numberOfLine;
		long[] results = new long[2];
		int x = 65513;
		for (int i = 0; i < line.length; i++) {
			an = an + (Byte.toUnsignedInt(line[i])+i+1+offset);
			bn = (bn + an % x) % x;
		}
		
		results[0] = an;
		results[1] = bn;
		
		return results;
	}
	
	/**
	 * berechnet Pruefsumme byte fuer byte
	 */
	public static long[] calculateCheckSumByteByByte(byte b, long[] anbn, long offset) {
		long an = anbn[0];
		long bn = anbn[1];
		long[] results = new long[2];
		int x = 65513;
		an = an + (Byte.toUnsignedInt(b)+1+offset);
		bn = (bn + an % x) % x;
		
		results[0] = an;
		results[1] = bn;
		
		return results;
	}
	
	/**
	 * berechnet aus einem Byte-Array der Länge 8 im Little-Endian-Format einen long-Wert
	 */
	public static long byteToLong8(byte[] data) { // benötigt Array mit Länge 8
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getLong();
	}
	
	/**
	 * berechnet aus einem Byte-Array der Länge 4 im Little-Endian-Format einen Integer-Wert
	 */
	public static int byteToInt4(byte[] data) { // benötigt Array mit Länge 4
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getInt();
	}
	
	/**
	 * gibt Länge des Datensegments als Byte-Array der Länge 8 zurück
	 * datenSegementGroesse = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(header.getDatenSegmentGroesse()).array();
	 */
	public static byte[] longToByteArray(long l) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		byte[] temp = buffer.order(ByteOrder.LITTLE_ENDIAN).putLong(l).array();
		return temp;
	}
	
	/**
	 * zum Ueberpruefen der Output-Datei wird der Hashwert berechnet 
	 */
	public static void md5(File outputFile) throws ConverterException {
		try {
			FileInputStream fileInputStream = new FileInputStream(outputFile);
			BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
			
			byte[] datei = bufferedInputStream.readAllBytes();
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] md5 = md.digest(datei);
			for (int i = 0; i < md5.length; i++) {
				System.out.print(String.format("%02X", md5[i]));
			}
			System.out.println();
			
			bufferedInputStream.close();
			fileInputStream.close();
		} catch (FileNotFoundException e) {
			throw new ConverterException("Datei kann nicht gefunden werden");
		} catch (IOException e) {
			throw new ConverterException("Beim Lesen der Datei ist ein Fehler aufgetreten");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
