package propra.imageconverter;

import java.io.File;
import java.util.ArrayList;

public class AutoMode {
	
	String inputPath;
	String outputPath;
	String inputFormat;
	String outputFormat;
	ArrayList<String> outputPathArrayListOfAllOutputFiles = new ArrayList<>();
	
	public AutoMode (String inputPath, String outputPath, String inputFormat, String outputFormat) throws ConverterException {
		this.inputPath = inputPath;
		this.outputPath = outputPath;
		this.outputFormat = outputFormat;
		this.inputFormat = inputFormat;
		
		action();
		deleteLargerFilesAndRenameSmallestFile();
	}

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
	
	private String getOutputPathTemp(String outputFormat, String compression) throws ConverterException {
		String outputPathTemp;
		if (outputFormat == "tga") {
			outputPathTemp = outputPath.substring(0, outputPath.length()-4) + "_" + compression + "_temp.tga";
		} else if (outputFormat == "propra") {
			outputPathTemp = outputPath.substring(0, outputPath.length()-7) + "_" + compression + "_temp.propra";
		} else {
			throw new ConverterException("Ausgabeformat ungültig");
		}
		outputPathArrayListOfAllOutputFiles.add(outputPathTemp);
		System.out.println(new File(outputPathTemp));
		System.out.println();
		return "--output=" + outputPathTemp;
	}

	private void deleteLargerFilesAndRenameSmallestFile() {
		String outputPathMinimalSize = outputPathArrayListOfAllOutputFiles.get(0);
		for (int i = 1; i < outputPathArrayListOfAllOutputFiles.size(); i++) {
			if (new File(outputPathMinimalSize).length() > new File(outputPathArrayListOfAllOutputFiles.get(i)).length()) {
				outputPathMinimalSize = outputPathArrayListOfAllOutputFiles.get(i);
			}
		}
		System.out.println(outputPathMinimalSize);
		for (String path : outputPathArrayListOfAllOutputFiles) {
			System.out.println(new File(path).length());
			if (!path.equals(outputPathMinimalSize)) {
				new File(path).delete();
			}
		}
		new File(outputPathMinimalSize).renameTo(new File(outputPath));
	}
}
