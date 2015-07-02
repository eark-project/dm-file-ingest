package org.eu.eark.fileingest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

public class FileIngestService {
	
	public static void main(String[] args) throws Exception {
		
		System.out.println("Starting file ingest service.");
		List<String> paths = new ArrayList<>();
		
		String queueName = "file-ingest";
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();
		
		channel.queueDeclare(queueName, false, true, true, null);
		
		QueueingConsumer consumer = new QueueingConsumer(channel);
		channel.basicConsume(queueName, true, consumer);
		
		Timer timer = new Timer();
		TimerTask timerTask = null;
		
		while (true) {
			System.out.println("Ready for messages.");
			QueueingConsumer.Delivery delivery = consumer.nextDelivery();
			
			if (timerTask != null) {
				boolean taskHasAlreadyRun = !timerTask.cancel();
				if (taskHasAlreadyRun) paths.clear();
			}
			String message = new String(delivery.getBody());
			System.out.println("Got a message: " + message);
			paths.add(message);
			timerTask = new JobExecution(args, paths);
			timer.schedule(timerTask, inFuture(Calendar.MINUTE, 5));
		}
	}

	private static class JobExecution extends TimerTask {
		
		private String[] args;
		
		public JobExecution(String[] args, List<String> paths) {
			this.args = extendedArgs(args, paths);
		}
		
		private static String[] extendedArgs(String[] args, List<String> paths) {
			String[] extendedArgs = new String[args.length + 2];
			System.arraycopy(args, 0, extendedArgs, 0, args.length);
			String pathArg = "";
			for (String path : paths) {
				if (!pathArg.isEmpty()) pathArg += ",";
				pathArg += path;
			}
			extendedArgs[args.length] = "-i";
			extendedArgs[args.length + 1] = pathArg;
			return extendedArgs;
		}
		
		@Override
		public void run() {
			System.out.println("Starting MapReduce job.");
			try {
				ToolRunner.run(new Configuration(), new FileIngestJob(), args);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private static Date inFuture(int field, int amount) {
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(field, amount);
		return c.getTime();
	}

}
