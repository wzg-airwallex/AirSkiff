package com.airwallex.airskiff.examples;

import com.airwallex.airskiff.core.SourceStream;
import com.airwallex.airskiff.core.api.Stream;
import com.airwallex.airskiff.flink.FlinkLocalTextConfig;
import com.airwallex.airskiff.flink.FlinkRealtimeCompiler;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

public class LocalRealtimeSQLWordCountJob {
  public static void main(String[] args) throws Exception {
    StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironment();
    StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);
    env.setParallelism(1);

    Path inputPath = Paths.get("doesn't matter");
    // default host and port is localhost:10000 for socket stream
    FlinkLocalTextConfig config = new FlinkLocalTextConfig(inputPath.toAbsolutePath().toString());
    Stream<Counter> stream = new SourceStream<>(config).flatMap(x -> Arrays.asList(x.split("\\s")), String.class)
      .map(x -> new Counter(x, 1L), Counter.class)
      .sql(
        "SELECT key, COUNT(*) OVER (PARTITION BY key ORDER BY row_time__ RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) FROM text",
        "text",
        Counter.class
      );

    new FlinkRealtimeCompiler(env, tableEnv).compile(stream).print();
    env.execute();
  }
}
