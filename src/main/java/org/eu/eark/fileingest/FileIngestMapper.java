package org.eu.eark.fileingest;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BytesWritable;
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
import org.lilyproject.repository.api.RecordNotFoundException;
import org.lilyproject.repository.api.RepositoryException;
import org.lilyproject.util.io.Closer;

import java.io.IOException;
import java.io.OutputStream;

/**
 * reads a file and stores the content in lily
 */
public class FileIngestMapper extends Mapper<Text, BytesWritable, Text, Text> {
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
	protected void map(Text key, BytesWritable value, Context context) throws IOException, InterruptedException {

		String idPath = key.toString();
		byte[] data = value.getBytes();
		
		Configuration conf = context.getConfiguration();
		try {
			String tableName = conf.get(FileIngestJob.TABLE_NAME);
			LTable table = tableName == null ?
					repository.getDefaultTable() : repository.getTable(tableName);
			RecordId id = repository.getIdGenerator().newRecordId(idPath);
			Record record = table.newRecord(id);
			record.setRecordType(q("File"));
			record.setField(q("path"), idPath);
			
			long size = value.getLength();
			record.setField(q("size"), size);
			
			String filename;
			if (idPath.contains("/")) {
				String[] pathElements = idPath.split("/");
				filename = pathElements[pathElements.length - 1];
			} else {
				filename = idPath;
			}
			
			String contentType = new Tika().detect(data, filename);
			record.setField(q("contentType"), contentType);
			
			Blob blob = new Blob(contentType, size, filename);
			try (OutputStream os = table.getOutputStream(blob)) {
				os.write(data);
			}
			record.setField(q("content"), blob);
			
			Boolean confidential = true;
			try {
				Record existingRecord = table.read(id, q("confidential"));
				confidential = existingRecord.<Boolean>getField(q("confidential"));
			} catch (RecordNotFoundException e) {}
			
			record.setField(q("confidential"), confidential);
			
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
