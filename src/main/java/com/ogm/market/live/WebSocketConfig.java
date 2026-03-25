package com.ogm.market.live;

import com.ogm.market.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/live-queue")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * STOMP auth interceptor — validates JWT on CONNECT and SUBSCRIBE.
     *
     * Rules:
     *  - CONNECT: optional JWT in header. If present and valid, sets principal.
     *  - SUBSCRIBE to /topic/agent/{id}/** : requires valid JWT
     *  - SUBSCRIBE to /topic/queue/** and /topic/availability/**: public (customers)
     *  - All other commands: pass through
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null) return message;

                // ── CONNECT: extract JWT if present, set principal ──────────
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        if (jwtUtil.isTokenValid(token)) {
                            String email = jwtUtil.extractEmail(token);
                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(email, null, List.of());
                            accessor.setUser(auth);
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    }
                    // Customers (no token) connect without principal — that's OK
                    return message;
                }

                // ── SUBSCRIBE: protect agent topics ────────────────────────
                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String dest = accessor.getDestination();
                    if (dest != null && dest.startsWith("/topic/agent/")) {
                        // Agent topics require authenticated principal
                        if (accessor.getUser() == null) {
                            // Return null to silently drop the subscription
                            // (throwing would disconnect the entire STOMP session)
                            return null;
                        }
                    }
                    // /topic/queue/* and /topic/availability/* are public — customers use these
                    return message;
                }

                return message;
            }
        });
    }
}