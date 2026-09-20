package com.anotherstar.client.util.obj;

import java.util.ArrayList;

/**
 * OBJ 中的分组（g/o）。旋转参数对应原实现自定义的 {@code rc x y z} 指令，
 * 用于在绘制时围绕 pivot 旋转，1.20.1 中只保留数据。
 */
public class GroupObject {

	public String name;
	public ArrayList<Face> faces = new ArrayList<Face>();
	public float rotationPointX;
	public float rotationPointY;
	public float rotationPointZ;
	public float rotateAngleX;
	public float rotateAngleY;
	public float rotateAngleZ;

	public GroupObject() {
		this("");
	}

	public GroupObject(String name) {
		this.name = name;
	}

	public void setRotationPoint(float rotationPointXIn, float rotationPointYIn, float rotationPointZIn) {
		rotationPointX = rotationPointXIn;
		rotationPointY = rotationPointYIn;
		rotationPointZ = rotationPointZIn;
	}

}
