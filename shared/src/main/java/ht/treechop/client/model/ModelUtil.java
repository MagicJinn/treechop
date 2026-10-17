package ht.treechop.client.model;

import ht.tuber.math.Vector3;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.joml.Vector3f;

public class ModelUtil {
    /**
     * Clamps quad vertices into the given AABB (in 0–16 block-pixel space) and
     * re-lerps UVs so the texture shrinks with the geometry.
     */
    public static BakedQuad trimQuad(BakedQuad quad, Vector3 corner1, Vector3 corner2) {
        Vector3 mins = new Vector3(
                Math.min(corner1.x(), corner2.x()),
                Math.min(corner1.y(), corner2.y()),
                Math.min(corner1.z(), corner2.z())
        );
        Vector3 maxes = new Vector3(
                Math.max(corner1.x(), corner2.x()),
                Math.max(corner1.y(), corner2.y()),
                Math.max(corner1.z(), corner2.z())
        );

        Vertex oldV1 = getVertex(quad, 0);
        Vertex oldV2 = getVertex(quad, 1);
        Vertex oldV3 = getVertex(quad, 2);
        Vertex oldV4 = getVertex(quad, 3);

        Vertex newV1 = lerpVertexUVsInTriangle(oldV1.xyz().clamp(mins, maxes), oldV1, oldV2, oldV3);
        Vertex newV2 = lerpVertexUVsInTriangle(oldV2.xyz().clamp(mins, maxes), oldV1, oldV2, oldV3);
        Vertex newV3 = lerpVertexUVsInTriangle(oldV3.xyz().clamp(mins, maxes), oldV1, oldV2, oldV3);
        Vertex newV4 = lerpVertexUVsInTriangle(oldV4.xyz().clamp(mins, maxes), oldV1, oldV2, oldV3);

        return makeQuad(quad, newV1, newV2, newV3, newV4);
    }

    public static BakedQuad translateQuad(BakedQuad quad, Vector3 translation) {
        Vertex v1 = getVertex(quad, 0).translate(translation);
        Vertex v2 = getVertex(quad, 1).translate(translation);
        Vertex v3 = getVertex(quad, 2).translate(translation);
        Vertex v4 = getVertex(quad, 3).translate(translation);
        return makeQuad(quad, v1, v2, v3, v4);
    }

    private static BakedQuad makeQuad(BakedQuad template, Vertex v1, Vertex v2, Vertex v3, Vertex v4) {
        return new BakedQuad(
                toBlockSpace(v1),
                toBlockSpace(v2),
                toBlockSpace(v3),
                toBlockSpace(v4),
                UVPair.pack((float) v1.u, (float) v1.v),
                UVPair.pack((float) v2.u, (float) v2.v),
                UVPair.pack((float) v3.u, (float) v3.v),
                UVPair.pack((float) v4.u, (float) v4.v),
                template.direction(),
                template.materialInfo()
        );
    }

    private static Vector3f toBlockSpace(Vertex vertex) {
        return new Vector3f(
                (float) (vertex.x / 16.0),
                (float) (vertex.y / 16.0),
                (float) (vertex.z / 16.0)
        );
    }

    private static Vertex lerpVertexUVsInTriangle(Vector3 pos, Vertex v1, Vertex v2, Vertex v3) {
        Vector3 weights = getLerpWeightsInSimplex(pos, v1.xyz(), v2.xyz(), v3.xyz());
        double u = weights.dot(v1.u, v2.u, v3.u);
        double v = weights.dot(v1.v, v2.v, v3.v);
        return new Vertex(pos.x, pos.y, pos.z, u, v);
    }

    private static Vector3 getLerpWeightsInSimplex(Vector3 pos, Vector3 v1, Vector3 v2, Vector3 v3) {
        Vector3 side1 = v2.subtract(v1);
        Vector3 side2 = v3.subtract(v1);
        Vector3 normal = side2.cross(side1).normalize();

        double offset = pos.subtract(v1).dot(normal);
        Vector3 p = pos.subtract(normal.scale(offset));

        Vector3 f1 = v1.subtract(p);
        Vector3 f2 = v2.subtract(p);
        Vector3 f3 = v3.subtract(p);

        Vector3 va = side1.cross(side2);
        Vector3 va1 = f2.cross(f3);
        Vector3 va2 = f3.cross(f1);
        Vector3 va3 = f1.cross(f2);

        double a = va.length();
        double w1 = va1.length() / a * Math.signum(va.dot(va1));
        double w2 = va2.length() / a * Math.signum(va.dot(va2));
        double w3 = va3.length() / a * Math.signum(va.dot(va3));

        return new Vector3(w1, w2, w3);
    }

    private static Vertex getVertex(BakedQuad quad, int index) {
        var pos = quad.position(index);
        long packedUV = quad.packedUV(index);
        return new Vertex(
                pos.x() * 16.0,
                pos.y() * 16.0,
                pos.z() * 16.0,
                UVPair.unpackU(packedUV),
                UVPair.unpackV(packedUV)
        );
    }

    private record Vertex(double x, double y, double z, double u, double v) {
        public Vector3 xyz() {
            return new Vector3(x, y, z);
        }

        public Vertex translate(Vector3 translation) {
            return new Vertex(x + translation.x, y + translation.y, z + translation.z, u, v);
        }
    }
}
