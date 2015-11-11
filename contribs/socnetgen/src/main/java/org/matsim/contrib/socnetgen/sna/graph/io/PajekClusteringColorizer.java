/* *********************************************************************** *
 * project: org.matsim.*
 * PajekClusteringColorizer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.socnetgen.sna.graph.io;

import gnu.trove.TObjectDoubleHashMap;
import org.apache.commons.math.stat.StatUtils;
import org.matsim.contrib.socnetgen.sna.graph.Edge;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.analysis.Transitivity;

import java.util.Set;

/**
 * @author illenberger
 *
 */
public class PajekClusteringColorizer<V extends Vertex, E extends Edge> extends PajekColorizer<V, E> {

	private double c_min;
	
	private double c_max;
	
	private TObjectDoubleHashMap<V> clustering;
	
	@SuppressWarnings("unchecked")
	public PajekClusteringColorizer(Graph g) {
		super();
		clustering = Transitivity.getInstance().localClusteringCoefficients((Set<V>)g.getVertices());		
		c_min = StatUtils.min(clustering.getValues());
		c_max = StatUtils.max(clustering.getValues());
	}
	
	@Override
	public String getEdgeColor(E e) {
		return getColor(-1);
	}

	@Override
	public String getVertexFillColor(V ego) {
		double c = clustering.get(ego);
		double color = (c - c_min) / (c_max - c_min);
		return getColor(color);
	}

}