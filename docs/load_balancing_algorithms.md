### Core Load Balancing Algorithms:

-   **Round Robin:** Distributes requests sequentially across all
    available servers in a circular order.
    -   **Pros:**
        -   Simple and easy to implement.
        -   Distributes requests evenly across available servers.
        -   Does not require monitoring server load.
    -   **Cons:**
        -   Does not consider server capacity or current workload.
        -   May overload slower or already-busy servers.
        -   Not ideal when requests have significantly different
            processing times.
-   **Sticky Round Robin:** Works like Round Robin, but keeps a client
    assigned to the same server after the first request. For example, if
    Client A is assigned to Server 1, their subsequent requests continue
    going to Server 1 as long as the session mapping remains valid.
    -   **Pros:**
        -   Maintains session persistence (sticky sessions).
        -   Reduces the need to share session data between servers when
            sessions are stored locally.
        -   Combines Round Robin distribution for new clients with
            session affinity for returning clients.
    -   **Cons:**
        -   Can lead to uneven load distribution because some servers
            may receive more active users.
        -   If a server fails, clients assigned to it may lose their
            sessions.
        -   Scaling servers up or down can change client-to-server
            assignments.
        -   Less suitable when sessions are stored centrally, such as in
            Redis, because stickiness may not be necessary.
-   **Weighted Round Robin:** Assigns a weight to each server based on
    capacity; servers with higher weights receive more traffic.
    -   **Pros:**
        -   Distributes traffic according to server capacity.
        -   Allows powerful servers to handle more requests.
        -   Easy to configure using predefined weights.
    -   **Cons:**
        -   Does not consider real-time server load.
        -   Requires manual weight configuration and adjustment.
        -   May overload a server if its workload suddenly increases.
-   **Least Connections:** Directs new traffic to the server with the
    fewest active connections, helping prevent any single server from
    becoming a bottleneck.
    -   **Pros:**
        -   Handles requests with different processing times better than
            Round Robin.
        -   Adapts to changing numbers of active connections.
        -   Can prevent traffic from being sent to a server with many
            ongoing connections.
    -   **Cons:**
        -   Active connections do not always reflect actual server
            workload.
        -   Requires tracking active connections.
        -   May not work well when connections have very different
            resource requirements.
-   **IP Hash:** Uses the client's IP address to determine which server
    receives the request, helping ensure session persistence (sticky
    sessions).
    -   **Pros:**
        -   Routes clients with the same IP address to the same server
            in typical configurations.
        -   Can maintain session persistence without a separate session
            lookup.
        -   Useful when application sessions are stored locally on each
            server.
    -   **Cons:**
        -   Can cause uneven load distribution.
        -   Client IP changes may result in a different server.
        -   Multiple clients behind the same NAT or proxy may be routed
            to the same server.
        -   Server changes or failures can disrupt session persistence.
-   **Least Response Time:** Routes traffic to the server that provides
    the fastest response, often considering response time together with
    the number of active connections. The exact calculation depends on
    the load balancer implementation.
    -   **Pros:**
        -   Prioritizes servers that are responding faster.
        -   Helps reduce request latency.
        -   Can adapt to changes in server performance.
    -   **Cons:**
        -   Requires continuous response-time monitoring.
        -   Response times can fluctuate due to network conditions.
        -   Monitoring and calculation add complexity.
        -   May send more traffic to the fastest server, increasing its
            workload.
