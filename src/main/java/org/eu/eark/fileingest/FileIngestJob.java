package org.eu.eark.fileingest;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.eu.eark.fileingest.input.TarFileInputFormat;
import org.lilyproject.client.LilyClient;
import org.lilyproject.mapreduce.LilyMapReduceUtil;
import org.lilyproject.repository.api.TableManager;

import java.io.IOException;

/**
 * Sets up and launches a Lily-based MapReduce job.
 */
public class FileIngestJob extends Configured implements Tool {
	private String zkConnectString;
	public static final String TABLE_NAME = "tableName";
	private String tableName;
	
	public static void main(String[] args) throws Exception {
		// Let <code>ToolRunner</code> handle generic command-line options
		int res = ToolRunner.run(new Configuration(), new FileIngestJob(), args);
		System.exit(res);
	}

	@Override
	public int run(String[] args) throws Exception {
		int result = parseArgs(args);
		if (result != 0) {
			return result;
		}

		Configuration config = getConf();

		Job job = new Job(config, "FileIngestJob");
		job.setJarByClass(FileIngestJob.class);

		job.setMapperClass(FileIngestMapper.class);
		job.setNumReduceTasks(0);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);

		job.setInputFormatClass(TarFileInputFormat.class);
		// The mapper writes directly to Lily, so for Hadoop there is no output to produce
		job.setOutputFormatClass(NullOutputFormat.class);

		job.getConfiguration().set(LilyMapReduceUtil.ZK_CONNECT_STRING, zkConnectString);
		if (tableName != null) {
			try (LilyClient lilyClient = new LilyClient(zkConnectString, 30000)) {
				TableManager tableManager = lilyClient.getDefaultRepository().getTableManager();
				if (!tableManager.tableExists(tableName))
					tableManager.createTable(tableName);			
				job.getConfiguration().set(TABLE_NAME, tableName);
			}
		}
		//Path[] inputPaths = Utils.getRecursivePaths(FileSystem.get(config), new Path("files"));
		//FileInputFormat.setInputPaths(job, inputPaths);
		FileInputFormat.addInputPath(job, new Path("files"));

		// Launch the job
		boolean b = job.waitForCompletion(true);
		if (!b) {
			throw new IOException("error executing job!");
		}

		return 0;
	}

	@SuppressWarnings("static-access")
	protected int parseArgs(String[] args) {
		Options cliOptions = new Options();

		Option tableOption = OptionBuilder.isRequired(false).withArgName("table name").hasArg()
				.withDescription("Lily table name")
				.withLongOpt("table").create("t");
		cliOptions.addOption(tableOption);
		
		Option zkOption = OptionBuilder.isRequired().withArgName("connection-string").hasArg()
				.withDescription("ZooKeeper connection string: hostname1:port,hostname2:port,...")
				.withLongOpt("zookeeper").create("z");
		cliOptions.addOption(zkOption);

		CommandLineParser parser = new PosixParser();
		CommandLine cmd;
		try {
			cmd = parser.parse(cliOptions, args);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			System.out.println();

			HelpFormatter help = new HelpFormatter();
			help.printHelp(getClass().getSimpleName(), cliOptions, true);
			return 1;
		}

		tableName = cmd.getOptionValue(tableOption.getOpt());
		zkConnectString = cmd.getOptionValue(zkOption.getOpt());

		return 0;
	}
}
