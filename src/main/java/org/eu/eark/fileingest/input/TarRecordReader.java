package org.eu.eark.fileingest.input;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

public class TarRecordReader extends RecordReader<Text, BytesWritable> {
	
	private TarArchiveInputStream in;
	private FSDataInputStream fileIn;
	private Text key;
	private BytesWritable value;
	private long pos;
	private long end;
	private List<String> nameFilter;
	
	public TarRecordReader() {
		nameFilter = new ArrayList<String>();
	}
	
	public TarRecordReader(List<String> nameFilter) {
		this.nameFilter = nameFilter;
	}

	@Override
	public void initialize(InputSplit genericSplit, TaskAttemptContext context) throws IOException, InterruptedException {
		FileSplit split = (FileSplit) genericSplit;
		Configuration job = context.getConfiguration();
		final Path file = split.getPath();
		final FileSystem fs = file.getFileSystem(job);
		fileIn = fs.open(file);
		in = new TarArchiveInputStream(new BufferedInputStream(fileIn));
		key = new Text();
		pos = 0;
		end = fs.getFileStatus(file).getLen();
	}

	@Override
	public void close() throws IOException {
		if (in != null) {
			in.close();
		}
	}

	@Override
	public Text getCurrentKey() throws IOException, InterruptedException {
		return key;
	}

	@Override
	public BytesWritable getCurrentValue() throws IOException, InterruptedException {
		return value;
	}

	@Override
	public float getProgress() throws IOException, InterruptedException {
		return (float)pos / end;
	}

	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException {
		TarArchiveEntry entry;
		do {
			entry = in.getNextTarEntry();
			if (entry == null)
				return false;
		} while (!entry.isFile() || filterApplies(entry.getName()));
		key.set(entry.getName());
		byte[] b = new byte[(int) entry.getSize()];
		in.read(b);
		value = new BytesWritable(b);
		pos += entry.getSize();
		return true;
	}

	private boolean filterApplies(String name) {
		for (String filterEntry : nameFilter)
			if (name.matches(filterEntry))
				return true;
		return false;
	}

}
