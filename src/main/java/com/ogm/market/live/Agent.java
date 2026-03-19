package com.ogm.market.live;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agent")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password; // store BCrypt hash in production

    private String phone;

    private String photoUrl;

    private String designation; // e.g. "Senior Property Consultant"
}