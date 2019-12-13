package propra.imageconverter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author Michael Köhler
 * Eine Instanz dieser Klasse übernimmt die Konvertierung 
 * vom Propra-Format in das TGA-Format
 * dabei wird je nach Bedarf komprimiert und/oder dekomprimiert
 */

public class ConverterPropraToTga {
	
	private File inputFile;
	private File outputFile;
	private boolean rleCompressionOutputFile;
	private PropraFormat propraFormat;
	
    private int imageWidth;
    private int imageHeight;
    private long dataSegmentSize;
    private byte typeOfCompression;
    private long sizeOfDataSegmentOutputFile = 0;
	private boolean uncompressInputFile = false;
	private boolean compressOutputFile = false;

	/**
	 * @author Michael Koehler
	 * Eine Instanz dieser Klasse übernimmt die Konvertierung vom
	 * Propra-Format in das tga-Format
	 * dabei wird je nach Bedarf komprimiert und/oder dekomprimiert
	 */
	public ConverterPropraToTga(String inputPath, String outputPath, boolean rleCompressionOutputFile) throws ConverterException {
		inputFile = new File(inputPath);
		outputFile = new File(outputPath);
		this.rleCompressionOutputFile = rleCompressionOutputFile;
		
		propraFormat = new PropraFormat(inputPath); // Ueberpruefen der Input-Datei
		
		imageWidth = propraFormat.getImageWidth();
		imageHeight = propraFormat.getImageHeight();
		dataSegmentSize = propraFormat.getDataSegmentSize();
		typeOfCompression = propraFormat.getTypeOfCompression();

		if (typeOfCompression == 1 && !rleCompressionOutputFile) uncompressInputFile = true;
		if (typeOfCompression == 0 && rleCompressionOutputFile) compressOutputFile = true;
		if (typeOfCompression == 1 && rleCompressionOutputFile) {
			uncompressInputFile = true;
			compressOutputFile = true;
		}
		convertToTga();
	}

	/* 
	 * konvertiert den Header und die Pixel in das tga-Format
	 */
	public void convertToTga() throws ConverterException {
		try {
			FileInputStream fileInputStream = new FileInputStream(inputFile);
			BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
			FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
			
//			erstelle Header
			byte[] headerBegin = {0,0};
			bufferedOutputStream.write(headerBegin);
//			schreibe Kompressionstyp
			if (rleCompressionOutputFile) bufferedOutputStream.write(10);
			else bufferedOutputStream.write(2);
			byte[] headerFields = {0,0,0,0,0,0,0};
			bufferedOutputStream.write(headerFields);
//			schreibe Y Nullpunkt
			bufferedOutputStream.write(propraFormat.getHeader()[12]);
			bufferedOutputStream.write(propraFormat.getHeader()[13]);
//			schreibe Bildbreite
			bufferedOutputStream.write(propraFormat.getHeader()[10]);
			bufferedOutputStream.write(propraFormat.getHeader()[11]);
//			schreibe Bildhoehe
			bufferedOutputStream.write(propraFormat.getHeader()[12]);
			bufferedOutputStream.write(propraFormat.getHeader()[13]);
//			schreibe BitsPerPixel
			bufferedOutputStream.write(24);
//			schreibe BildAttribut
			bufferedOutputStream.write(32);
//			ueberspringe 18 Byte und beginne mit dem Lesen ab dem Datensegment
			bufferedInputStream.skip(28);
			
			byte[] inputPixel = new byte[3];
			byte[] outputPixel = new byte[3];
			byte[] outputByteCompressed = new byte[1];
			byte[] inputLine = new byte[imageWidth*3];
			byte[] outputLineCompressed;
			
			for (int line = 0; line < imageHeight; line++) {
//				Einlesen einer Bildlinie
				if (uncompressInputFile) {
					inputLine = Utility.uncompressInputLine(bufferedInputStream, imageWidth);
				} else {
					for (int pixel = 0; pixel < imageWidth; pixel++) {
						inputPixel = bufferedInputStream.readNBytes(3);
						for (int i = 0; i < 3; i++) {
							inputLine[pixel*3 + i] = inputPixel[i];
						}
					}
				}
				
				if (compressOutputFile) {
//					Farbreihenfolge im Pixel ändern, GBR --> BGR
					for (int pixel = 0; pixel < imageWidth; pixel++) {
						byte temp;
						temp = inputLine[pixel*3];
						inputLine[pixel*3] = inputLine[pixel*3 + 1];
						inputLine[pixel*3 + 1] = temp;
					}
//					Linie komprimieren
					outputLineCompressed = Utility.compressOutputLine(inputLine, imageWidth);
//					Linie in Ausgabe-Datei schreiben
					for (int i = 0; i < outputLineCompressed.length; i++) {
						outputByteCompressed[0] = outputLineCompressed[i];
						bufferedOutputStream.write(outputByteCompressed);
					}
				} 
				if (!compressOutputFile){
//					konvertieren und in OutputFile schreiben
					for (int pixel = 0; pixel < imageWidth; pixel++) {
						for (int i = 0; i < 3; i++) {
							inputPixel[i] = inputLine[pixel*3 + i];
						}
						outputPixel = convertPixel(inputPixel);
						bufferedOutputStream.write(outputPixel);
					}
				}
			}

			bufferedInputStream.close();
			bufferedOutputStream.flush();
			bufferedOutputStream.close();
			fileOutputStream.close();
			fileInputStream.close();
		} catch (FileNotFoundException e) {
			throw new ConverterException("Datei kann nicht gefunden werden");
		} catch (IOException e) {
			throw new ConverterException("Beim Lesen der Datei ist ein Fehler aufgetreten");
		}
	}
	
	private byte[] convertPixel(byte[] inputPixel) { // Farbreihenfolge im Pixel ändern, GBR --> BGR
		byte[] temp = new byte[3];
		temp[0] = inputPixel[1];
		temp[1] = inputPixel[0];
		temp[2] = inputPixel[2];
		return temp;
	}
}
