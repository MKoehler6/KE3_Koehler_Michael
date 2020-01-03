package propra.imageconverter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import propra.imageconverter.formats.TgaFormat;

/**
 * @author Michael Köhler
 * Eine Instanz dieser Klasse kopiert die Daten vom dataInputArray in das
 * dataOutputArray
 *
 */
public class CopyCompressDecompressTgaFile {
	
	private File inputFile;
	private File outputFile;
	private TgaFormat tgaFormat;
	private int imageWidth;
    private int imageHeight;
	boolean zeilenAnfang;
	
	/**
	 * @author Michael Koehler
	 * Eine Instanz dieser Klasse kopiert vom
	 * tga-Format in das tga-Format
	 * dabei wird je nach Bedarf komprimiert und/oder dekomprimiert
	 */
	public CopyCompressDecompressTgaFile(String inputPath, String outputPath, boolean rleCompressionOutputFile) throws ConverterException {
		inputFile = new File(inputPath);
		outputFile = new File(outputPath);
		tgaFormat = new TgaFormat(inputPath);
		imageWidth = tgaFormat.getImageWidth();
		imageHeight = tgaFormat.getImageHeight();
		if (tgaFormat.getImageType() == 10 && !rleCompressionOutputFile) uncompressAndCopy();
		if (tgaFormat.getImageType() == 2 && rleCompressionOutputFile) copyAndCompress();
		if (tgaFormat.getImageType() == 2 && !rleCompressionOutputFile) copy();
		if (tgaFormat.getImageType() == 10 && rleCompressionOutputFile) copy();
	}

	/* 
	 * kopiert den Header und die Pixel und dekomprimiert vorher
	 * die Input-Datei
	 */
	public void uncompressAndCopy() throws ConverterException {
		try {
			FileInputStream fileInputStream = new FileInputStream(inputFile);
			BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
			FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
//			kopiere Header
			for (int i = 0; i < 18; i++) {
				bufferedOutputStream.write(bufferedInputStream.read());
			}
			
			byte[] inputPixel = new byte[3];
			byte[] outputPixel = new byte[3];
			byte[] outputByteCompressed = new byte[1];
			byte[] inputLine = new byte[imageWidth*3];
			byte[] outputLineCompressed;
			
			for (int line = 0; line < imageHeight; line++) {
//				Einlesen einer Bildlinie
				inputLine = Utility.uncompressInputLine(bufferedInputStream, imageWidth);
				
//				in OutputFile schreiben
				for (int pixel = 0; pixel < imageWidth; pixel++) {
					for (int i = 0; i < 3; i++) {
						outputPixel[i] = inputLine[pixel*3 + i];
					}
					bufferedOutputStream.write(outputPixel);
				}
			}
			
//			Header anpassen
			bufferedOutputStream.flush();
			FileChannel fileChannel = fileOutputStream.getChannel();
			byte[] test = {2};
			fileChannel.write(ByteBuffer.wrap(test), 2);
			
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
	
	/* 
	 * kopiert den Header und die Pixel und komprimiert anschliessend
	 * die Output-Datei
	 */
	public void copyAndCompress() throws ConverterException {
		try {
			FileInputStream fileInputStream = new FileInputStream(inputFile);
			BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
			FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
	//		kopiere Header
			for (int i = 0; i < 18; i++) {
				bufferedOutputStream.write(bufferedInputStream.read());
			}
			
			byte[] inputPixel = new byte[3];
			byte[] outputPixel = new byte[3];
			byte[] outputByteCompressed = new byte[1];
			byte[] inputLine = new byte[imageWidth*3];
			byte[] outputLineCompressed;
			
			for (int line = 0; line < imageHeight; line++) {
//				Einlesen einer Bildlinie
				for (int pixel = 0; pixel < imageWidth; pixel++) {
					inputPixel = bufferedInputStream.readNBytes(3);
					for (int i = 0; i < 3; i++) {
						inputLine[pixel*3 + i] = inputPixel[i];
					}
				}
//				Line komprimieren
				outputLineCompressed = Utility.compressOutputLine(inputLine, imageWidth);
//				Line in OutputFile schreiben
				for (int i = 0; i < outputLineCompressed.length; i++) {
					outputByteCompressed[0] = outputLineCompressed[i];
					bufferedOutputStream.write(outputByteCompressed);
				}
			}
			
//			Header anpassen
			bufferedOutputStream.flush();
			FileChannel fileChannel = fileOutputStream.getChannel();
			byte[] test = {10};
			fileChannel.write(ByteBuffer.wrap(test), 2);
			
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
	
	/* 
	 * kopiert den Header und die Pixel 
	 */
	public void copy() throws ConverterException {
		try {
			FileInputStream fileInputStream = new FileInputStream(inputFile);
			BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
			FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
			
			int data;
			while ((data = bufferedInputStream.read()) != -1) {
				bufferedOutputStream.write(data);
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
}
