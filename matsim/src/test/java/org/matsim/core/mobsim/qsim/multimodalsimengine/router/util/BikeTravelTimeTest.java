/* *********************************************************************** *
 * project: org.matsim.*
 * BikeTravelTimeTest.java
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

package org.matsim.core.mobsim.qsim.multimodalsimengine.router.util;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class BikeTravelTimeTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(BikeTravelTimeTest.class);
	
	public void testLinkTravelTimeCalculation() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		Node node1 = scenario.getNetwork().getFactory().createNode(scenario.createId("n1"), new CoordImpl(0.0, 0.0));
		Node node2 = scenario.getNetwork().getFactory().createNode(scenario.createId("n2"), new CoordImpl(1.0, 0.0));
		Link link = scenario.getNetwork().getFactory().createLink(scenario.createId("l1"), node1, node2);
		link.setLength(1.0);
		scenario.getNetwork().addNode(node1);
		scenario.getNetwork().addNode(node2);
		scenario.getNetwork().addLink(link);
		
		ObjectAttributes heightInformation = new ObjectAttributes();		
		heightInformation.putAttribute(node1.getId().toString(), CalcLinkSlopes.ATTRIBUTE_NAME, "0.0");
		heightInformation.putAttribute(node2.getId().toString(), CalcLinkSlopes.ATTRIBUTE_NAME, "0.0");
		Map<Id, Double> linkSlopes;
		linkSlopes = new CalcLinkSlopes().calcLinkSlopes(scenario.getNetwork(), heightInformation);

		PersonImpl person = (PersonImpl) scenario.getPopulation().getFactory().createPerson(scenario.createId("p1"));
		person.setAge(20);
		person.setSex("m");
		
		// set default walk speed; according to Weidmann 1.34 [m/s]
		double defaultWalkSpeed = 1.34;
		scenario.getConfig().plansCalcRoute().setTeleportedModeSpeed(TransportMode.walk, defaultWalkSpeed);
		
		// set default bike speed; Prakin and Rotheram according to 6.01 [m/s]
		double defaultBikeSpeed = 6.01;
		scenario.getConfig().plansCalcRoute().setTeleportedModeSpeed(TransportMode.bike, defaultBikeSpeed);
		
		BikeTravelTime bikeTravelTime;
				
		double speed;
		double expectedTravelTime;
		double calculatedTravelTime;
		
		// reference speed * person factor * slope factor
		bikeTravelTime = new BikeTravelTime(scenario.getConfig().plansCalcRoute(), linkSlopes);
		calculatedTravelTime = bikeTravelTime.getLinkTravelTime(link, 0.0, person, null);
		speed = defaultBikeSpeed * bikeTravelTime.personFactorCache.get() * 1.0;
		expectedTravelTime = link.getLength() / speed;
		printInfo(person, link, expectedTravelTime, calculatedTravelTime, heightInformation);
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < EPSILON);
		assertEquals(calculatedTravelTime - 0.09368418280727171, 0.0);
		
		// increase age
		person.setAge(80);
		bikeTravelTime = new BikeTravelTime(scenario.getConfig().plansCalcRoute(), linkSlopes);
		calculatedTravelTime = bikeTravelTime.getLinkTravelTime(link, 0.0, person, null);
		speed = defaultBikeSpeed * bikeTravelTime.personFactorCache.get() * 1.0;
		expectedTravelTime = link.getLength() / speed;
		printInfo(person, link, expectedTravelTime, calculatedTravelTime, heightInformation);
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < EPSILON);
		assertEquals(calculatedTravelTime - 0.2206463555843433, 0.0);

		// change gender
		person.setSex("f");
		bikeTravelTime = new BikeTravelTime(scenario.getConfig().plansCalcRoute(), linkSlopes);
		calculatedTravelTime = bikeTravelTime.getLinkTravelTime(link, 0.0, person, null);
		speed = defaultBikeSpeed * bikeTravelTime.personFactorCache.get() * 1.0;
		expectedTravelTime = link.getLength() / speed;
		printInfo(person, link, expectedTravelTime, calculatedTravelTime, heightInformation);
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < EPSILON);
		assertEquals(calculatedTravelTime - 0.24496957588497956, 0.0);
		
		// change slope from 0% to 10%
		heightInformation.putAttribute(node2.getId().toString(), CalcLinkSlopes.ATTRIBUTE_NAME, "0.1");
		linkSlopes = new CalcLinkSlopes().calcLinkSlopes(scenario.getNetwork(), heightInformation);
		bikeTravelTime = new BikeTravelTime(scenario.getConfig().plansCalcRoute(), linkSlopes);

		calculatedTravelTime = bikeTravelTime.getLinkTravelTime(link, 0.0, person, null);
		double slope = bikeTravelTime.getSlope(link);
		double slopeShift = bikeTravelTime.getSlopeShift(slope);
		speed = defaultBikeSpeed * bikeTravelTime.personFactorCache.get() + slopeShift;
		expectedTravelTime = link.getLength() / speed;
		printInfo(person, link, expectedTravelTime, calculatedTravelTime, heightInformation);				
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < EPSILON);
		assertEquals(calculatedTravelTime - 0.7332007724445855, 0.0);
		
		// change slope from 10% to -10%
		heightInformation.putAttribute(node2.getId().toString(), CalcLinkSlopes.ATTRIBUTE_NAME, "-0.1");
		linkSlopes = new CalcLinkSlopes().calcLinkSlopes(scenario.getNetwork(), heightInformation);
		bikeTravelTime = new BikeTravelTime(scenario.getConfig().plansCalcRoute(), linkSlopes);

		calculatedTravelTime = bikeTravelTime.getLinkTravelTime(link, 0.0, person, null);
		slope = bikeTravelTime.getSlope(link);
		slopeShift = bikeTravelTime.getSlopeShift(slope);
		speed = defaultBikeSpeed * bikeTravelTime.personFactorCache.get() + slopeShift;
		expectedTravelTime = link.getLength() / speed;
		printInfo(person, link, expectedTravelTime, calculatedTravelTime, heightInformation);				
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < EPSILON);
		assertEquals(calculatedTravelTime - 0.40547153706106515, 0.0);
		
		// on very steep links bike speed should equals walk speed - set slope to 25%
		heightInformation.putAttribute(node2.getId().toString(), CalcLinkSlopes.ATTRIBUTE_NAME, "0.25");
		linkSlopes = new CalcLinkSlopes().calcLinkSlopes(scenario.getNetwork(), heightInformation);
		bikeTravelTime = new BikeTravelTime(scenario.getConfig().plansCalcRoute(), linkSlopes);

		WalkTravelTime walkTravelTime = new WalkTravelTime(scenario.getConfig().plansCalcRoute(), linkSlopes);
		calculatedTravelTime = bikeTravelTime.getLinkTravelTime(link, 0.0, person, null);	
		expectedTravelTime = walkTravelTime.getLinkTravelTime(link, 0.0, person, null);
		printInfo(person, link, expectedTravelTime, calculatedTravelTime, heightInformation);
		assertTrue(Math.abs(expectedTravelTime - calculatedTravelTime) < EPSILON);
		assertEquals(calculatedTravelTime - 1.7305194978040106, 0.0);
	}
	
	private void printInfo(PersonImpl p, Link l, double expected, double calculated, ObjectAttributes heightInformation) {
		StringBuffer sb = new StringBuffer();
		sb.append("Age: ");
		sb.append(p.getAge());
		sb.append("; ");

		sb.append("Sex: ");
		sb.append(p.getSex());
		sb.append("; ");
		
		sb.append("Link Steepness: ");
		String fromHeight = (String) heightInformation.getAttribute(l.getFromNode().getId().toString(), CalcLinkSlopes.ATTRIBUTE_NAME);
		String toHeight = (String) heightInformation.getAttribute(l.getToNode().getId().toString(), CalcLinkSlopes.ATTRIBUTE_NAME);
		sb.append(100.0 * (Double.valueOf(toHeight) - Double.valueOf(fromHeight)) / l.getLength());
		sb.append("%; ");
		
		sb.append("Expected Travel Time: ");
		sb.append(expected);
		sb.append("; ");

		sb.append("Calculated Travel Time: ");
		sb.append(calculated);
		sb.append("; ");
		
		log.info(sb.toString());
	}
	
	public void testThreadLocals() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		PersonImpl p1 = new PersonImpl(new IdImpl("1"));
		p1.setAge(90);
		p1.setSex("f");

		PersonImpl p2 = new PersonImpl(new IdImpl("2"));
		p2.setAge(20);
		p2.setSex("m");
		
		Node node1 = scenario.getNetwork().getFactory().createNode(scenario.createId("n1"), new CoordImpl(0.0, 0.0));
		Node node2 = scenario.getNetwork().getFactory().createNode(scenario.createId("n2"), new CoordImpl(1.0, 0.0));
		Link link = scenario.getNetwork().getFactory().createLink(scenario.createId("l1"), node1, node2);
		link.setLength(1.0);
		scenario.getNetwork().addNode(node1);
		scenario.getNetwork().addNode(node2);
		scenario.getNetwork().addLink(link);
		
		ObjectAttributes heightInformation = new ObjectAttributes();		
		heightInformation.putAttribute(node1.getId().toString(), CalcLinkSlopes.ATTRIBUTE_NAME, "0.0");
		heightInformation.putAttribute(node2.getId().toString(), CalcLinkSlopes.ATTRIBUTE_NAME, "0.0");
		Map<Id, Double> linkSlopes = new CalcLinkSlopes().calcLinkSlopes(scenario.getNetwork(), heightInformation);

		BikeTravelTime bikeTravelTime = new BikeTravelTime(config.plansCalcRoute(), linkSlopes);
		
		double tt1 = bikeTravelTime.getLinkTravelTime(link, 0.0, p1, null);
		double tt2 = bikeTravelTime.getLinkTravelTime(link, 0.0, p2, null);
		
		log.info("run concurrent tests");
		Runnable r1 = new TestRunnable(p1, p2, bikeTravelTime, link, tt1, tt2);
		Runnable r2 = new TestRunnable(p2, p1, bikeTravelTime, link, tt2, tt1);
		Runnable r3 = new TestRunnable(p1, p2, bikeTravelTime, link, tt1, tt2);
		Runnable r4 = new TestRunnable(p2, p1, bikeTravelTime, link, tt2, tt1);
		
		Thread t1 = new Thread(r1);
		Thread t2 = new Thread(r2);
		Thread t3 = new Thread(r3);
		Thread t4 = new Thread(r4);
		
		t1.setName("Thread 1");
		t2.setName("Thread 2");
		t3.setName("Thread 3");
		t4.setName("Thread 4");
		
		t1.start();
		t2.start();
		t3.start();
		t4.start();
		
		try {
			t1.join();
			t2.join();
			t3.join();
			t4.join();
		} catch (InterruptedException e) { 
			Gbl.errorMsg(e); 
		}
		
		log.info("done");

	}

	private static class TestRunnable implements Runnable {

		private final Person p1;
		private final Person p2;
		private final BikeTravelTime bikeTravelTime;
		private final Link link;
		private final double expectedTravelTime1;
		private final double expectedTravelTime2;
		
		public TestRunnable(Person p1, Person p2, BikeTravelTime bikeTravelTime, Link link, 
				double expectedTravelTime1, double expectedTravelTime2) {
			this.p1 = p1;
			this.p2 = p2;
			this.bikeTravelTime = bikeTravelTime;
			this.link = link;
			this.expectedTravelTime1 = expectedTravelTime1;
			this.expectedTravelTime2 = expectedTravelTime2;
		}
		
		@Override
		public void run() {
			log.info("Thread " + Thread.currentThread().getName() + " started");
			for (int i = 0; i < 10000; i++) {
				// check with alternating persons 
				double tt1 = bikeTravelTime.getLinkTravelTime(link, 0.0, p1, null);
				assertTrue(tt1 == expectedTravelTime1);
				
				double tt2 = bikeTravelTime.getLinkTravelTime(link, 0.0, p2, null);
				assertTrue(tt2 == expectedTravelTime2);
				
				// check with constant person
				for (int j = 0; j < 5; j++) {
					double tt = bikeTravelTime.getLinkTravelTime(link, 0.0, p1, null);
					assertTrue(tt == expectedTravelTime1);
				}
			}
			log.info("Thread " + Thread.currentThread().getName() + " finished");
		}
	}
}
