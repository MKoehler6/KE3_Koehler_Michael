package propra.imageconverter;

import java.io.File;

import propra.imageconverter.autoMode.AutoMode;
import propra.imageconverter.baseN.DecodeBaseN;
import propra.imageconverter.baseN.EncodeBaseN;
import propra.imageconverter.formats.PropraFormat;
import propra.imageconverter.formats.TgaFormat;

public class ArgumentHandler {
	
	private String inputPath = null;
	private String outputPath = null;
	private String inputFormat = null;
	private String outputFormat = null;
	private String outputPathTemp = null; // temporärer Pfad zum Zwischenspeichern bei 2 Durchläufen
	private boolean encode = false;
	private boolean decode = false;
	private int typeOfEncoding;
	private int typeOfDecoding;
	private String alphabet;
	private boolean rleCompressionOutputFile = false;
	private boolean huffmanCompressionOutputFile = false;
	private boolean autoMode = false;
	private boolean baseN = false;

	/**
	 * @author Michael Koehler
	 * @throws ConverterException: zentrale Fehlerbehandlung in einer eigenen Klasse
	 * In dieser Klasse werden die Kommandozeilenparameter ausgewertet und die entsprechende 
	 * Aktion ausgelöst
	 */
	public ArgumentHandler(String[] args) throws ConverterException {

//		input-Format, input-Pfad, output-Format, outputPfad festlegen 
//		Argumente --encode --decode --compression auswerten
		try {
			for (String arg : args) { 
				System.out.println(arg);
				String[] splitted = arg.split("=");
				if (splitted[0].startsWith("--input")) {
					inputPath = splitted[1];
					if (splitted[1].toLowerCase().contains(".tga")) inputFormat = "tga";
					if (splitted[1].toLowerCase().contains(".propra")) inputFormat = "propra";
				}
				if (splitted[0].startsWith("--output")) {
					outputPath = splitted[1];
					if (splitted[1].toLowerCase().contains(".tga")) outputFormat = "tga";
					if (splitted[1].toLowerCase().contains(".propra")) outputFormat = "propra";
				}
				if (splitted[0].startsWith("--compression") && splitted[1].toLowerCase().contains("rle")) {
					rleCompressionOutputFile = true;
				}
				if (splitted[0].startsWith("--compression") && splitted[1].toLowerCase().contains("huffman")) {
					huffmanCompressionOutputFile = true;
				}
				if (splitted[0].startsWith("--compression") && splitted[1].toLowerCase().contains("auto")) {
					autoMode = true;
				}
				if (splitted[0].startsWith("--encode-base-32")) {
					encode = true;
					alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUV";
					typeOfEncoding = 32;
					outputPath = inputPath + ".base-32";
				}
				if (splitted[0].startsWith("--decode-base-32")) {
					decode = true;
					alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUV";
					typeOfDecoding = 32;
					if (inputPath.endsWith(".base-32")) {
						outputPath = inputPath.substring(0, inputPath.length()-8);
					} else throw new ConverterException("falsche Dateiendung");
				}
				if (splitted[0].startsWith("--encode-base-n")) {
					encode = true;
					baseN = true;
					alphabet = splitted[1];
					typeOfEncoding = alphabet.length();
					outputPath = inputPath + ".base-n";
				}
				if (splitted[0].startsWith("--decode-base-n")) {
					decode = true;
					alphabet = "ausErsterZeile"; // aus der ersten Zeile der Datei auslesen
					typeOfDecoding = 0;
					if (inputPath.endsWith(".base-n")) {
						outputPath = inputPath.substring(0, inputPath.length()-7);
					} else throw new ConverterException("falsche Dateiendung");
				}
			}
			
			if (inputFormat == null || inputPath == null) 
				throw new ConverterException("Input-Parameter ungültig");
			
		} catch (Exception e) {
			throw new ConverterException("Argumente ungültig");
		}

	}
	
	public void startAction() throws ConverterException {
		
		if (autoMode) {
			new AutoMode(inputPath, outputPath, inputFormat, outputFormat);
		}
		else if (encode) {
			new EncodeBaseN(inputPath, outputPath, typeOfEncoding, alphabet, baseN);
		}
		else if (decode) {
			new DecodeBaseN(inputPath, outputPath, typeOfDecoding, alphabet);
		}
		else if (inputFormat == "tga" && outputFormat == "tga") {
			new CopyCompressDecompressTgaFile(inputPath, outputPath, rleCompressionOutputFile);
		}
		else if (inputFormat == "propra" && outputFormat == "propra") {
//			wenn input-Datei rle komprimiert ist und output soll huffman-kodiert werden, 
//			dann sind 2 Durchläufe nötig, erst rle-dekomprimieren, 
//			dann im zweiten Durchlauf huffman-kodieren
			PropraFormat propraFormat = new PropraFormat(inputPath);
			if (propraFormat.getTypeOfCompression() == 1 && huffmanCompressionOutputFile) {
				outputPathTemp = outputPath.substring(0, outputPath.length()-7) + "_temp.propra";
				huffmanCompressionOutputFile = false;
//				erster Durchlauf
				new CopyCompressDecompressPropraFile(inputPath, outputPathTemp, rleCompressionOutputFile, huffmanCompressionOutputFile);
				inputPath = outputPathTemp;
				huffmanCompressionOutputFile = true;
			}
			new CopyCompressDecompressPropraFile(inputPath, outputPath, rleCompressionOutputFile, huffmanCompressionOutputFile);
		}
		else if (inputFormat == "tga" && outputFormat == "propra") {
//			wenn input-Datei rle komprimiert ist und output soll huffman-kodiert werden, 
//			dann sind 2 Durchläufe nötig, erst rle-dekomprimieren, 
//			dann im zweiten Durchlauf huffman-kodieren
			TgaFormat tgaFormat = new TgaFormat(inputPath);
			if (tgaFormat.getImageType() == 10 && huffmanCompressionOutputFile) {
				outputPathTemp = outputPath.substring(0, outputPath.length()-7) + "_temp.tga";
//				erster Durchlauf
				new CopyCompressDecompressTgaFile(inputPath, outputPathTemp, rleCompressionOutputFile);
				inputPath = outputPathTemp;
			}
			new ConverterTgaToPropra(inputPath, outputPath, rleCompressionOutputFile, huffmanCompressionOutputFile);
		}
		else if (inputFormat == "propra" && outputFormat == "tga") {
			new ConverterPropraToTga(inputPath, outputPath, rleCompressionOutputFile);
		}
		else {
			throw new ConverterException("Argumente ungültig");
		}
//		Utility.md5(new File(outputPath));
	}
}
