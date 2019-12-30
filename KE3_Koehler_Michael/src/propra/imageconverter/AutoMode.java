package propra.imageconverter;

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
		deleteLargeFiles();
		renameSmallestFile();
//		TODO noch entfernen zusammen mit MD5 in Utility
		System.exit(0);
	}

	private void action() throws ConverterException {
		String[] args1 = {"--input=" + inputPath, getOutputPathTemp(outputFormat, "uncompressed"), "--compression=uncompressed"};
		ImageConverter.startWithoutMain(args1);
		String[] args2 = {"--input=" + inputPath, getOutputPathTemp(outputFormat, "rle"), "--compression=rle"};
		ImageConverter.startWithoutMain(args2);
		String[] args3 = {"--input=" + inputPath, getOutputPathTemp(outputFormat, "huffman"), "--compression=huffman"};
		ImageConverter.startWithoutMain(args3);
	}
	
	private String getOutputPathTemp(String outputFormat, String compression) throws ConverterException {
		String outputPathTemp;
		if (outputFormat == "tga") {
			outputPathTemp = "--output=" + outputPath.substring(0, outputPath.length()-4) + "_" + compression + "_temp.tga";
		} else if (outputFormat == "propra") {
			outputPathTemp = "--output=" + outputPath.substring(0, outputPath.length()-7) + "_" + compression + "_temp.propra";
		} else {
			throw new ConverterException("Ausgabeformat ungültig");
		}
		return outputPathTemp;
	}

	private void deleteLargeFiles() {
		// TODO Auto-generated method stub
		
	}
	
	private void renameSmallestFile() {
		// TODO Auto-generated method stub
		
	}
	
}
