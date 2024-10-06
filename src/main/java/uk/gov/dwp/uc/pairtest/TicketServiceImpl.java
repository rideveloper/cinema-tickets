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

    private final TicketPaymentService paymentService;
    private final SeatReservationService reservationService;

    public TicketServiceImpl(TicketPaymentService paymentService, SeatReservationService reservationService) {
        this.paymentService = paymentService;
        this.reservationService = reservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        validatePurchaseRequest(accountId, ticketTypeRequests);

        int totalAmount = calculateTotalAmount(ticketTypeRequests);
        int seatsToReserve = calculateSeatsToReserve(ticketTypeRequests);

        paymentService.makePayment(accountId, totalAmount);
        reservationService.reserveSeat(accountId, seatsToReserve);
    }

    private void validatePurchaseRequest(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        if (accountId <= 0) throw new InvalidPurchaseException("Invalid account ID");

        if (Objects.isNull(ticketTypeRequests) || ticketTypeRequests.length == 0)
            throw new InvalidPurchaseException("No tickets requested");

        int totalTickets = 0, adultTickets = 0, childTickets = 0, infantTickets = 0;

        for (TicketTypeRequest request : ticketTypeRequests) {
            if (request.getNoOfTickets() < 0) throw new InvalidPurchaseException("Invalid number of tickets");

            totalTickets += request.getNoOfTickets();
            switch (request.getTicketType()) {
                case ADULT -> adultTickets += request.getNoOfTickets();
                case CHILD -> childTickets += request.getNoOfTickets();
                case INFANT -> infantTickets += request.getNoOfTickets();
            }
        }

        if (totalTickets > MAX_TICKETS)
            throw new InvalidPurchaseException(String.format("Maximum %d tickets per purchase", MAX_TICKETS));

        if (adultTickets == 0 && (childTickets > 0 || infantTickets > 0))
            throw new InvalidPurchaseException("Child and Infant tickets require at least one Adult ticket.");

        if (infantTickets > adultTickets)
            throw new InvalidPurchaseException("Each Infant must be accompanied by one Adult.");
    }

    private int calculateTotalAmount(TicketTypeRequest... ticketTypeRequests) {
        int totalAmount = 0;
        for (TicketTypeRequest request : ticketTypeRequests) {
            switch (request.getTicketType()) {
                case ADULT -> totalAmount += request.getNoOfTickets() * ADULT_PRICE;
                case CHILD -> totalAmount += request.getNoOfTickets() * CHILD_PRICE;

                // Infants are free, so no need to add to the total
            }
        }

        return totalAmount;
    }

    private int calculateSeatsToReserve(TicketTypeRequest... ticketTypeRequests) {
        int seatsToReserve = 0;
        for (TicketTypeRequest request : ticketTypeRequests) {
            if (request.getTicketType() != TicketTypeRequest.Type.INFANT) {
                seatsToReserve += request.getNoOfTickets();
            }
        }

        return seatsToReserve;
    }

}
