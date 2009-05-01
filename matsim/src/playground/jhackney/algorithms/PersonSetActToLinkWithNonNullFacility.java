/* *********************************************************************** *
 * project: org.matsim.*
 * PersonSetNearestFacility.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.jhackney.algorithms;

import java.util.Iterator;

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.api.facilities.Facilities;
import org.matsim.core.api.facilities.Facility;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PersonSetActToLinkWithNonNullFacility extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String H = "h";
	private static final String W = "w";
	private static final String L = "l";
	private static final String S = "s";
	private static final String E = "e";
	private static final String HOME = "home";
	private static final String WORK = "work";
	private static final String EDUCATION = "education";
	private static final String LEISURE = "leisure";
	private static final String SHOP = "shop";

	private final Facilities facilities;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonSetActToLinkWithNonNullFacility(final Facilities facilities) {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.facilities = facilities;
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// build methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final String getFacilityActType(String act_type) {
		if (act_type.startsWith(E)) { return EDUCATION; }
		else if (act_type.startsWith(S)) { return SHOP; }
		else if (act_type.startsWith(L)) { return LEISURE; }
		else if (act_type.startsWith(H)) { return HOME; }
		else if (act_type.startsWith(W)) { return WORK; }
		else { Gbl.errorMsg("act_type=" + act_type + " not allowed!"); return null; }
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		if (person.getPlans().size() != 1) { Gbl.errorMsg("Each person must have one plan!"); }
		Plan plan = person.getPlans().get(0);
		this.run(plan);
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public void run(Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;
				String type = this.getFacilityActType(act.getType());
				Coord coord = act.getCoord();
				if (coord == null) { Gbl.errorMsg("Each act must have a coord!"); }
				double nearest_dist = Double.MAX_VALUE;
				Facility nearest_f = null;
				Iterator<? extends Facility> f_it = this.facilities.getFacilities().values().iterator();
				while (f_it.hasNext()) {
					Facility f = f_it.next();
					if (f.getActivityOptions().containsKey(type)) {
						double dist = f.calcDistance(coord);
						if (dist < nearest_dist) {
							nearest_dist = dist;
							nearest_f = f;
						}
					}
				}
				if (nearest_f == null) {
					Gbl.errorMsg("p_id=" + plan.getPerson().getId() + ": no facility found for act=" + act);
				}
				act.setCoord(nearest_f.getCoord());
				act.setLink(nearest_f.getLink());//JH
				System.out.println("f link "+ nearest_f.getId()+" "+nearest_f.getLink().getId());
				System.out.println("  p_id=" + plan.getPerson().getId() + ", act=" + act.getType() + ": nearest dist=" + nearest_dist);
			}
		}
	}
}

