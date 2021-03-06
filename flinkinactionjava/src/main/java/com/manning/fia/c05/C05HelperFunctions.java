package com.manning.fia.c05;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import com.manning.fia.utils.Event;

public class C05HelperFunctions {
	public static class MyBoundedOutOfOrderedness
	      extends BoundedOutOfOrdernessTimestampExtractor<Tuple4<Integer, Integer, Integer, Long>> {

		public MyBoundedOutOfOrderedness(Time maxOutOfOrderness) {
			super(maxOutOfOrderness);
		}

		/*
		 * @Override public final Watermark getCurrentWatermark() { // this
		 * guarantees that the watermark never goes backwards. long potentialWM =
		 * currentMaxTimestamp - maxOutOfOrderness; if(potentialWM >=
		 * lastEmittedWatermark) { lastEmittedWatermark = potentialWM; } return
		 * new Watermark(lastEmittedWatermark); }
		 */
		@Override
		public long extractTimestamp(Tuple4<Integer, Integer, Integer, Long> element) {
			return element.f3;
		}
	}

	public static class BasicWaterMarkAssigner
	      implements AssignerWithPeriodicWatermarks<Tuple4<Integer, Integer, Integer, Long>> {		
		private long maxTimestamp = 0;
		@Override
		public Watermark getCurrentWatermark() {
						return new Watermark(this.maxTimestamp);
		}

		@Override
		public long extractTimestamp(Tuple4<Integer, Integer, Integer, Long> element, long previousElementTimestamp) {
			long millis = element.f3;
			maxTimestamp = Math.max(maxTimestamp, millis);
			return Long.valueOf(millis);
		}
	}

	@SuppressWarnings("serial")
	public static class BasicApplyFunction implements
	                              WindowFunction<Tuple4<Integer, Integer, Integer, Long>, 
	                                             Tuple4<Long, Long, List<Integer>, Integer>, 
	                                             Tuple, 
	                                             TimeWindow> {
		@Override
		public void apply(Tuple key, 
				            TimeWindow window, 
				            Iterable<Tuple4<Integer, Integer, Integer, Long>> inputs,
		                  Collector<Tuple4<Long, Long, List<Integer>, Integer>> out) 
		                  		throws Exception {			
			List<Integer> eventIds = new ArrayList<>(0);
			int sum = 0;
			Iterator<Tuple4<Integer, Integer, Integer, Long>> iter = inputs.iterator();
			while (iter.hasNext()) {
				Tuple4<Integer, Integer, Integer, Long> input = iter.next();
				eventIds.add(input.f1);
				sum += input.f2;
			}
			out.collect(new Tuple4<>(window.getStart(), window.getEnd(), eventIds, sum));
		}
	}
}
