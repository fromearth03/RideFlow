package com.cwtw.rideflow.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String licenseNumber;

    @Column(nullable = false)
    private boolean isAvailable;

    @Builder.Default
    @Column(nullable = false)
    private boolean approved = false;

    @Builder.Default
    @OneToMany(mappedBy = "driver", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Vehicle> vehicles = new ArrayList<>();
}
