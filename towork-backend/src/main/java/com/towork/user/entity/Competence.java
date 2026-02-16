package com.towork.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.towork.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "competences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler" })
public class Competence extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freelancer_id", nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Freelancer freelancer;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "level", nullable = false)
    private String level; // BEGINNER, INTERMEDIATE, ADVANCED, EXPERT

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "is_certified", nullable = false)
    private Boolean isCertified = false;

    @Column(name = "certification_name")
    private String certificationName;

    @Column(name = "certification_date")
    private java.time.LocalDate certificationDate;
}
