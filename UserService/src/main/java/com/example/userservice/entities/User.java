package com.example.userservice.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;
import java.util.List;
import java.time.LocalDate;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "surname")
    private String surname;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "email")
    private String email;

    @OneToMany(mappedBy = "user")
    private List<CardInfo> cardInfos;

    public User(String name, String surname, LocalDate birthDate, String email) {
        this.name = name;
        this.surname = surname;
        this.birthDate = birthDate;
        this.email = email;
    }
}
