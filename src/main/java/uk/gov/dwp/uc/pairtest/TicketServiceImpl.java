package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.Objects;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */
    private static final int MAX_TICKETS = 25; //remember to externalise this in the properties file
    private static final int CHILD_PRICE = 15;
    private static final int ADULT_PRICE = 25;
    private static final int INFANT_PRICE = 0;

    private final TicketPaymentService paymentService;
    private final SeatReservationService reservationService;

    public TicketServiceImpl(TicketPaymentService paymentService, SeatReservationService reservationService) {
        this.paymentService = paymentService;
        this.reservationService = reservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        var ticketCounts = validatePurchaseRequest(accountId, ticketTypeRequests);

        var totalAmount = calculateTotalAmount(ticketCounts);
        var seatsToReserve = calculateSeatsToReserve(ticketCounts);

        paymentService.makePayment(accountId, totalAmount);
        reservationService.reserveSeat(accountId, seatsToReserve);
    }

    private TicketCounts validatePurchaseRequest(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        if (Objects.isNull(accountId) || accountId <= 0) throw new InvalidPurchaseException("Invalid account ID");

        if (Objects.isNull(ticketTypeRequests) || ticketTypeRequests.length == 0)
            throw new InvalidPurchaseException("No tickets requested");

        var ticketCounts = aggregateTicketCounts(ticketTypeRequests);

        if (ticketCounts.totalTickets > MAX_TICKETS)
            throw new InvalidPurchaseException(String.format("Maximum %d tickets per purchase", MAX_TICKETS));

        if (ticketCounts.adultTickets == 0 && (ticketCounts.childTickets > 0 || ticketCounts.infantTickets > 0))
            throw new InvalidPurchaseException("Child and Infant tickets require at least one Adult ticket.");

        if (ticketCounts.infantTickets > ticketCounts.adultTickets)
            throw new InvalidPurchaseException("Each Infant must be accompanied by one Adult.");

        return ticketCounts;
    }

    private TicketCounts aggregateTicketCounts(TicketTypeRequest... ticketTypeRequests) {
        int totalTickets = 0, adultTickets = 0, childTickets = 0, infantTickets = 0;

        for (var request : ticketTypeRequests) {
            if (request.getNoOfTickets() < 0) throw new InvalidPurchaseException("Invalid number of tickets");

            totalTickets += request.getNoOfTickets();
            switch (request.getTicketType()) {
                case ADULT -> adultTickets += request.getNoOfTickets();
                case CHILD -> childTickets += request.getNoOfTickets();
                case INFANT -> infantTickets += request.getNoOfTickets();
            }
        }

        return new TicketCounts(adultTickets, childTickets, infantTickets, totalTickets);
    }

    private int calculateTotalAmount(TicketCounts ticketCounts) {
        return (ticketCounts.adultTickets * ADULT_PRICE)
                + (ticketCounts.childTickets * CHILD_PRICE)
                + (ticketCounts.infantTickets * INFANT_PRICE);
    }

    private int calculateSeatsToReserve(TicketCounts ticketCounts) {
        return ticketCounts.adultTickets + ticketCounts.childTickets;
    }

    private record TicketCounts(int adultTickets, int childTickets, int infantTickets, int totalTickets) {}

}
