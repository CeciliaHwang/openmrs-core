/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.api;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Order.Action;
import org.openmrs.Patient;
import org.openmrs.TestOrder;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseContextSensitiveTest;
import org.openmrs.test.Verifies;
import org.openmrs.util.PrivilegeConstants;

/**
 * TODO clean up and test all methods in OrderService
 */
public class OrderServiceTest extends BaseContextSensitiveTest {
	
	private OrderService orderService;
	
	private PatientService patientService;
	
	@Before
	public void setup() {
		if (orderService == null) {
			orderService = Context.getOrderService();
		}
		if (patientService == null) {
			patientService = Context.getPatientService();
		}
	}
	
	//protected static final String OTHER_ORDERS_DATASET = "org/openmrs/api/include/OrderServiceTest-otherOrders.xml";
	
	private boolean isOrderActive(Order order, Date asOfDate) {
		return order.isCurrent(asOfDate) && order.getAction() != Action.DISCONTINUE;
	}
	
	/**
	 * @see {@link OrderService#saveOrder(Order)}
	 */
	@Test(expected = APIException.class)
	@Verifies(value = "should not save order if order doesnt validate", method = "saveOrder(Order)")
	public void saveOrder_shouldNotSaveOrderIfOrderDoesntValidate() throws Exception {
		OrderService orderService = Context.getOrderService();
		Order order = new Order();
		order.setPatient(null);
		orderService.saveOrder(order);
	}
	
	/**
	 * @see {@link OrderService#getOrderByUuid(String)}
	 */
	@Test
	@Verifies(value = "should find object given valid uuid", method = "getOrderByUuid(String)")
	public void getOrderByUuid_shouldFindObjectGivenValidUuid() throws Exception {
		String uuid = "921de0a3-05c4-444a-be03-e01b4c4b9142";
		Order order = Context.getOrderService().getOrderByUuid(uuid);
		Assert.assertEquals(1, (int) order.getOrderId());
	}
	
	/**
	 * @see {@link OrderService#getOrderByUuid(String)}
	 */
	@Test
	@Verifies(value = "should return null if no object found with given uuid", method = "getOrderByUuid(String)")
	public void getOrderByUuid_shouldReturnNullIfNoObjectFoundWithGivenUuid() throws Exception {
		Assert.assertNull(Context.getOrderService().getOrderByUuid("some invalid uuid"));
	}
	
	/**
	 * @see {@link OrderService#saveOrder(Order)}
	 */
	@Test
	@Verifies(value = "when saving a discontinuedReasonNonCoded parameter the value is correctly stored to the database", method = "saveOrder(Order)")
	public void saveOrder_shouldSaveDiscontinuedReasonNonCoded() throws Exception {
		String uuid = "921de0a3-05c4-444a-be03-e01b4c4b9142";
		Order order = Context.getOrderService().getOrderByUuid(uuid);
		String discontinuedReasonNonCoded = "Non coded discontinued reason";
		
		order.setDiscontinuedReasonNonCoded(discontinuedReasonNonCoded);
		OrderService orderService = Context.getOrderService();
		orderService.saveOrder(order);
		
		order = Context.getOrderService().getOrderByUuid(uuid);
		
		Assert.assertEquals(discontinuedReasonNonCoded, order.getDiscontinuedReasonNonCoded());
	}
	
	@Test
	public void purgeOrder_shouldDeleteObsThatReference() throws Exception {
		executeDataSet("org/openmrs/api/include/OrderServiceTest-deleteObsThatReference.xml");
		final String ordUuid = "0c96f25c-4949-4f72-9931-d808fbcdb612";
		final String obsUuid = "be3a4d7a-f9ab-47bb-aaad-bc0b452fcda4";
		ObsService os = Context.getObsService();
		OrderService service = Context.getOrderService();
		
		Obs obs = os.getObsByUuid(obsUuid);
		Assert.assertNotNull(obs);
		
		Order order = service.getOrderByUuid(ordUuid);
		Assert.assertNotNull(order);
		
		//sanity check to ensure that the obs and order are actually related
		Assert.assertEquals(order, obs.getOrder());
		
		//Ensure that passing false does not delete the related obs
		service.purgeOrder(order, false);
		Assert.assertNotNull(os.getObsByUuid(obsUuid));
		
		service.purgeOrder(order, true);
		
		//Ensure that actually the order got purged
		Assert.assertNull(service.getOrderByUuid(ordUuid));
		
		//Ensure that the related obs got deleted
		Assert.assertNull(os.getObsByUuid(obsUuid));
		
	}
	
	/**
	 * @see {@link OrderNumberGenerator#getNewOrderNumber()}
	 */
    @Ignore // Test does not terminate
	@Test
	@Verifies(value = "should always return unique orderNumbers when called multiple times without saving orders", method = "getNewOrderNumber()")
	public void getNewOrderNumber_shouldAlwaysReturnUniqueOrderNumbersWhenCalledMultipleTimesWithoutSavingOrders()
	        throws Exception {
		
		executeDataSet("org/openmrs/api/include/OrderServiceTest-globalProperties.xml");
		
		int N = 50;
		final Set<String> uniqueOrderNumbers = new HashSet<String>(50);
		List<Thread> threads = new ArrayList<Thread>();
		for (int i = 0; i < N; i++) {
			threads.add(new Thread(new Runnable() {
				
				@Override
				public void run() {
					try {
						Context.openSession();
						Context.addProxyPrivilege(PrivilegeConstants.ADD_ORDERS);
						uniqueOrderNumbers.add(((OrderNumberGenerator) Context.getOrderService()).getNewOrderNumber());
					}
					finally {
						Context.removeProxyPrivilege(PrivilegeConstants.ADD_ORDERS);
						Context.closeSession();
					}
				}
			}));
		}
		for (int i = 0; i < N; ++i) {
			threads.get(i).start();
		}
		for (int i = 0; i < N; ++i) {
			threads.get(i).join();
		}
		//since we used a set we should have the size as N indicating that there were no duplicates
		Assert.assertEquals(N, uniqueOrderNumbers.size());
	}
	
	/**
	 * @see {@link OrderService#getOrderByOrderNumber(String)}
	 */
	@Test
	@Verifies(value = "should find object given valid order number", method = "getOrderByOrderNumber(String)")
	public void getOrderByOrderNumber_shouldFindObjectGivenValidOrderNumber() throws Exception {
		Order order = Context.getOrderService().getOrderByOrderNumber("1");
		Assert.assertNotNull(order);
		Assert.assertEquals(1, (int) order.getOrderId());
	}
	
	/**
	 * @see {@link OrderService#getOrderByOrderNumber(String)}
	 */
	@Test
	@Verifies(value = "should return null if no object found with given order number", method = "getOrderByOrderNumber(String)")
	public void getOrderByOrderNumber_shouldReturnNullIfNoObjectFoundWithGivenOrderNumber() throws Exception {
		Assert.assertNull(Context.getOrderService().getOrderByOrderNumber("some invalid order number"));
	}
	
	/**
	 * @see {@link OrderService#getOrderHistoryByConcept(Patient,Concept)}
	 */
	@Test
	@Verifies(value = "should return orders with the given concept", method = "getOrderHistoryByConcept(Patient,Concept)")
	public void getOrderHistoryByConcept_shouldReturnOrdersWithTheGivenConcept() throws Exception {
		//We should have two orders with this concept.
		Concept concept = Context.getConceptService().getConcept(88);
		Patient patient = Context.getPatientService().getPatient(2);
		List<Order> orders = Context.getOrderService().getOrderHistoryByConcept(patient, concept);
		
		//They must be sorted by startDate starting with the latest
		Assert.assertEquals(4, orders.size());
		Assert.assertEquals(5, orders.get(0).getOrderId().intValue());
		Assert.assertEquals(444, orders.get(1).getOrderId().intValue());
		Assert.assertEquals(44, orders.get(2).getOrderId().intValue());
		Assert.assertEquals(4, orders.get(3).getOrderId().intValue());
		
		concept = Context.getConceptService().getConcept(792);
		orders = Context.getOrderService().getOrderHistoryByConcept(patient, concept);
		
		//They must be sorted by startDate starting with the latest
		Assert.assertEquals(4, orders.size());
		Assert.assertEquals(3, orders.get(0).getOrderId().intValue());
		Assert.assertEquals(222, orders.get(1).getOrderId().intValue());
		Assert.assertEquals(22, orders.get(2).getOrderId().intValue());
		Assert.assertEquals(2, orders.get(3).getOrderId().intValue());
	}
	
	/**
	 * @see {@link OrderService#getOrderHistoryByConcept(Patient, Concept)}
	 */
	@Test
	@Verifies(value = "should return empty list for concept without orders", method = "getOrderHistoryByConcept(Patient,Concept)")
	public void getOrderHistoryByConcept_shouldReturnEmptyListForConceptWithoutOrders() throws Exception {
		Concept concept = Context.getConceptService().getConcept(21);
		Patient patient = Context.getPatientService().getPatient(2);
		List<Order> orders = Context.getOrderService().getOrderHistoryByConcept(patient, concept);
		Assert.assertEquals(0, orders.size());
	}
	
	/**
	 * @see {@link OrderService#getOrderHistoryByOrderNumber(String)}
	 */
	@Test
	@Verifies(value = "should return all order history for given order number", method = "getOrderHistoryByOrderNumber(String)")
	public void getOrderHistoryByOrderNumber_shouldReturnAllOrderHistoryForGivenOrderNumber() throws Exception {
		List<Order> orders = Context.getOrderService().getOrderHistoryByOrderNumber("111");
		Assert.assertEquals(2, orders.size());
		Assert.assertEquals(111, orders.get(0).getOrderId().intValue());
		Assert.assertEquals(1, orders.get(1).getOrderId().intValue());
	}
	
	/**
	 * @verifies return the order frequency that matched the specified id
	 * @see OrderService#getOrderFrequency(Integer)
	 */
	@Test
	public void getOrderFrequency_shouldReturnTheOrderFrequencyThatMatchedTheSpecifiedId() throws Exception {
		Assert
		        .assertEquals("28090760-7c38-11e3-baa7-0800200c9a66", Context.getOrderService().getOrderFrequency(1)
		                .getUuid());
	}
	
	/**
	 * @verifies return all active orders for the specified patient
	 * @see OrderService#getActiveOrders(org.openmrs.Patient, Class, org.openmrs.CareSetting,
	 *      java.util.Date)
	 */
	@Test
	public void getActiveOrders_shouldReturnAllActiveOrdersForTheSpecifiedPatient() throws Exception {
		Patient patient = Context.getPatientService().getPatient(2);
		List<Order> orders = orderService.getActiveOrders(patient, Order.class, null, null);
		assertEquals(5, orders.size());
		Order[] expectedOrders = { orderService.getOrder(222), orderService.getOrder(3), orderService.getOrder(444),
		        orderService.getOrder(5), orderService.getOrder(7) };
		assertThat(orders, hasItems(expectedOrders));
		
		assertTrue(isOrderActive(orders.get(0), null));
		assertTrue(isOrderActive(orders.get(1), null));
		assertTrue(isOrderActive(orders.get(2), null));
		assertTrue(isOrderActive(orders.get(3), null));
		assertTrue(isOrderActive(orders.get(4), null));
	}
	
	/**
	 * @verifies return all active orders for the specified patient and care setting
	 * @see OrderService#getActiveOrders(org.openmrs.Patient, Class, org.openmrs.CareSetting,
	 *      java.util.Date)
	 */
	@Test
	public void getActiveOrders_shouldReturnAllActiveOrdersForTheSpecifiedPatientAndCareSetting() throws Exception {
		Patient patient = patientService.getPatient(2);
		CareSetting careSetting = orderService.getCareSetting(1);
		List<Order> orders = orderService.getActiveOrders(patient, Order.class, careSetting, null);
		assertEquals(4, orders.size());
		Order[] expectedOrders = { orderService.getOrder(3), orderService.getOrder(444), orderService.getOrder(5),
		        orderService.getOrder(7) };
		assertThat(orders, hasItems(expectedOrders));
	}
	
	/**
	 * @verifies return all active drug orders for the specified patient
	 * @see OrderService#getActiveOrders(org.openmrs.Patient, Class, org.openmrs.CareSetting,
	 *      java.util.Date)
	 */
	@Test
	public void getActiveOrders_shouldReturnAllActiveDrugOrdersForTheSpecifiedPatient() throws Exception {
		Patient patient = patientService.getPatient(2);
		List<DrugOrder> orders = orderService.getActiveOrders(patient, DrugOrder.class, null, null);
		assertEquals(2, orders.size());
		DrugOrder[] expectedOrders = { (DrugOrder) orderService.getOrder(3), (DrugOrder) orderService.getOrder(5) };
		assertThat(orders, hasItems(expectedOrders));
	}
	
	/**
	 * @verifies return all active test orders for the specified patient
	 * @see OrderService#getActiveOrders(org.openmrs.Patient, Class, org.openmrs.CareSetting,
	 *      java.util.Date)
	 */
	@Test
	public void getActiveOrders_shouldReturnAllActiveTestOrdersForTheSpecifiedPatient() throws Exception {
		Patient patient = patientService.getPatient(2);
		List<TestOrder> orders = orderService.getActiveOrders(patient, TestOrder.class, null, null);
		assertEquals(1, orders.size());
		assertEquals(orders.get(0), orderService.getOrder(7));
	}
	
	/**
	 * @verifies fail if patient is null
	 * @see OrderService#getActiveOrders(org.openmrs.Patient, Class, org.openmrs.CareSetting,
	 *      java.util.Date)
	 */
	@Test(expected = IllegalArgumentException.class)
	public void getActiveOrders_shouldFailIfPatientIsNull() throws Exception {
		orderService.getActiveOrders(null, Order.class, orderService.getCareSetting(1), null);
	}
	
	/**
	 * @verifies return active orders as of the specified date
	 * @see OrderService#getActiveOrders(org.openmrs.Patient, Class, org.openmrs.CareSetting,
	 *      java.util.Date)
	 */
	@Test
	public void getActiveOrders_shouldReturnActiveOrdersAsOfTheSpecifiedDate() throws Exception {
		Patient patient = Context.getPatientService().getPatient(2);
		Date asOfDate = Context.getDateTimeFormat().parse("02/12/2007 23:59:59");
		List<Order> orders = orderService.getActiveOrders(patient, Order.class, null, asOfDate);
		assertEquals(0, orders.size());
		
		asOfDate = Context.getDateFormat().parse("03/12/2007");
		orders = orderService.getActiveOrders(patient, Order.class, null, asOfDate);
		assertEquals(1, orders.size());
		assertEquals(orderService.getOrder(2), orders.get(0));
		
		asOfDate = Context.getDateFormat().parse("10/12/2007");
		orders = orderService.getActiveOrders(patient, Order.class, null, asOfDate);
		assertEquals(1, orders.size());
		assertEquals(orderService.getOrder(2), orders.get(0));
		
		asOfDate = Context.getDateTimeFormat().parse("10/12/2007 00:01:00");
		orders = orderService.getActiveOrders(patient, Order.class, null, asOfDate);
		assertEquals(0, orders.size());
		
		asOfDate = Context.getDateFormat().parse("09/04/2008");
		orders = orderService.getActiveOrders(patient, Order.class, null, asOfDate);
		assertEquals(3, orders.size());
		Order[] expectedOrders = { orderService.getOrder(222), orderService.getOrder(3), orderService.getOrder(4) };
		assertThat(orders, hasItems(expectedOrders));
		
		asOfDate = Context.getDateFormat().parse("25/09/2008");
		orders = orderService.getActiveOrders(patient, Order.class, null, asOfDate);
		assertEquals(5, orders.size());
		Order[] expectedOrders1 = { orderService.getOrder(222), orderService.getOrder(3), orderService.getOrder(444),
		        orderService.getOrder(5), orderService.getOrder(6) };
		assertThat(orders, hasItems(expectedOrders1));
		
		asOfDate = Context.getDateTimeFormat().parse("26/09/2008 10:24:10");
		orders = orderService.getActiveOrders(patient, Order.class, null, asOfDate);
		assertEquals(4, orders.size());
		Order[] expectedOrders2 = { orderService.getOrder(222), orderService.getOrder(3), orderService.getOrder(444),
		        orderService.getOrder(5) };
		assertThat(orders, hasItems(expectedOrders2));
		
		asOfDate = Context.getDateFormat().parse("20/11/2008");
		orders = orderService.getActiveOrders(patient, Order.class, null, asOfDate);
		assertEquals(5, orders.size());
		Order[] expectedOrders3 = { orderService.getOrder(222), orderService.getOrder(3), orderService.getOrder(444),
		        orderService.getOrder(5), orderService.getOrder(7) };
		assertThat(orders, hasItems(expectedOrders3));
		
		asOfDate = Context.getDateFormat().parse("02/12/2008");
		orders = orderService.getActiveOrders(patient, Order.class, null, asOfDate);
		assertEquals(6, orders.size());
		Order[] expectedOrders4 = { orderService.getOrder(222), orderService.getOrder(3), orderService.getOrder(444),
		        orderService.getOrder(5), orderService.getOrder(7), orderService.getOrder(9) };
		assertThat(orders, hasItems(expectedOrders4));
		
		asOfDate = Context.getDateFormat().parse("04/12/2008");
		orders = orderService.getActiveOrders(patient, Order.class, null, asOfDate);
		for (Order o : orders) {
			System.out.println(o.getOrderId());
		}
		assertEquals(5, orders.size());
		Order[] expectedOrders5 = { orderService.getOrder(222), orderService.getOrder(3), orderService.getOrder(444),
		        orderService.getOrder(5), orderService.getOrder(7) };
		assertThat(orders, hasItems(expectedOrders5));
	}
}
