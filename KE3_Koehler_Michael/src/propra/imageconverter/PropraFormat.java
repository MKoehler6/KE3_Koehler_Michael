package propra.imageconverter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * @author Michael Koehler
 * Eine Instanz dieser Klasse liest den Header der Input-Datei und ueberprueft, 
 * ob die Datei den Anforderungen entspricht
 *
 */
public class PropraFormat implements IFormat {
	
	private byte[] header = new byte[28];
    private int imageWidth;
    private int imageHeight;
    private byte bitsPerPixel;
    private byte typeOfCompression;
    private long sizeOfDataSegmentinHeader;
    private long sizeOfDataSegment;
    private long checkSumFromHeader;
    private long calculatedCheckSum;
    private File inputFile;

	public PropraFormat(String inputPath) throws ConverterException {
		inputFile = new File(inputPath);
		readHeader();
		checkDataOfHeader();
	}
	
	/* 
	 * liest den Header ein und speichert relevante Daten
	 */
	public void readHeader() throws ConverterException {
		try(FileInputStream fileInputStream = new FileInputStream(inputFile)) {
			header = fileInputStream.readNBytes(28);
		} catch (FileNotFoundException e) {
			throw new ConverterException("Datei kann nicht gefunden werden");
		} catch (IOException e) {
			throw new ConverterException("Beim Lesen der Datei ist ein Fehler aufgetreten");
		}
		
		byte[] widthArray = {header[10], header[11],0,0};
		imageWidth = ByteBuffer.wrap(widthArray).order(ByteOrder.LITTLE_ENDIAN).getInt();
		byte[] heightArray = {header[12], header[13],0,0};
        imageHeight = ByteBuffer.wrap(heightArray).order(ByteOrder.LITTLE_ENDIAN).getInt();
        bitsPerPixel = header[14];
        typeOfCompression = header[15];
        byte[] sizeOfDataSegmentArray = Arrays.copyOfRange(header, 16, 24);
        sizeOfDataSegmentinHeader = ByteBuffer.wrap(sizeOfDataSegmentArray).order(ByteOrder.LITTLE_ENDIAN).getLong();
        sizeOfDataSegment = inputFile.length() - 28;
        byte[] checkSumArray = {header[24], header[25], header[26], header[27], 0, 0, 0, 0};
        checkSumFromHeader = ByteBuffer.wrap(checkSumArray).order(ByteOrder.LITTLE_ENDIAN).getLong();
	}
	
	/* 
	 * überprüft alle Anforderungen an die Propra-Datei und die Korrektheit der Daten im Header 
	 * und die Prüfsumme
	 */
	public void checkDataOfHeader() throws ConverterException {
//		prüfe optionale Anforderungen an Propra-Datei
		checkImageDimensions(); // Bildbreite oder -höhe dürfen nicht 0 sein
		checkTypeOfCompression(); // Kompressionstyp muss 0 sein
		if (typeOfCompression == 0)  checkCorrectAmountOfPixel();
		checkCheckSum();
		checkSizeOfDataSegmentInHeader();
//		prüfe, ob Spezifikationen der Propra-Dateien eingehalten werden
		checkNameProPraWS19();
		checkBitsPerPixel(); // Anzahl der Bits pro Pixel muss 24 sein
	}
	

	private void checkImageDimensions() throws ConverterException {
		if (imageWidth == 0 || imageHeight == 0)
			throw new ConverterException("mind. eine Bilddimension ist 0");
	}
	
	private void checkTypeOfCompression() throws ConverterException {
		if (typeOfCompression != 0 && typeOfCompression != 1) 
			throw new ConverterException("nicht unterstützter Kompressionstyp");
	}
	
	/**
	 * überprüft, ob die Anzahl der Pixel mit den Angaben im Header übereinstimmen
	 */
	private void checkCorrectAmountOfPixel() throws ConverterException{
		if (sizeOfDataSegment != imageWidth * imageHeight * 3) 
			throw new ConverterException("Pixelanzahl stimmt nicht mit Breite x Höhe im Header überein");
	}
	
	/**
	 * liest das komplette Datensegment und berechnet dabei byteweise die Pruefsumme
	 */
	private void checkCheckSum() throws ConverterException{
		try {
			FileInputStream fileInputStream = new FileInputStream(inputFile);
			BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
			bufferedInputStream.skip(28);
			byte[] data;
			long[] anbn = {0,1};
			for (long i = 0; i < sizeOfDataSegment; i++) {
				data = bufferedInputStream.readNBytes(1);
				anbn = Utility.calculateCheckSumOfLine(data, anbn, i);
			}
			long an = anbn[0];
			long bn = anbn[1];
			an = an % 65513;
			calculatedCheckSum = an*65536 + bn;

			bufferedInputStream.close();
			fileInputStream.close();
		} catch (FileNotFoundException e) {
			throw new ConverterException("Datei kann nicht gefunden werden");
		} catch (IOException e) {
			throw new ConverterException("Beim Lesen der Datei ist ein Fehler aufgetreten");
		} 
//		Abgleich der berechneten Pruefsumme mit dem Header
		if (checkSumFromHeader != calculatedCheckSum) {
			throw new ConverterException("Prüfsumme falsch");
		}
	}
	
	/**
	 * überprüft die Angaben der Bildbreite und -höhe im Header
	 */
	private void checkSizeOfDataSegmentInHeader() throws ConverterException{
		if (sizeOfDataSegment != sizeOfDataSegmentinHeader) {
			throw new ConverterException("falsche Dateigröße im Header");
		}
	}

	/**
	 * überprüft die Angabe "ProPraWS19" im Header
	 */
	private void checkNameProPraWS19() throws ConverterException{
		byte[] nameOfFormat = {80, 114, 111, 80, 114, 97, 87, 83, 49, 57}; // = ProPraWS19
		for (int i = 0; i < nameOfFormat.length; i++) {
			if (header[i] != nameOfFormat[i]) throw new ConverterException("Formatname nicht "
					+ "korrekt angegeben");
		}
	}
	
	private void checkBitsPerPixel() throws ConverterException {
		if (bitsPerPixel != 24) throw new ConverterException("Bits pro Pixel nicht korrekt");
	}

	public int getImageWidth() {
		return imageWidth;
	}

	public int getImageHeight() {
		return imageHeight;
	}

	public long getDataSegmentSize() {
		return sizeOfDataSegment;
	}
	
	public byte getTypeOfCompression() {
		return typeOfCompression;
	}
	
	public byte[] getHeader() {
		return header;
	}
}
