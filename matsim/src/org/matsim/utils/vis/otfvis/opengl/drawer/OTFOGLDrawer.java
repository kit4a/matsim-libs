/* *********************************************************************** *
 * project: org.matsim.*
 * OTFOGLDrawer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.utils.vis.otfvis.opengl.drawer;

import static javax.media.opengl.GL.GL_COLOR_BUFFER_BIT;
import static javax.media.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static javax.media.opengl.GL.GL_LINEAR;
import static javax.media.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static javax.media.opengl.GL.GL_TEXTURE_MIN_FILTER;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLEventListener;
import javax.swing.JFrame;

import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimResource;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.vis.netvis.renderers.ValueColorizer;
import org.matsim.utils.vis.otfvis.caching.SceneGraph;
import org.matsim.utils.vis.otfvis.caching.SceneLayer;
import org.matsim.utils.vis.otfvis.data.OTFClientQuad;
import org.matsim.utils.vis.otfvis.data.OTFDataSimpleAgent;
import org.matsim.utils.vis.otfvis.data.OTFData.Receiver;
import org.matsim.utils.vis.otfvis.gui.OTFVisConfig;
import org.matsim.utils.vis.otfvis.handler.OTFDefaultLinkHandler;
import org.matsim.utils.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfvis.interfaces.OTFQueryHandler;
import org.matsim.utils.vis.otfvis.opengl.gl.InfoText;
import org.matsim.utils.vis.otfvis.opengl.gl.Point3f;
import org.matsim.utils.vis.otfvis.opengl.gui.VisGUIMouseHandler;

import com.sun.opengl.util.j2d.TextRenderer;
import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureIO;



abstract class OGLSceneLayerImpl implements SceneLayer{
	private final List<OTFGLDrawable> items = new ArrayList<OTFGLDrawable>();

	public void addItem(Receiver item) {
		this.items.add((OTFGLDrawable)item);
	}

	public void draw() {
		for (OTFGLDrawable item : this.items) item.draw();
	}

	public void finish() {
		// TODO Auto-generated method stub

	}

	public void init(SceneGraph graph) {
		// TODO Auto-generated method stub

	}

	public Object newInstance(Class clazz) throws InstantiationException, IllegalAccessException {
		// TODO Auto-generated method stub
		return null;
	}

}

public class OTFOGLDrawer implements OTFDrawer, GLEventListener, OGLProvider{
	private static int linkTexWidth = 0;
	private static float agentSize = 10.f;
	//private static float scaledAgentSize = 10.f;
	private int netDisplList = 0;
	private int agentDisplList = 0;

	//private boolean isValid = false;
	public boolean isActiveNet = false;
	private GL gl = null;
	private GLCanvas canvas = null;
	private VisGUIMouseHandler mouseMan = null;
	private final OTFClientQuad clientQ;

	//Handle these separately, as the agents needs textures set, which should only be done once
	private final List<OTFGLDrawable> netItems = new ArrayList<OTFGLDrawable>();
	private final List<OTFGLDrawable> agentItems = new ArrayList<OTFGLDrawable>();
	//private final List<OTFGLDrawable> otherItems = new ArrayList<OTFGLDrawable>();

	//private final SimpleBackgroundDrawer background = null;

	private static List<OTFGLDrawable> newItems = new ArrayList<OTFGLDrawable>();

	private StatusTextDrawer statusDrawer = null;

	private OTFVisConfig config = null;
	private OTFQueryHandler queryHandler = null;

	public static class StatusTextDrawer {

		private TextRenderer textRenderer;
		private String status;
		private int statusWidth;
		private final GLAutoDrawable drawable;

		public StatusTextDrawer(GLAutoDrawable drawable) {
			initTextRenderer();
			this.drawable = drawable;
		}
		private void initTextRenderer() {
			// Create the text renderer
			Font font = new Font("SansSerif", Font.PLAIN, 32);
			this.textRenderer = new TextRenderer(font, true, false);
			InfoText.setRenderer(this.textRenderer);
		}

		private void displayStatusText(String text) {
				this.status  = text;

				if (this.statusWidth == 0) {
					// Place it at a fixed offset wrt the upper right corner
					this.statusWidth = (int) this.textRenderer.getBounds("FPS: 10000.00").getWidth();
				}

			// Calculate text location and color
			int x = this.drawable.getWidth() - this.statusWidth - 5;
			int y = this.drawable.getHeight() - 30;
			float c = 0.55f;

			// Render the text
			this.textRenderer.beginRendering(this.drawable.getWidth(), this.drawable.getHeight());
			this.textRenderer.setColor(c, c, c, c);
			this.textRenderer.draw(this.status, x, y);
			this.textRenderer.endRendering();
		}

	}


	public static class FastColorizer {

		final int grain;
		Color [] fastValues;
		double minVal, maxVal, valRange;

		public FastColorizer(double[] ds, Color[] colors, int grain) {
			ValueColorizer helper = new ValueColorizer(ds,colors);
			this.grain = grain;
			this.fastValues = new Color[grain];
			this.minVal = ds[0];
			this.maxVal = ds[ds.length-1];
			this.valRange = this.maxVal - this.minVal;
			// calc prerendered Values
			double step = this.valRange/grain;
			for(int i = 0; i< grain; i++) {
				double value = i*step + this.minVal;
				this.fastValues[i] = helper.getColor(value);
			}


		}

		public FastColorizer(double[] ds, Color[] colors) {
			this(ds, colors, 1000);
		}

		public Color getColor(double value) {
			if (value >= this.maxVal) return this.fastValues[this.grain-1];
			if (value < this.minVal) return this.fastValues[0];
			return this.fastValues[(int)((value-this.minVal)*this.grain/this.valRange)];
		}


	}
	public static class RandomColorizer{
		Color [] fastValues;
		private static final Random rand = new Random();

		public RandomColorizer(int size) {
			this.fastValues = new Color[size];
			for (int i = 0; i < size; i++) this.fastValues[i] = new Color(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255));
		}

		public Color getColor(int value) {
			return this.fastValues[value];
		}
	}

	public static class AgentDrawer extends OTFGLDrawableImpl implements OTFDataSimpleAgent.Receiver {
		//Anything above 50km/h should be yellow!
		private final static FastColorizer colorizer = new FastColorizer(
				new double[] { 0.0, 25, 50, 75}, new Color[] {
						Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE});

		protected char[] id;
		protected float startX, startY, color;
		protected int state;

		public static  Texture  carjpg = null;
		public static  Texture  wavejpg = null;
		public static  Texture  pedpng = null;

		public void setAgent(char[] id, float startX, float startY, int state, int user, float color) {
			this.id = id;
			this.startX = startX;
			this.startY = startY;
			this.color = color;
			this.state = state;
		}

		public void displayPS(GL gl) {

			//GLUT glut = new GLUT();

			/*float[] ambientDiffuse = {0f,0.7f,0f,1f};
	        gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT_AND_DIFFUSE, ambientDiffuse);*/

//			gl.glEnable(GL.GL_TEXTURE_2D);
//			gl.glTexEnvf(GL.GL_POINT_SPRITE_NV, GL.GL_COORD_REPLACE_NV, GL.GL_TRUE);
//			gl.glBindTexture(GL.GL_TEXTURE_2D, spriteTexture[0]);

			gl.glEnable(GL.GL_POINT_SPRITE_NV);

			gl.glPointSize(agentSize/10);

			gl.glBegin(GL.GL_POINTS);
			gl.glVertex3f(this.startX,this.startY, 0);
			gl.glEnd();


			gl.glDisable(GL.GL_POINT_SPRITE_NV);
//			gl.glDisable(GL.GL_TEXTURE_2D);

		}

		protected void setColor(GL gl) {
			Color color = colorizer.getColor(0.1 + 0.9*this.color);
			if ((this.state & 1) != 0) {
				color = Color.lightGray;
			}
			gl.glColor4d(color.getRed()/255., color.getGreen()/255.,color.getBlue()/255.,.8);

		}

		public void onDraw(GL gl) {
			final int z = 0;
			final float laneWidth = agentSize;
			final float width = laneWidth*1.5f;
			final float length = laneWidth*1.5f;

			setColor(gl);

			if (true) {
				displayPS(gl);
				return;
			}

			gl.glBegin(GL.GL_QUADS);
			gl.glTexCoord2f(1,1); gl.glVertex3f(this.startX - length, this.startY - width, z);
			gl.glTexCoord2f(1,0); gl.glVertex3f(this.startX - length, this.startY + width, z);
			gl.glTexCoord2f(0,0); gl.glVertex3f(this.startX + length, this.startY + width, z);
			gl.glTexCoord2f(0,1); gl.glVertex3f(this.startX + length, this.startY - width, z);
			gl.glEnd();
		}
	}

	protected static volatile GLContext motherContext = null;

	
	public OTFOGLDrawer(JFrame frame, OTFClientQuad clientQ) {
		this.clientQ = clientQ;
		GLCapabilities caps = new GLCapabilities();
		if (motherContext == null) {
			this.canvas = new GLCanvas(caps);
			motherContext = this.canvas.getContext();
		} else {
			this.canvas = new GLCanvas(caps, null, motherContext, null);
		}

		this.canvas.addGLEventListener(this);
		this.mouseMan = new VisGUIMouseHandler(this);
		this.mouseMan.setBounds((float)clientQ.getMinEasting(), (float)clientQ.getMinNorthing(), (float)clientQ.getMaxEasting(), (float)clientQ.getMaxNorthing(), 100);

		this.canvas.addMouseListener(this.mouseMan);
		this.canvas.addMouseMotionListener(this.mouseMan);
		this.canvas.addMouseWheelListener(this.mouseMan);

		this.canvas.setMinimumSize(new Dimension(50,50));
		this.canvas.setPreferredSize(new Dimension(300,300));
		this.canvas.setMaximumSize(new Dimension(1024,1024));

		OTFClientQuad.ClassCountExecutor counter = clientQ.new ClassCountExecutor(OTFDefaultLinkHandler.class);
		clientQ.execute(null, counter);
		double linkcount = counter.getCount();

		int size = linkTexWidth + (int)(0.5*Math.sqrt(linkcount))*2 +2;
		linkTexWidth = size;

		this.config = (OTFVisConfig) Gbl.getConfig().getModule("otfvis");
		}

	public static void addItem(OTFGLDrawable item) {
		newItems.add(item);
	}

	public void drawNetList(){
		// make quad filled to hit every pixel/texel

		this.gl.glNewList(this.netDisplList, GL.GL_COMPILE);

		System.out.print("DRAWING NET ONCE: objects count: " + this.netItems.size() );
		OTFGLDrawableImpl.gl = this.gl;
		for (OTFGLDrawable item : this.netItems) {
			item.draw();
		}
		this.gl.glEndList();
	}

	public void updateDisplay() {
		// make quad filled to hit every pixel/texel

		this.gl.glNewList(this.agentDisplList, GL.GL_COMPILE);

		for (OTFGLDrawable item : this.agentItems) {
			item.draw();
		}
		this.gl.glEndList();
		//System.out.println("CLIENT DRAWER DRAWED  == " + netItems.size()  +"objects time");
	}

	//private String lastTime = "";
	
	synchronized public void display(GLAutoDrawable drawable) {
//		Gbl.startMeasurement();
		this.gl = drawable.getGL();

		float[] components = this.config.getBackgroundColor().getColorComponents(new float[4]);
		this.gl.glClearColor(components[0], components[1], components[2], components[3]);
		this.gl.glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		this.gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);

		this.gl.glEnable(GL.GL_BLEND);
		this.gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

		this.mouseMan.setFrustrum(this.gl);

		components = this.config.getNetworkColor().getColorComponents(components);
		this.gl.glColor4d(components[0], components[1], components[2], components[3]);

		if ( this.actGraph != null) this.actGraph.draw();

		if(queryHandler != null) queryHandler.drawQueries(this);
		
//		if(background != null) {
//			background.onDraw(gl);
//		}
//
//
//		if (isActiveNet) {
//			gl.glEnable(GL.GL_TEXTURE_2D);
//			gl.glBindTexture(GL.GL_TEXTURE_2D, QuadDrawer.linkcolors);
//			gl.glCallList(netDisplList);
//			gl.glDisable(GL.GL_TEXTURE_2D);
//		} else gl.glCallList(netDisplList);
//
//		//AgentDrawer.carjpg.enable();
//		//AgentDrawer.carjpg.bind();
//		gl.glCallList(agentDisplList);
//		//AgentDrawer.carjpg.disable();
//
//		int vehs = 0;
//		for (OTFGLDrawable item : otherItems) {
//			item.draw();
//			if (item instanceof AgentArrayDrawer) {
//				vehs += ((AgentArrayDrawer)item).count;
//			}
//		}
//
		this.gl.glDisable(GL.GL_BLEND);

		InfoText.drawInfoTexts(drawable);

		this.mouseMan.drawElements(this.gl);
		//statusDrawer.displayStatusText( lastTime);
		//statusDrawer.displayStatusText("Z " + mouseMan.getView().z);

//		System.out.print("DRAWING : " );
//		Gbl.printElapsedTime();
	}

	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
	}

	public void init(GLAutoDrawable drawable) {
		this.gl = drawable.getGL();

		this.gl.setSwapInterval(0);
		float[] components = this.config.getBackgroundColor().getColorComponents(new float[4]);
		this.gl.glClearColor(components[0], components[1], components[2], components[3]);

		this.gl.glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		this.mouseMan.init(this.gl);

		AgentDrawer.carjpg = createTexture(MatsimResource.getAsInputStream("car.png"));
		AgentDrawer.wavejpg = createTexture(MatsimResource.getAsInputStream("square.png"));
		AgentDrawer.pedpng = createTexture(MatsimResource.getAsInputStream("ped.png"));

		int test = this.gl.glGetError();
		System.out.println("GLerror = " + test);
		//TextureIO.newTexture(new TextureData(GL.GL_RGBA, size,size,0,GL.GL_RGBA8,GL.GL_INT,false,false,true,IntBuffer.wrap(buffer),null));
		test = this.gl.glGetError();
		System.out.println("GLerror = " + test);

		// create two new lists
		this.netDisplList = this.gl.glGenLists(1);
		this.agentDisplList = this.gl.glGenLists(1);

		drawNetList();
		//this.isValid = false;
	}

	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {
		GL gl = drawable.getGL();

		gl.glViewport(0, 0, width, height);
		this.mouseMan.setAspectRatio((double)width / (double)height);
		this.mouseMan.setFrustrum(gl);
		this.statusDrawer = new StatusTextDrawer(drawable);
	}


	public void handleClick(Point2D.Double point, int mouseButton) {
		Point2D.Double origPoint = new Point2D.Double(point.x + this.clientQ.offsetEast, point.y + this.clientQ.offsetNorth);
		if(queryHandler != null) queryHandler.handleClick(origPoint, mouseButton);
	}

	public void handleClick(Rectangle currentRect, int button) {
		Rectangle2D.Double origRect = new Rectangle2D.Double(currentRect.x + this.clientQ.offsetEast, currentRect.y + this.clientQ.offsetNorth, currentRect.width, currentRect.height);
		if(queryHandler != null) queryHandler.handleClick(origRect, button);
	}

	/***
	 * redraw refreshes the displayed graphic i.e. it draws the same items as last time something was drawn
	 * useful for zooming, or overlaying GUI items
	 * if the displayed Rect is moved or enlarged, we need to call invalidate, to get the correct data from the host
	 */
	public void redraw() {
		this.canvas.display();
	}

	private final Object blockRefresh = new Object();
	private SceneGraph actGraph = null;

	/***
	 * invalidate, gets the actual correct data from the host, to display the given rect
	 * This method is used in most cases
	 * @throws RemoteException
	 */
	public void invalidate(int time) throws RemoteException {

		agentSize = Float.parseFloat(Gbl.getConfig().getParam(OTFVisConfig.GROUP_NAME, OTFVisConfig.AGENT_SIZE));
		//scaledAgentSize = agentSize * this.mouseMan.getScale();


		//lastTime = Time.writeTime(time, ':');
		
		// do something like
		// getTimeStep from somewhere
		// check: is there a cached version for timestep
		// use chached version, else get the real one

		{
			synchronized (this.blockRefresh) {

				QuadTree.Rect rect = this.mouseMan.getBounds();
				synchronized (newItems) {
//					clientQ.getDynData(rect);
//					Gbl.startMeasurement();
//					clientQ.invalidate(rect);

					this.actGraph  = this.clientQ.getSceneGraph(time, rect, this);

//					if ( AgentPointDrawer.globalArrayDrawer.count != 0) {
//						AgentPointDrawer.globalArrayDrawer.compress();
//						graph.addItem(AgentPointDrawer.globalArrayDrawer);
//					}
//					List<OTFDrawable> list = graph.getAllItemsKILLTHIS();
//					newItems.clear();
//					for(OTFDrawable item : list) newItems.add((OTFGLDrawable)item);
//
//
//					moveNewItems();

			}
			}
//			System.out.println("Scale: " + scaledAgentSize + " Invalidate : " );
//			Gbl.printElapsedTime();
		}
		// Todo put drawing to displyLists here and in
		// display(gl) we only display the two lists
		
		if(queryHandler != null) queryHandler.updateQueries();
        //this.isValid = false;
		redraw();
	}

	/**
	 * @return the canvas
	 */
	public Component getComponent() {
		return this.canvas;
	}

	public GL getGL() {
		return this.gl;
	}

	public OTFClientQuad getQuad() {
		return this.clientQ;
	}

	public Point3f getView() {
		return this.mouseMan.getView();
	}

	/**
	 * @return the actGraph
	 */
	public SceneGraph getActGraph() {
		return this.actGraph;
	}

	static public Texture createTexture(String filename) {
		Texture t = null;
		try {
			t = TextureIO.newTexture(new FileInputStream(filename),
					true, null);
			t.setTexParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			t.setTexParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		} catch (IOException e) {
			System.err.println("Error loading " + filename); // TODO switch to Log4J, include exception-message to output
		}
		return t;
	}

	static public Texture createTexture(final InputStream data) {
		Texture t = null;
		try {
			t = TextureIO.newTexture(data, true, null);
			t.setTexParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			t.setTexParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		} catch (IOException e) {
			System.err.println("Error loading Texture from stream: " + e.getMessage());
		}
		return t;
	}
	
	public void clearCache() {
		if (clientQ != null) clientQ.clearCache();
	}

	/**
	 * @param queryHandler the queryHandler to set
	 */
	public void setQueryHandler(OTFQueryHandler queryHandler) {
		if(queryHandler != null) this.queryHandler = queryHandler;
	}


}
