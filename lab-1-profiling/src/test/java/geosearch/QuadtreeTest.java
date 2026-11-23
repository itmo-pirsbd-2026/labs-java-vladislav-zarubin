package geosearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class QuadtreeTest {
    @ParameterizedTest
    @EnumSource(Quadtree.Variant.class)
    void emptyTreeAndInvalidInsert(Quadtree.Variant variant) {
        Quadtree tree = new Quadtree(variant);

        assertThatIllegalArgumentException().isThrownBy(() -> tree.insert(new GeoPoint(1, 91, 0)));
        assertThatIllegalArgumentException().isThrownBy(() -> tree.insert(new GeoPoint(2, 0, Double.NaN)));
        assertThat(tree.size()).isZero();
        assertThat(tree.searchInBox(-1, -1, 1, 1)).isEmpty();
        assertThat(tree.searchInRadius(0, 0, 1)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(Quadtree.Variant.class)
    void searchesInBoxAndOnCircleBoundary(Quadtree.Variant variant) {
        Quadtree tree = new Quadtree(variant);
        GeoPoint center = new GeoPoint(1, 0, 0);
        GeoPoint onCircle = new GeoPoint(2, 3, 4);
        GeoPoint outsideCircle = new GeoPoint(3, 5, 5);
        for (GeoPoint point : List.of(center, onCircle, outsideCircle)) {
            tree.insert(point);
        }

        assertThat(tree.size()).isEqualTo(3);
        assertThat(tree.searchInBox(-5, -5, 5, 5))
                .containsExactlyInAnyOrder(center, onCircle, outsideCircle);
        assertThat(tree.searchInRadius(0, 0, 5)).containsExactlyInAnyOrder(center, onCircle);
    }

    @ParameterizedTest
    @EnumSource(Quadtree.Variant.class)
    void findsWorldBordersAndSplitLines(Quadtree.Variant variant) {
        Quadtree tree = new Quadtree(variant);
        List<GeoPoint> points = new ArrayList<>();
        long id = 0;
        for (double lat : new double[]{-90, 0, 90}) {
            for (double lng : new double[]{-180, 0, 180}) {
                GeoPoint point = new GeoPoint(id++, lat, lng);
                points.add(point);
                tree.insert(point);
            }
        }

        assertThat(tree.size()).isEqualTo(points.size());
        for (GeoPoint point : points) {
            assertThat(tree.searchInRadius(point.lat, point.lng, 0)).containsExactly(point);
        }
    }

    @ParameterizedTest
    @EnumSource(Quadtree.Variant.class)
    void duplicateCoordinatesDoNotBreakTree(Quadtree.Variant variant) {
        Quadtree tree = new Quadtree(variant);
        for (int i = 0; i < 20; i++) {
            tree.insert(new GeoPoint(i, 10, 10));
        }

        assertThat(tree.size()).isEqualTo(20);
        assertThat(tree.searchInRadius(10, 10, 0)).hasSize(20);
    }

    @ParameterizedTest
    @EnumSource(Quadtree.Variant.class)
    void randomizedSearchMatchesNaive(Quadtree.Variant variant) {
        Quadtree tree = new Quadtree(variant);
        NaiveGeoIndex naive = new NaiveGeoIndex();
        for (GeoPoint point : RandomDataGenerator.generatePoints(2_000, 123L)) {
            tree.insert(point);
            naive.insert(point);
        }

        Random random = new Random(789L);
        for (GeoPoint query : RandomDataGenerator.generatePoints(200, 456L)) {
            double radius = 0.1 + random.nextDouble() * 15;
            assertThat(tree.searchInRadius(query.lat, query.lng, radius))
                    .containsExactlyInAnyOrderElementsOf(naive.searchInRadius(query.lat, query.lng, radius));
        }
    }
}
