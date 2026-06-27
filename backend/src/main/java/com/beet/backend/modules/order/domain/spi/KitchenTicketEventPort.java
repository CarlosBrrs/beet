package com.beet.backend.modules.order.domain.spi;

import com.beet.backend.modules.order.domain.model.KitchenTicketDomain;

public interface KitchenTicketEventPort {
    void ticketCreated(KitchenTicketDomain ticket);

    void ticketUpdated(KitchenTicketDomain ticket);

    void ticketStatusChanged(KitchenTicketDomain ticket);
}