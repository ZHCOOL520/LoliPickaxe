package com.anotherstar.client.gui;

import java.net.URI;
import java.net.URISyntaxException;

import com.anotherstar.client.util.LoliCardOnlineUtil;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class GUILoliCardOnline extends Screen {

	private final String url;

	private int dcx;
	private int dcy;
	private double ds;
	private boolean clicked;
	private boolean moved;
	private int clickX;
	private int clickY;
	private int odcx;
	private int odcy;
	private URI clickedLinkURI;

	public GUILoliCardOnline(String url) {
		super(Component.empty());
		this.url = url;
		this.dcx = 0;
		this.dcy = 0;
		this.ds = 1;
		this.clicked = false;
		this.clickX = 0;
		this.clickY = 0;
	}

	@Override
	protected void init() {
		this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose()).bounds((this.width - 200) / 2, this.height - 20, 200, 20).build());
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (delta == 0) {
			return super.mouseScrolled(mouseX, mouseY, delta);
		}
		if (ds > 0.1 || delta > 0) {
			ds *= delta > 0 ? 1.28 : 0.78125;
			dcx += delta > 0 ? (this.width / 2 + dcx - mouseX) * 0.28 : (mouseX - this.width / 2 - dcx) * 0.21875;
			dcy += delta > 0 ? (this.height / 2 + dcy - mouseY) * 0.28 : (mouseY - this.height / 2 - dcy) * 0.21875;
		}
		return true;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		boolean handled = super.mouseClicked(mouseX, mouseY, button);
		if (!handled && button == 0) {
			clicked = true;
			clickX = (int) mouseX;
			clickY = (int) mouseY;
			odcx = dcx;
			odcy = dcy;
		}
		return handled;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		boolean handled = super.mouseReleased(mouseX, mouseY, button);
		if (clicked) {
			clicked = false;
			if (!moved) {
				try {
					URI uri = new URI(url);
					if (minecraft.options.chatLinksPrompt().get()) {
						clickedLinkURI = uri;
						minecraft.setScreen(new ConfirmLinkScreen(result -> {
							if (result) {
								openWebLink(clickedLinkURI);
							}
							clickedLinkURI = null;
							minecraft.setScreen(this);
						}, url, false));
					} else {
						openWebLink(uri);
					}
				} catch (URISyntaxException e) {
				}
			}
			moved = false;
		}
		return handled;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		boolean handled = super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
		if (clicked) {
			dcx = odcx + (int) mouseX - clickX;
			dcy = odcy + (int) mouseY - clickY;
			moved = true;
		}
		return handled;
	}

	private void openWebLink(URI url) {
		try {
			Class<?> oclass = Class.forName("java.awt.Desktop");
			Object object = oclass.getMethod("getDesktop").invoke((Object) null);
			oclass.getMethod("browse", URI.class).invoke(object, url);
		} catch (Throwable e) {
		}
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// 1.20.1 中网络卡片贴图已注册成 ResourceLocation，可直接走 GuiGraphics#blit（原实现是裸 GL 纹理 id）
		ResourceLocation texture = LoliCardOnlineUtil.getTexture(url);
		if (texture != null) {
			int imageWidth = LoliCardOnlineUtil.getWidth(url);
			int imageHeight = LoliCardOnlineUtil.getHeight(url);
			if (imageWidth > 0 && imageHeight > 0) {
				double ratio = (double) imageWidth / (double) imageHeight;
				int cx = this.width / 2 + dcx;
				int cy = this.height / 2 + dcy;
				double proportion;
				if (ratio < (double) this.width / (double) this.height) {
					proportion = (double) this.height / (double) imageHeight;
				} else {
					proportion = (double) this.width / (double) imageWidth;
				}
				int x = (int) (imageWidth * proportion / 2 * ds);
				int y = (int) (imageHeight * proportion / 2 * ds);
				if (x > 0 && y > 0) {
					guiGraphics.blit(texture, cx - x, cy - y, x * 2, y * 2, 0.0F, 0.0F, imageWidth, imageHeight, imageWidth, imageHeight);
				}
			}
		} else {
			LoliCardOnlineUtil.load(url);
		}
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

}
