package cvf.ids.system.api.connector;

import cvf.ids.system.api.statemachine.ContractNegotiation;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static cvf.ids.system.api.statemachine.ContractNegotiation.State.CONSUMER_REQUESTED;
import static cvf.ids.system.api.statemachine.ContractNegotiation.State.TERMINATED;

/**
 * Manages contract negotiations on a consumer.
 */
public class ConsumerNegotiationManager {

    private Map<String, ContractNegotiation> negotiations = new ConcurrentHashMap<>();

    private Queue<ConsumerNegotiationListener> listeners = new ConcurrentLinkedQueue<>();

    public void consumerRequested(String negotiationId, String correlationId) {
        var contractNegotiation = getNegotiations().get(negotiationId);
        contractNegotiation.setCorrelationId(correlationId);
        contractNegotiation.transition(CONSUMER_REQUESTED);
    }

    public void consumerCounterRequested(String negotiationId) {
        var contractNegotiation = getNegotiations().get(negotiationId);
        contractNegotiation.transition(CONSUMER_REQUESTED);
    }

    public void terminate(String id) {
        var negotiation = getNegotiations().get(id);
        negotiation.transition(TERMINATED);
        listeners.forEach(l -> l.terminated(negotiation));
    }

    public ContractNegotiation createNegotiation(String datasetId) {
        var negotiation = ContractNegotiation.Builder.newInstance().datasetId(datasetId).build();
        negotiations.put(negotiation.getId(), negotiation);

        listeners.forEach(l -> l.negotiationCreated(negotiation));

        return negotiation;
    }

    @NotNull
    public ContractNegotiation findByCorrelationId(String id) {
        return negotiations.values().stream()
                .filter(n -> id.equals(n.getCorrelationId()))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Negotiation not found for correlation id: " + id));
    }

    public Map<String, ContractNegotiation> getNegotiations() {
        return negotiations;
    }

    public void registerListener(ConsumerNegotiationListener listener) {
        listeners.add(listener);
    }

    public void deregisterListener(ConsumerNegotiationListener listener) {
        listeners.remove(listener);
    }
}