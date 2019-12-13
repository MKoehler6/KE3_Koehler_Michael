package propra.imageconverter;

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
public class TgaFormat implements IFormat{
	
	private byte[] header = new byte[18];
	private byte imageIDLength;
    private byte imageType;
    private int xZero;
    private int yZero;
    private int imageWidth;
    private int imageHeight;
    private byte bitsPerPixel;
    private byte imageAttributeByte;
    private long sizeOfDataSegment;
    private File inputFile;

	public TgaFormat (String inputPath) throws ConverterException{
		inputFile = new File(inputPath);
		readHeader();
		checkDataOfHeader();
		
	}
	
	/* 
	 * liest den Header ein und speichert relevante Daten
	 */
	public void readHeader() throws ConverterException {
		try(FileInputStream fileInputStream = new FileInputStream(inputFile)) {
			header = fileInputStream.readNBytes(18);
		} catch (FileNotFoundException e) {
			throw new ConverterException("Datei kann nicht gefunden werden");
		} catch (IOException e) {
			throw new ConverterException("Beim Lesen der Datei ist ein Fehler aufgetreten");
		}
		
		imageIDLength = header[0];
		imageType = header[2];
		byte[] xZeroArray = {header[8], header[9],0,0};
		xZero = ByteBuffer.wrap(xZeroArray).order(ByteOrder.LITTLE_ENDIAN).getInt();
		byte[] yZeroArray = {header[10], header[11],0,0};
		yZero = ByteBuffer.wrap(yZeroArray).order(ByteOrder.LITTLE_ENDIAN).getInt();
		byte[] widthArray = {header[12], header[13],0,0};
		imageWidth = ByteBuffer.wrap(widthArray).order(ByteOrder.LITTLE_ENDIAN).getInt();
		byte[] heightArray = {header[14], header[15],0,0};
        imageHeight = ByteBuffer.wrap(heightArray).order(ByteOrder.LITTLE_ENDIAN).getInt();
        bitsPerPixel = header[16];
        imageAttributeByte = header[17];
        sizeOfDataSegment = inputFile.length() - 18;
	}

	/* 
	 * überprüft alle Anforderungen an die tga-Datei und die Korrektheit der Daten im Header 
	 */
	public void checkDataOfHeader() throws ConverterException {
//		prüfe optionale Anforderungen an TGA-Datei
		checkImageDimensions(); // Bildbreite oder -höhe dürfen nicht 0 sein
		if (imageType == 2) checkCorrectAmountOfPixel();
//		prüfe, ob geforderte Einschränkungen für TGA-Dateien eingehalten werden
		checkBitsPerPixel(); // nur 24Bit zugelasen
		checkImageType(); // nur Bildtyp 2 oder 10
		checkAttributeByte(); // vertikale Lage des Nullpunktes, Wert muss 32 betragen
		checkImageIDLength(); // muss Null sein
	}
	
	private void checkImageDimensions() throws ConverterException {
		if (imageWidth == 0 || imageHeight == 0) 
			throw new ConverterException("mind. eine Bilddimension ist 0");
	}

	/**
	 * überprüft, ob die Anzahl der Pixel mit den Angaben im Header übereinstimmen
	 * entfernt einen eventuell vorhandenen Dateifuß
	 */
	private void checkCorrectAmountOfPixel() throws ConverterException{
		if (sizeOfDataSegment < imageWidth * imageHeight * 3) 
			throw new ConverterException("Fehlende Pixeldaten."
				+ " Stimmen nicht mit Breite x Höhe im Header überein");
		if (sizeOfDataSegment < imageWidth * imageHeight * 3) {
			// TODO entferne Dateifuß
		}
	}

	private void checkBitsPerPixel() throws ConverterException {
		if (bitsPerPixel != 24) throw new ConverterException("Bits pro Pixel nicht korrekt");
	}
	
	private void checkImageType() throws ConverterException {
		if (imageType != 2 && imageType != 10) 
				throw new ConverterException("Bildtyp nicht korrekt");
	}
	
	private void checkAttributeByte() throws ConverterException {
		if (imageAttributeByte != 32) throw new ConverterException("Bild-Attribut-Byte nicht korrekt");
	}

	private void checkImageIDLength() throws ConverterException {
		if (imageIDLength != 0) throw new ConverterException("Länge der Bild-ID nicht korrekt");
	}
	
	public int getImageWidth() {
		return imageWidth;
	}

	public int getImageHeight() {
		return imageHeight;
	}
	
	public byte getImageType() {
		return imageType;
	}
	
	public byte[] getHeader() {
		return header;
	}
}
