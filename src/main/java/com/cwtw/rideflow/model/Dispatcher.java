package com.cwtw.rideflow.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dispatchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispatcher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Builder.Default
    @Column(nullable = false)
    private boolean approved = false;
}
