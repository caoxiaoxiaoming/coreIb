package com.coreib.kernel;

@FunctionalInterface
public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
