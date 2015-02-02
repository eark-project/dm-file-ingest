package org.eu.eark.fileingest;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.tika.Tika;
import org.lilyproject.client.LilyClient;
import org.lilyproject.indexer.Indexer;
import org.lilyproject.mapreduce.LilyMapReduceUtil;
import org.lilyproject.repository.api.Blob;
import org.lilyproject.repository.api.LRepository;
import org.lilyproject.repository.api.LTable;
import org.lilyproject.repository.api.QName;
import org.lilyproject.repository.api.Record;
import org.lilyproject.repository.api.RecordId;
import org.lilyproject.repository.api.RepositoryException;
import org.lilyproject.util.io.Closer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/**
 * reads a file and stores the content in lily
 */
public class FileIngestMapper extends Mapper<Text, Text, Text, Text> {
	private LilyClient lilyClient;
	private LRepository repository;

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		super.setup(context);
		this.lilyClient = LilyMapReduceUtil.getLilyClient(context.getConfiguration());
		try {
			this.repository = lilyClient.getDefaultRepository();
		} catch (RepositoryException e) {
			throw new RuntimeException("Failed to get repository", e);
		}
	}

	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
		Closer.close(lilyClient);
		super.cleanup(context);
	}
	
	@Override
	protected void map(Text key, Text value, Context context) throws IOException, InterruptedException {

		String pathString = key.toString();
		
		Path path = new Path(pathString);
		try (FileSystem fileSystem = FileSystem.get(new URI(pathString), context.getConfiguration())) {
			
			LTable table = repository.getDefaultTable();
			String idPath = Utils.relativePath(pathString);
			RecordId id = repository.getIdGenerator().newRecordId(idPath);
			Record record = table.newRecord(id);
			record.setRecordType(q("File"));
			record.setField(q("path"), idPath);
			
			long size = fileSystem.getFileStatus(path).getLen();
			record.setField(q("size"), size);
			
			String filename;
			if (idPath.contains("/")) {
				String[] pathElements = idPath.split("/");
				filename = pathElements[pathElements.length - 1];
			} else {
				filename = idPath;
			}
			
			String contentType = "application/octet-stream";
			try (FSDataInputStream fis = fileSystem.open(path)) {
				contentType = new Tika().detect(fis, filename);
			}
			record.setField(q("contentType"), contentType);
			
			try (FSDataInputStream fis = fileSystem.open(path)) {
				Blob blob = new Blob(contentType, size, filename);
				try (OutputStream os = table.getOutputStream(blob)) {
					IOUtils.copyBytes(fis, os, 32 * 1024);
				}
				record.setField(q("content"), blob);
			}
			
			table.createOrUpdate(record);
			Indexer indexer = lilyClient.getIndexer();
			indexer.index(table.getTableName(), record.getId());
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static QName q(String name) {
		return new QName("org.eu.eark", name);
	}
}
