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
import propra.imageconverter.huffman.HuffmanEncoding;

/**
 * @author Michael Koehler
 * Eine Instanz dieser Klasse übernimmt die Konvertierung vom
 * tga-Format in das Propra-Format
 * dabei wird je nach Bedarf komprimiert und/oder dekomprimiert
 */
public class ConverterTgaToPropra {
	
	private File inputFile;
	private File outputFile;
	private boolean rleCompressionOutputFile;
	private boolean huffmanCompressionOutputFile;
	private TgaFormat tgaFormat;
	
	private int imageWidth;
    private int imageHeight;
    private byte tgaImageType;
	private long sizeOfDataSegmentOutputFile = 0;
	private long[] anbn = {0,1}; // fuer die Pruefsummenberechnung, speichert An und Bn zwischen
	private long calculatedCheckSum;
	private boolean uncompressRleInputFile = false;

	public ConverterTgaToPropra(String inputPath, String outputPath, boolean rleCompressionOutputFile,
			boolean huffmanCompressionOutputFile) throws ConverterException {
		inputFile = new File(inputPath);
		outputFile = new File(outputPath);
		this.rleCompressionOutputFile = rleCompressionOutputFile;
		this.huffmanCompressionOutputFile = huffmanCompressionOutputFile;
		
		tgaFormat = new TgaFormat(inputPath); // Ueberpruefen der Input-Datei
		
		imageWidth = tgaFormat.getImageWidth();
		imageHeight = tgaFormat.getImageHeight();
		tgaImageType = tgaFormat.getImageType();
		
		if (tgaImageType == 10) uncompressRleInputFile = true;
		convertToPropra();
	}
	
	/* 
	 * konvertiert den Header und die Pixel in das Propra-Format
	 */
	public void convertToPropra() throws ConverterException {
		try {
			FileInputStream fileInputStream = new FileInputStream(inputFile);
			BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
			FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
			
//			erstelle Header, soweit die Daten schon verfuegbar sind
			byte[] nameOfFormat = {80, 114, 111, 80, 114, 97, 87, 83, 49, 57}; // = ProPraWS19
			bufferedOutputStream.write(nameOfFormat);
//			schreibe Bildbreite
			bufferedOutputStream.write(tgaFormat.getHeader()[12]);
			bufferedOutputStream.write(tgaFormat.getHeader()[13]);
//			schreibe Bildhoehe
			bufferedOutputStream.write(tgaFormat.getHeader()[14]);
			bufferedOutputStream.write(tgaFormat.getHeader()[15]);
//			schreibe BitsPerPixel
			bufferedOutputStream.write(24);
//			schreibe Kompressionstyp
			if (rleCompressionOutputFile) bufferedOutputStream.write(1);
			else if (huffmanCompressionOutputFile) bufferedOutputStream.write(2);
			else bufferedOutputStream.write(0);
//			schreibe Platzhalter fuer Datensegmentgroesse und Pruefsumme
			for (int i = 0; i < 12; i++) {
				bufferedOutputStream.write(0);
			}
//			ueberspringe den Header und beginne mit dem Lesen ab dem Datensegment
			bufferedInputStream.skip(18);
			
			byte[] inputPixel = new byte[3];
			byte[] outputPixel = new byte[3];
			byte[] outputByteCompressed = new byte[1];
			byte[] inputLine = new byte[imageWidth*3];
			byte[] outputLineCompressed;
			
//			wenn Output-Datei Huffman-kodiert werden soll: Huffman-Baum und CodeBook erstellen
			HuffmanEncoding huffmanEncoding = new HuffmanEncoding();
			if (huffmanCompressionOutputFile) {
				huffmanEncoding.createHuffmanTreeAndCodeBook(inputFile, imageWidth, imageHeight, "tga");
			}
			
			for (int line = 0; line < imageHeight; line++) {
//				Einlesen einer Bildlinie
				if (uncompressRleInputFile) {
					inputLine = Utility.uncompressInputLine(bufferedInputStream, imageWidth);
				} else {
					for (int pixel = 0; pixel < imageWidth; pixel++) {
						inputPixel = bufferedInputStream.readNBytes(3);
						for (int i = 0; i < 3; i++) {
							inputLine[pixel*3 + i] = inputPixel[i];
						}
					}
				}
//				Ausgabe der Bilddaten zeilenweise
				if (rleCompressionOutputFile) // Output-Datei soll rle kompriniert werden
				{
//					Pixelreihenfolge ändern, BGR --> GBR
					inputLine = convertLineToPropra(inputLine);
//					Linie komprimieren
					outputLineCompressed = Utility.compressOutputLine(inputLine, imageWidth);
//					Linie in Ausgabe-Datei schreiben und Pruefsumme byteweise berechnen
					for (int i = 0; i < outputLineCompressed.length; i++) {
						outputByteCompressed[0] = outputLineCompressed[i];
						calculateCheckSum(outputByteCompressed);
						bufferedOutputStream.write(outputByteCompressed);
					}
				} else if (huffmanCompressionOutputFile){ // Output-Datei soll Huffman komprimiert werden
					inputLine = convertLineToPropra(inputLine); // Pixelreihenfolge ändern, BGR --> GBR
					outputLineCompressed = huffmanEncoding.writeEncodedPixelInOutputLine(inputLine);
					calculateCheckSum(outputLineCompressed);
					bufferedOutputStream.write(outputLineCompressed);
				} else { // Output-Datei bleibt unkomprimiert 
//					konvertieren, Pruefsumme pixelweise berechnen und in OutputFile schreiben
					for (int pixel = 0; pixel < imageWidth; pixel++) {
						for (int i = 0; i < 3; i++) {
							inputPixel[i] = inputLine[pixel*3 + i];
						}
						outputPixel = convertOnePixelToPropra(inputPixel);
						calculateCheckSum(outputPixel);
						bufferedOutputStream.write(outputPixel);
					}
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
			// schreibe SizeOfDataSegment in Header
			byte[] sizeOfDataSegment = Utility.longToByteArray(sizeOfDataSegmentOutputFile);
			fileChannel.write(ByteBuffer.wrap(sizeOfDataSegment), 16); 
			// schliesse Berechnung der Pruefsumme ab und schreibe sie in den Header
			calculatedCheckSum = calculateCheckSumFinal();  
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
	
	private byte[] convertOnePixelToPropra(byte[] inputPixel) {
		byte[] temp = new byte[3];
		temp[0] = inputPixel[1];
		temp[1] = inputPixel[0];
		temp[2] = inputPixel[2];
		return temp;
	}
	
	private byte[] convertLineToPropra(byte[] inputLine) {
		for (int pixel = 0; pixel < imageWidth; pixel++) 
		{
			byte temp;
			temp = inputLine[pixel*3];
			inputLine[pixel*3] = inputLine[pixel*3 + 1];
			inputLine[pixel*3 + 1] = temp;
		}
		return inputLine;
	}

}
