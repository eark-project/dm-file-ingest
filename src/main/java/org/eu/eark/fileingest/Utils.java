package org.eu.eark.fileingest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class Utils {
	
	public static String relativePath(String file) throws URISyntaxException {
		URI fileURI = new URI(file);
		return fileURI.getPath().split("/", 5)[4];
	}
	
	public static Path[] getRecursivePaths(FileSystem fs, Path input) throws IOException, URISyntaxException {
		List<Path> result = new ArrayList<>();
		FileStatus[] listStatus = fs.globStatus(input);
		for (FileStatus fstat : listStatus) {
			readSubDirectory(fstat, fs, result);
		}
		return result.toArray(new Path[result.size()]);
	}

	private static void readSubDirectory(FileStatus fileStatus, FileSystem fs, List<Path> paths)
			throws IOException, URISyntaxException {
		if (!fileStatus.isDirectory()) {
			paths.add(fileStatus.getPath());
		} else {
			String subPath = fileStatus.getPath().toString();
			FileStatus[] listStatus = fs.globStatus(new Path(subPath + "/*"));
			if (listStatus.length == 0) {
				paths.add(fileStatus.getPath());
			}
			for (FileStatus fst : listStatus) {
				readSubDirectory(fst, fs, paths);
			}
		}
	}
}
