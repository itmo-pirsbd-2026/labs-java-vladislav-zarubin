package geosearch.benchmark;

import geosearch.GeoPoint;
import geosearch.Quadtree;
import geosearch.RandomDataGenerator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(2)
@Threads(1)
public class GeoIndexBenchmark {
    @State(Scope.Thread)
    public static class BuildState {
        @Param({"10000", "100000", "300000"})
        public int size;

        @Param({"BASELINE", "CPU"})
        public Quadtree.Variant variant;

        List<GeoPoint> points;

        @Setup(Level.Trial)
        public void setup() {
            points = RandomDataGenerator.generatePoints(size, 1000L);
        }
    }

    @State(Scope.Thread)
    public static class SearchState {
        @Param({"10000", "100000", "300000"})
        public int size;

        @Param({"0.1", "1.0", "10.0"})
        public double radius;

        @Param({"BASELINE", "MEMORY"})
        public Quadtree.Variant variant;

        Quadtree quadtree;
        List<GeoPoint> queries;
        int nextQuery;

        @Setup(Level.Trial)
        public void setup() {
            quadtree = new Quadtree(variant);
            for (GeoPoint point : RandomDataGenerator.generatePoints(size, 1000L)) {
                quadtree.insert(point);
            }
            queries = RandomDataGenerator.generatePoints(500, 3000L);
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public Quadtree buildQuadtree(BuildState state) {
        Quadtree quadtree = new Quadtree(state.variant);
        for (GeoPoint point : state.points) {
            quadtree.insert(point);
        }
        return quadtree;
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public List<GeoPoint> searchInRadius(SearchState state) {
        GeoPoint query = state.queries.get(state.nextQuery);
        state.nextQuery++;
        if (state.nextQuery == state.queries.size()) {
            state.nextQuery = 0;
        }
        return state.quadtree.searchInRadius(query.lat, query.lng, state.radius);
    }
}
