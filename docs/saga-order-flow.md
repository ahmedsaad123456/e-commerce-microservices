# Order saga (choreography)

```mermaid
sequenceDiagram
    participant C as Client
    participant O as Order
    participant P as Product
    participant Pay as Payment
    participant N as Notification
    C->>O: POST /orders
    O->>P: purchase (HTTP): stock deducted
    O->>O: save order PENDING + outbox row (one transaction)
    O-->>Pay: order-topic: OrderConfirmation
    O-->>N: order-topic: OrderConfirmation (email "order received")
    Pay->>Pay: claim order + charge
    alt payment succeeded
        Pay-->>O: payment-topic: PaymentResult SUCCEEDED
        O->>O: PENDING -> CONFIRMED
    else payment declined
        Pay-->>O: payment-topic: PaymentResult FAILED
        O->>O: PENDING -> CANCELLED
        Pay-->>P: payment-topic: PaymentResult FAILED
        P->>P: restore stock (once)
    end
    Pay-->>N: payment-topic: PaymentResult (email)
```

## Temporary states
| Moment | What is true | Who can observe it |
|---|---|---|
| After POST /orders | Order PENDING, stock already deducted | Client (GET /orders/{id}) |
| Payment done, result not consumed yet | Payment SUCCEEDED, Order still PENDING | Anyone reading both services |
| Payment declined, compensation pending | Order PENDING (or CANCELLED), stock still low | Product catalog readers |
| Payment service down | Order stays PENDING, events wait in Kafka | Client |

## Rules
- Every step is a local transaction + an outbox event. No distributed transaction.
- Every consumer is idempotent: events are delivered at-least-once.
- Order state machine: PENDING -> CONFIRMED | CANCELLED. Nothing else.