package com.manning.fia.c06;

import java.util.HashMap;
import java.util.Map;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.streaming.api.datastream.ConnectedStreams;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.datastream.SplitStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.CoFlatMapFunction;
import org.apache.flink.streaming.api.functions.co.CoMapFunction;
import org.apache.flink.util.Collector;
import org.joda.time.format.DateTimeFormat;

public class ConnectedStreamsWithBroadcast {

	public static void main(String[] args) throws Exception{
      StreamExecutionEnvironment execEnv =
            StreamExecutionEnvironment.getExecutionEnvironment();
      execEnv.setParallelism(5);      
      DataStream<Tuple2<String,Integer>> rulesSource = execEnv.addSource(new RulesSource());      
      DataStream<Tuple2<String,Integer>> eventSource = execEnv.addSource(new ContinousEventsSource());
      ConnectedStreams<Tuple2<String, Integer>, Tuple2<String, Integer>> connectedStream = 
      		eventSource.connect(rulesSource.broadcast());      
      DataStream<Tuple3<String, Integer, String>> resultStream = connectedStream.flatMap(new RulesAndEventsCoFlatMapFunction());
      SplitStream<Tuple3<String, Integer, String>> splitStream = resultStream.split(new MyOutputSelector());
      DataStream<Tuple3<String, Integer, String> > normal = splitStream.select("NORMAL");
      DataStream<Tuple3<String, Integer, String> > alerts = splitStream.select("ALERT");
      DataStream<Tuple3<String, Integer, String> > normalButNoRules = splitStream.select("NORULE");
      alerts.printToErr();
      normal.print();
      normalButNoRules.print();
		execEnv.execute();
	}
}
