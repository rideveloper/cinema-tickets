package uk.gov.dwp.uc.pairtest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TicketServiceImplTest {
    @Mock
    private TicketPaymentService paymentService;
    @Mock
    private SeatReservationService reservationService;
    private TicketService ticketService;

    @Before
    public void setUp() {
        ticketService = new TicketServiceImpl(paymentService, reservationService);
    }

    @Test
    public void purchaseTickets_validPurchase_success() {
        TicketTypeRequest adultTicket = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest childTicket = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);

        ticketService.purchaseTickets(1L, adultTicket, childTicket);

        verify(paymentService).makePayment(1L, 65); // 2 * 25 + 1 * 15
        verify(reservationService).reserveSeat(1L, 3);
    }

    @Test
    public void purchaseTickets_withInfant_success() {
        TicketTypeRequest adultTicket = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest infantTicket = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        ticketService.purchaseTickets(1L, adultTicket, infantTicket);

        verify(paymentService).makePayment(1L, 50); // 2 * 25
        verify(reservationService).reserveSeat(1L, 2); // Infants don't need seats
    }

    @Test(expected = InvalidPurchaseException.class)
    public void purchaseTickets_invalidAccountId_throwsException() {
        TicketTypeRequest adultTicket = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        ticketService.purchaseTickets(0L, adultTicket);
    }

    @Test(expected = InvalidPurchaseException.class)
    public void purchaseTickets_noTickets_throwsException() {
        ticketService.purchaseTickets(1L);
    }

    @Test
    public void purchaseTickets_exceedMaxTickets_throwsException() {
        TicketTypeRequest adultTicket = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 25);
        TicketTypeRequest infantTicket = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);

        try {
            ticketService.purchaseTickets(1L, adultTicket, infantTicket);
            fail("Expected InvalidPurchaseException was not thrown");
        } catch (InvalidPurchaseException e) {
            assertTrue(e.getMessage().contains("Maximum 25 tickets"));
        }
    }

    @Test(expected = InvalidPurchaseException.class)
    public void purchaseTickets_noAdultTicket_throwsException() {
        TicketTypeRequest childTicket = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        ticketService.purchaseTickets(1L, childTicket);
    }

    @Test(expected = InvalidPurchaseException.class)
    public void purchaseTickets_moreInfantsThanAdults_throwsException() {
        TicketTypeRequest adultTicket = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest infantTicket = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);
        ticketService.purchaseTickets(1L, adultTicket, infantTicket);
    }

    @Test(expected = InvalidPurchaseException.class)
    public void purchaseTickets_negativeTicketCount_throwsException() {
        TicketTypeRequest adultTicket = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, -1);
        ticketService.purchaseTickets(1L, adultTicket);
    }

    @Test
    public void purchaseTickets_maxTickets_success() {
        TicketTypeRequest adultTicket = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 25);

        ticketService.purchaseTickets(1L, adultTicket);

        verify(paymentService).makePayment(1L, 625); // 25 * 25
        verify(reservationService).reserveSeat(1L, 25);
    }
}
