/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2015 Tobias Pietzsch, Stephan Preibisch, Barry DeZonia,
 * Stephan Saalfeld, Curtis Rueden, Albert Cardona, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Jonathan Hale, Lee Kamentsky, Larry Lindsey, Mark
 * Hiner, Michael Zinsmaier, Martin Horn, Grant Harris, Aivar Grislis, John
 * Bogovic, Steffen Jaensch, Stefan Helfrich, Jan Funke, Nick Perry, Mark Longair,
 * Melissa Linkert and Dimiter Prodanov.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package tests;

import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import io.scif.img.ImgUtilityService;

import java.awt.Adjustable;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.border.TitledBorder;

import net.imagej.ImgPlus;
import net.imglib2.converter.RealARGBConverter;
import net.imglib2.display.projector.IterableIntervalProjector2D;
import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

/**
 * A simple UI that demonstrates display of {@link Img}s.
 *
 * @author Curtis Rueden
 */
public class ImgPanel extends JPanel {

	public class ImgData<T extends RealType<T> & NativeType<T>> {

		public String name;
		public ImgPlus<T> imgPlus;
		public ImgPanel owner;
		public int width, height;
		public ARGBScreenImage screenImage;
		public RealARGBConverter<T> converter;
		public IterableIntervalProjector2D<T, ARGBType> projector;

		public ImgData(final String name, final ImgPlus<T> imgPlus,
			final ImgPanel owner)
		{
			this.name = name;
			this.imgPlus = imgPlus;
			this.owner = owner;
			width = (int) imgPlus.dimension(0);
			height = (int) imgPlus.dimension(1);
			screenImage = new ARGBScreenImage(width, height);
			final int min = 0, max = 255;
			converter = new RealARGBConverter<T>(min, max);
			projector =
				new IterableIntervalProjector2D<T, ARGBType>(0, 1, imgPlus, screenImage, converter);
			projector.map();
		}
	}

	public class SliderPanel extends JPanel {

		public SliderPanel(final ImgData<?> imgData) {
			setBorder(new TitledBorder(imgData.name));
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			// add one slider per dimension beyond the first two
			for (int d = 2; d < imgData.imgPlus.numDimensions(); d++) {
				final int dimLength = (int) imgData.imgPlus.dimension(d);
				final JScrollBar bar =
					new JScrollBar(Adjustable.HORIZONTAL, 0, 1, 0, dimLength);
				final int dim = d;
				bar.addAdjustmentListener(new AdjustmentListener() {

					@Override
					public void adjustmentValueChanged(final AdjustmentEvent e) {
						final int value = bar.getValue();
						imgData.projector.setPosition(value, dim);
						imgData.projector.map();
						imgData.owner.repaint();
					}
				});
				add(bar);
			}
		}
	}

	protected List<ImgData<?>> images = new ArrayList<ImgData<?>>();
	protected int maxWidth = 0, maxHeight = 0;

	public ImgPanel() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(new JPanel() { // image canvas

			@Override
			public void paint(final Graphics g) {
				for (final ImgData<?> imgData : images) {
					final Image image = imgData.screenImage.image();
					g.drawImage(image, 0, 0, this);
				}
			}

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(maxWidth, maxHeight);
			}
		});
	}

	public <T extends RealType<T> & NativeType<T>> void addImage(
		final String name, final ImgPlus<T> img)
	{
		final ImgData<T> imgData = new ImgData<T>(name, img, this);
		images.add(imgData);
		if (imgData.width > maxWidth) maxWidth = imgData.width;
		if (imgData.height > maxHeight) maxHeight = imgData.height;
		add(new SliderPanel(imgData));
	}

	public static final <T extends RealType<T> & NativeType<T>> void main(
		final String[] args)
	{
		final String[] urls = {
			"http://loci.wisc.edu/files/software/ome-tiff/z-series.zip"
		};
		final JFrame frame = new JFrame("ImgPanel Test Frame");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		final ImgPanel imgPanel = new ImgPanel();
		for (final String url : urls) {
			final ImgPlus<T> img = loadImage(url);
			imgPanel.addImage(url, img);
		}
		frame.setContentPane(imgPanel);
		frame.pack();
		center(frame);
		frame.setVisible(true);
	}

	private static <T extends RealType<T> & NativeType<T>> ImgPlus<T> loadImage(
		final String url)
	{
		try {
			final ImgOpener imgOpener = new ImgOpener();
			System.out.println("Downloading " + url);
			final ImgUtilityService imgUtilityService =
				imgOpener.getContext().getService(ImgUtilityService.class);
			final String id = imgUtilityService.cacheId(url);
			System.out.println("Opening " + id);
			return (ImgPlus<T>) imgOpener.openImg(id);
		}
		catch (final ImgIOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void center(final Window win) {
		final Dimension size = win.getSize();
		final Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		final int w = (screen.width - size.width) / 2;
		final int h = (screen.height - size.height) / 2;
		win.setLocation(w, h);
	}

}
