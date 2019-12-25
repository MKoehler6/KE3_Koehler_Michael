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

/**
 * @author Michael Koehler
 * Eine Instanz dieser Klasse kopiert vom
 * Propra-Format in das Propra-Format
 * dabei wird je nach Bedarf komprimiert und/oder dekomprimiert
 */

public class CopyCompressDecompressPropraFile {
	
	private PropraFormat propraFormat;
	private File inputFile;
	private File outputFile;
	private long sizeOfDataSegmentOutputFile = 0;
	private int imageWidth;
    private int imageHeight;
	private long[] anbn = {0,1}; // fuer die Pruefsummenberechnung, speichert An und Bn zwischen;
	private long calculatedCheckSum;
	private boolean huffmanCompressionOutputFile;
	
	public CopyCompressDecompressPropraFile(String inputPath, String outputPath, boolean rleCompressionOutputFile,
			boolean huffmanCompressionOutputFile) throws ConverterException {
		this.huffmanCompressionOutputFile = huffmanCompressionOutputFile;
		inputFile = new File(inputPath);
		outputFile = new File(outputPath);
		propraFormat = new PropraFormat(inputPath);
		imageWidth = propraFormat.getImageWidth();
		imageHeight = propraFormat.getImageHeight();
		if (propraFormat.getTypeOfCompression() == 1 && !rleCompressionOutputFile) uncompressAndCopy();
		if (propraFormat.getTypeOfCompression() == 0 && rleCompressionOutputFile) copyAndCompress();
		if (propraFormat.getTypeOfCompression() == 0 && !rleCompressionOutputFile) copy();
		if (propraFormat.getTypeOfCompression() == 1 && rleCompressionOutputFile) copy();
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
			for (int i = 0; i < 28; i++) {
				bufferedOutputStream.write(bufferedInputStream.read());
			}
			byte[] outputPixel = new byte[3];
			byte[] inputLine = new byte[imageWidth*3];
			
			for (int line = 0; line < imageHeight; line++) {
//				Einlesen einer Bildlinie
				inputLine = Utility.uncompressInputLine(bufferedInputStream, imageWidth);
				
//				in OutputFile schreiben
				for (int pixel = 0; pixel < imageWidth; pixel++) {
					for (int i = 0; i < 3; i++) {
						outputPixel[i] = inputLine[pixel*3 + i];
					}
					calculateCheckSum(outputPixel);
					bufferedOutputStream.write(outputPixel);
				}
			}
			
//			Header anpassen
			bufferedOutputStream.flush();
			FileChannel fileChannel = fileOutputStream.getChannel();
			byte[] test = {0};
			fileChannel.write(ByteBuffer.wrap(test), 15); // schreibe typeOfCompression
			byte[] sizeOfDataSegment = Utility.longToByteArray(sizeOfDataSegmentOutputFile);
			fileChannel.write(ByteBuffer.wrap(sizeOfDataSegment), 16); // schreibe SizeOfDataSegment in Header
			
			calculatedCheckSum = calculateCheckSumFinal(); // schliesse Berechnung der Pruefsumme ab 
			byte[] checkSum = Utility.longToByteArray(calculatedCheckSum);
			byte[] temp = new byte[1];
			for (int i = 0; i < 4; i++) {
				temp[0] = checkSum[i];
				fileChannel.write(ByteBuffer.wrap(temp), 24+i); // schreibe die ersten 4 Bytes von checkSum in Header
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
//			kopiere Header
			for (int i = 0; i < 28; i++) {
				bufferedOutputStream.write(bufferedInputStream.read());
			}
			byte[] inputPixel = new byte[3];
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
					calculateCheckSum(outputByteCompressed);
					bufferedOutputStream.write(outputByteCompressed);
				}
			}
			
//			Header anpassen
			bufferedOutputStream.flush();
			FileChannel fileChannel = fileOutputStream.getChannel();
			byte[] test = {1};
			fileChannel.write(ByteBuffer.wrap(test), 15); // schreibe typeOfCompression
			byte[] sizeOfDataSegment = Utility.longToByteArray(sizeOfDataSegmentOutputFile);
			fileChannel.write(ByteBuffer.wrap(sizeOfDataSegment), 16); // schreibe SizeOfDataSegment in Header
			
			calculatedCheckSum = calculateCheckSumFinal(); // schliesse Berechnung der Pruefsumme ab 
			byte[] checkSum = Utility.longToByteArray(calculatedCheckSum);
			byte[] temp = new byte[1];
			for (int i = 0; i < 4; i++) {
				temp[0] = checkSum[i];
				fileChannel.write(ByteBuffer.wrap(temp), 24+i); // schreibe die ersten 4 Bytes von checkSum in Header
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
	
	private void calculateCheckSum(byte[] data) {
//		Weitergabe der Bytes an die Methode calculateCheckSumByteByByte in Utility-Klasse
		for (int j = 0; j < data.length; j++) {
			anbn = Utility.calculateCheckSumByteByByte(data[j], anbn, sizeOfDataSegmentOutputFile);
			sizeOfDataSegmentOutputFile++;
		}
	}
	
	/**
	 * schliesst die Berechnung der Pruefsumme am Ende der Datei-Konvertierung ab
	 */
	private long calculateCheckSumFinal() {
		long an = anbn[0]; 
		long bn = anbn[1];
		an = an % 65513;
		long result = an*65536 + bn; 
		return result;
	}

}
