# Build
``` ./gradlew build ```

# Run Servers
``` ./gradlew cloud --args="10" --console=plain ```

# Run Clients
``` ./gradlew client --args="10" --console=plain ```

# Deactivate Server
``` sudo iptables -A INPUT -p tcp --dport 8000 -j DROP ```

# Reactivate Server
``` sudo iptables -F ```