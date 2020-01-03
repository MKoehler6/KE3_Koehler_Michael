package propra.imageconverter.autoMode;

import java.io.File;
import java.util.ArrayList;

import propra.imageconverter.ConverterException;
import propra.imageconverter.ImageConverter;

/**
 * Eine Instanz dieser Klasse übernimmt den Auto-Modus, konvertiert also mit derjenigen 
 * Kompression, die die kleinste Datei erzeugt
 * 
 * @author Michael Koehler
 */
public class AutoMode {
	
	String inputPath;
	String outputPath;
	String inputFormat;
	String outputFormat;
//	ArrayList mit den Pfaden aller temporären Output-Dateien und zwar für uncompresed, rle und huffman (nur propra)
	ArrayList<String> outputPathArrayListOfAllOutputFiles = new ArrayList<>();
	
	public AutoMode (String inputPath, String outputPath, String inputFormat, String outputFormat) throws ConverterException {
		this.inputPath = inputPath;
		this.outputPath = outputPath;
		this.outputFormat = outputFormat;
		this.inputFormat = inputFormat;
		
		action();
		deleteLargerFilesAndRenameSmallestFile();
	}

	/**
	 * ruft nacheinander das Programm auf mit den unterschiedlichen Kompressionen für die Output-Datei
	 */
	private void action() throws ConverterException {
		String[] args1 = {"--input=" + inputPath, getOutputPathTemp(outputFormat, "uncompressed"), "--compression=uncompressed"};
		ImageConverter.startWithoutMain(args1);
		String[] args2 = {"--input=" + inputPath, getOutputPathTemp(outputFormat, "rle"), "--compression=rle"};
		ImageConverter.startWithoutMain(args2);
		if (outputFormat == "propra") {
			String[] args3 = {"--input=" + inputPath, getOutputPathTemp(outputFormat, "huffman"), "--compression=huffman"};
			ImageConverter.startWithoutMain(args3);
		}
	}
	
	/**
	 * erstellt den Output-Pfad für die temporären Output-Dateien
	 */
	private String getOutputPathTemp(String outputFormat, String compression) throws ConverterException {
		String outputPathTemp;
		if (outputFormat == "tga") {
			outputPathTemp = outputPath.substring(0, outputPath.length()-4) + "_" + compression + "_temp.tga";
		} else if (outputFormat == "propra") {
			outputPathTemp = outputPath.substring(0, outputPath.length()-7) + "_" + compression + "_temp.propra";
		} else {
			throw new ConverterException("Ausgabeformat ungültig");
		}
//		speichern des Output-Pfades in der ArrayList
		outputPathArrayListOfAllOutputFiles.add(outputPathTemp);
		return "--output=" + outputPathTemp;
	}

	/**
	 * löscht die größeren Dateien und behält die kleinste Datei, die dann noch umbenannt wird in den
	 * ursprünglichen Output-Dateinamen
	 */
	private void deleteLargerFilesAndRenameSmallestFile() {
//		die kleinste Datei wird ermittelt
		String outputPathMinimalSize = outputPathArrayListOfAllOutputFiles.get(0);
		for (int i = 1; i < outputPathArrayListOfAllOutputFiles.size(); i++) {
			if (new File(outputPathMinimalSize).length() > new File(outputPathArrayListOfAllOutputFiles.get(i)).length()) {
				outputPathMinimalSize = outputPathArrayListOfAllOutputFiles.get(i);
			}
		}
//		löschen der größeren Dateien
		for (String path : outputPathArrayListOfAllOutputFiles) {
			if (!path.equals(outputPathMinimalSize)) {
				new File(path).delete();
			}
		}
		new File(outputPathMinimalSize).renameTo(new File(outputPath));
	}
}
