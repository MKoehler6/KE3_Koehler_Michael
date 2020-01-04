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

import propra.imageconverter.formats.PropraFormat;
import propra.imageconverter.huffman.HuffmanDecoding;
import propra.imageconverter.huffman.HuffmanEncoding;

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
	private boolean rleCompressionOutputFile;
	private boolean uncompressHuffmanInputFile = false;
	
	public CopyCompressDecompressPropraFile(String inputPath, String outputPath, boolean rleCompressionOutputFile,
			boolean huffmanCompressionOutputFile) throws ConverterException {
		this.huffmanCompressionOutputFile = huffmanCompressionOutputFile;
		this.rleCompressionOutputFile = rleCompressionOutputFile;
		inputFile = new File(inputPath);
		outputFile = new File(outputPath);
		propraFormat = new PropraFormat(inputPath);
		imageWidth = propraFormat.getImageWidth();
		imageHeight = propraFormat.getImageHeight();
		if (propraFormat.getTypeOfCompression() == 2) uncompressHuffmanInputFile = true;
//		hier wird entschieden, was alles komprimiert und dekomprimiert werden muss
//		und welche Methode zur Anwendung kommt
//		die Codestruktur könnte hier sicherlich noch optimiert werden
		if (propraFormat.getTypeOfCompression() == 1 && !rleCompressionOutputFile) uncompressRleAndCopy();
		if ((propraFormat.getTypeOfCompression() == 0 || propraFormat.getTypeOfCompression() == 2) 
				&& (rleCompressionOutputFile || huffmanCompressionOutputFile)) copyAndCompressRleOrHuffman();
		if (propraFormat.getTypeOfCompression() == 0 && !rleCompressionOutputFile && !huffmanCompressionOutputFile) copy();
		if (propraFormat.getTypeOfCompression() == 1 && rleCompressionOutputFile) copy();
		if (propraFormat.getTypeOfCompression() == 2 && huffmanCompressionOutputFile) copy();
		if (propraFormat.getTypeOfCompression() == 2 
				&& !rleCompressionOutputFile && !huffmanCompressionOutputFile) copyAndCompressRleOrHuffman();
	}
	
	/* 
	 * kopiert den Header und die Pixel und dekomprimiert vorher
	 * die Input-Datei
	 */
	public void uncompressRleAndCopy() throws ConverterException {
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
			byte[] typeOfCompression = {0};
			fileChannel.write(ByteBuffer.wrap(typeOfCompression), 15); // schreibe typeOfCompression
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
	 * die Output-Datei, bei Bedarf wird vorher noch zeilenweise huffman-dekodiert
	 */
	public void copyAndCompressRleOrHuffman() throws ConverterException {
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
			
//			wenn Input-Datei Huffman-kodiert ist: Huffman-Baum auslesen
			HuffmanDecoding huffmanUtility = new HuffmanDecoding();
			if (uncompressHuffmanInputFile) {
				huffmanUtility.readHuffmanTree(inputFile);
				bufferedInputStream.skip(huffmanUtility.counterAllBitsOfTree/8 + 1); // Huffman-Baum und dann 
				// das nächste Byte
			}
//			wenn Output-Datei Huffman-kodiert werden soll: Huffman-Baum und CodeBook erstellen
			HuffmanEncoding huffmanEncoding = new HuffmanEncoding();
			if (huffmanCompressionOutputFile) {
				huffmanEncoding.createHuffmanTreeAndCodeBook(inputFile, imageWidth, imageHeight);
			}
			
			for (int line = 0; line < imageHeight; line++) {
//				Einlesen einer Bildlinie
				if (uncompressHuffmanInputFile) {
					inputLine = huffmanUtility.decodeHuffman(bufferedInputStream, imageWidth);
				} else {
					for (int pixel = 0; pixel < imageWidth; pixel++) {
						inputPixel = bufferedInputStream.readNBytes(3);
						for (int i = 0; i < 3; i++) {
							inputLine[pixel*3 + i] = inputPixel[i];
						}
					}
				} 
//				Line in Output-Datei ausgeben
				if (rleCompressionOutputFile) {
					outputLineCompressed = Utility.compressOutputLine(inputLine, imageWidth);
	//				Line in OutputFile schreiben
					for (int i = 0; i < outputLineCompressed.length; i++) {
						outputByteCompressed[0] = outputLineCompressed[i];
						calculateCheckSum(outputByteCompressed);
						bufferedOutputStream.write(outputByteCompressed);
					} 
				} else if (huffmanCompressionOutputFile) {
					outputLineCompressed = huffmanEncoding.writeEncodedPixelInOutputLine(inputLine);
					calculateCheckSum(outputLineCompressed);
					bufferedOutputStream.write(outputLineCompressed);
				} else { // Output-Datei bleibt unkomprimiert 
					calculateCheckSum(inputLine);
					bufferedOutputStream.write(inputLine);
				}
			}
			
//			bei Huffman Kodierung: wenn die restlichen Bits kein vollständiges Byte ergeben, wird
//			jetzt das letzte Byte mit 0 aufgefüllt (Padding) und geschrieben
			if (huffmanEncoding.bitArrayForOneByte.size() > 0) {
				while (huffmanEncoding.bitArrayForOneByte.size() < 8) {
					huffmanEncoding.bitArrayForOneByte.add(0);
				}
				bufferedOutputStream.write(huffmanEncoding.toByteValue(huffmanEncoding.bitArrayForOneByte));
				byte[] lastByte = {(byte)huffmanEncoding.toByteValue(huffmanEncoding.bitArrayForOneByte)};
				calculateCheckSum(lastByte);
			}
			
//			Header anpassen
			bufferedOutputStream.flush();
			FileChannel fileChannel = fileOutputStream.getChannel();
			byte [] typeOfCompression = new byte[1];
			if (rleCompressionOutputFile) {
				typeOfCompression[0] = 1;
			} else if (huffmanCompressionOutputFile) {
				typeOfCompression[0] = 2;
			} else {
				typeOfCompression[0] = 0;
			}
			fileChannel.write(ByteBuffer.wrap(typeOfCompression), 15); // schreibe typeOfCompression
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
	 * kopiert den Header und die Pixel unverändert
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
