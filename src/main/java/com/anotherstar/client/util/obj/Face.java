package com.anotherstar.client.util.obj;

/**
 * OBJ 面。原实现的 render(Tessellator) 依赖 1.12.2 的顶点缓冲与固定管线，
 * 1.20.1 已无对应 API，这里只保留解析后的几何数据与法线计算。
 */
public class Face {

	public static int defaultColor = 0xFFFFFFFF;

	public static void setColor(int color) {
		defaultColor = color;
	}

	public static void resetColor() {
		defaultColor = 0xFFFFFFFF;
	}

	public Vertex[] vertices;
	public Vertex[] vertexNormals;
	public Vertex faceNormal;
	public TextureCoordinate[] textureCoordinates;
	public int glDrawingMode;
	public String usemtl;

	public Vertex calculateFaceNormal() {
		Vertex v0 = vertices[0];
		Vertex v1 = vertices[1];
		Vertex v2 = vertices[2];
		float ax = v1.x - v0.x;
		float ay = v1.y - v0.y;
		float az = v1.z - v0.z;
		float bx = v2.x - v0.x;
		float by = v2.y - v0.y;
		float bz = v2.z - v0.z;
		float nx = ay * bz - az * by;
		float ny = az * bx - ax * bz;
		float nz = ax * by - ay * bx;
		float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (length > 0.0F) {
			nx /= length;
			ny /= length;
			nz /= length;
		}
		return new Vertex(nx, ny, nz);
	}

}
