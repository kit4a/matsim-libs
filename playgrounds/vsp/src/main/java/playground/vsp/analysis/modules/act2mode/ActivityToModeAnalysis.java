/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsp.analysis.modules.act2mode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import playground.vsp.analysis.modules.AbstractAnalyisModule;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author droeder
 *
 */
public class ActivityToModeAnalysis extends AbstractAnalyisModule {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(ActivityToModeAnalysis.class);
	private Network net;
	private ActivityToModeAnalysisHandler handler;
	private HashMap<Integer, Set<SimpleFeature>> departureSlotFeatures;
	private HashMap<Integer, Set<SimpleFeature>> arrivalSlotFeatures;
	private int slotSize;
	private final String targetCoordinateSystem;

	/**
	 * a class to create shapefiles per timeSlot for activities mapped to the first mainmode after/before the activity
	 * @param sc, the scenario (it has to contain facilities!!!)
	 * @param personsOfInterest, might be null, than all persons are processed
	 * @param slotSize, timeSlotSize in seconds
	 */
	public ActivityToModeAnalysis(Scenario sc, Set<Id> personsOfInterest, int slotSize, String targetCoordinateSystem) {
		super(ActivityToModeAnalysis.class.getSimpleName());
		this.net = sc.getNetwork();
		this.handler = new ActivityToModeAnalysisHandler(this.net, personsOfInterest);
		this.slotSize = slotSize;
		this.targetCoordinateSystem = targetCoordinateSystem;
	}

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new ArrayList<EventHandler>();
		handler.add(this.handler);
		return handler;
	}

	@Override
	public void preProcessData() {

	}

	@Override
	public void postProcessData() {
		// creature featureType
		PointFeatureFactory factory = new PointFeatureFactory.Builder().
				setCrs(MGC.getCRS(this.targetCoordinateSystem)).
				setName("activities").
				addAttribute("ActType", String.class).
				addAttribute("Mode", String.class).
				create();
				
		//create features for departure
		this.departureSlotFeatures = new HashMap<Integer, Set<SimpleFeature>>();
		for(ActivityToMode atm : this.handler.getDepartures()){
			createFeatureAndAdd(atm, this.departureSlotFeatures, factory);
		}
		
		//create features for arrivals
		this.arrivalSlotFeatures = new HashMap<Integer, Set<SimpleFeature>>();
		for(ActivityToMode atm : this.handler.getArrivals()){
			createFeatureAndAdd(atm, this.arrivalSlotFeatures, factory);
		}

	}

	/**
	 * @param atm
	 * @param departureSlotFeatures2
	 * @param featureType 
	 */
	private void createFeatureAndAdd(ActivityToMode atm, 
			HashMap<Integer, Set<SimpleFeature>> slotFeatures, PointFeatureFactory factory) {
		Integer slice = (int) (atm.getTime() / this.slotSize);
		Set<SimpleFeature> temp = slotFeatures.get(slice);
		if(temp == null) {
			temp = new HashSet<SimpleFeature>();
		}
		Object[] featureAttribs = new Object[3];
		featureAttribs[0] = new GeometryFactory().createPoint(new Coordinate(atm.getCoord().getX(), atm.getCoord().getY(), 0.));
		featureAttribs[1] = atm.getActType();
		featureAttribs[2] = atm.getMode();
		SimpleFeature f = factory.createPoint(atm.getCoord(), new Object[] {atm.getActType(),  atm.getMode()}, null);
		temp.add(f);
	}

	@Override
	public void writeResults(String outputFolder) {
		for(Entry<Integer, Set<SimpleFeature>> e: this.departureSlotFeatures.entrySet()){
			ShapeFileWriter.writeGeometries(e.getValue(), outputFolder + "departure_" + e.getKey().toString()  + ".shp");
		}
		for(Entry<Integer, Set<SimpleFeature>> e: this.arrivalSlotFeatures.entrySet()){
			ShapeFileWriter.writeGeometries(e.getValue(), outputFolder + "arrival_" + e.getKey().toString()  + ".shp");
		}
	}
}

