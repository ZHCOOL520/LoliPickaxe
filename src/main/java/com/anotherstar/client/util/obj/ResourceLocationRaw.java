package com.anotherstar.client.util.obj;

import net.minecraft.resources.ResourceLocation;

/**
 * 保留原路径写法的 ResourceLocation（原版 ResourceLocation 在 1.20.1 中不再允许
 * 通过重写 getResourcePath 改变行为，这里改为单独暴露原始路径）。
 */
public class ResourceLocationRaw extends ResourceLocation {

	private final String namespaceRaw;
	private final String pathRaw;

	public ResourceLocationRaw(String resourceDomainIn, String resourcePathIn) {
		super(resourceDomainIn, resourcePathIn);
		this.namespaceRaw = resourceDomainIn;
		this.pathRaw = resourcePathIn;
	}

	public ResourceLocationRaw(String[] resourceName) {
		this(resourceName[0], resourceName[1]);
	}

	public ResourceLocationRaw(String resourceName) {
		this(decomposeResourceName(resourceName));
	}

	public String getRawNamespace() {
		return this.namespaceRaw;
	}

	public String getRawPath() {
		return this.pathRaw;
	}

	@Override
	public String toString() {
		return this.namespaceRaw + ':' + this.pathRaw;
	}

	private static String[] decomposeResourceName(String resourceName) {
		int index = resourceName.indexOf(ResourceLocation.NAMESPACE_SEPARATOR);
		if (index >= 0) {
			return new String[] { resourceName.substring(0, index), resourceName.substring(index + 1) };
		}
		return new String[] { ResourceLocation.DEFAULT_NAMESPACE, resourceName };
	}

}
