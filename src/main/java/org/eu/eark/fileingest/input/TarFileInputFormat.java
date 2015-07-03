package org.eu.eark.fileingest.input;

import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.eu.eark.fileingest.FileIngestJob;

public class TarFileInputFormat extends FileInputFormat<Text, BytesWritable> {

	@Override
	protected boolean isSplitable(JobContext context, Path filename) {
		return false;
	}

	@Override
	public RecordReader<Text, BytesWritable> createRecordReader(InputSplit split, TaskAttemptContext context)
			throws IOException, InterruptedException {
		context.setStatus(split.toString());
		
		String concatenatedNameFilter = context.getConfiguration().get(FileIngestJob.FILENAME_FILTER);
		if (concatenatedNameFilter != null)
			return new TarRecordReader(Arrays.asList(concatenatedNameFilter.split(";")));
		else
			return new TarRecordReader();
	}

}
