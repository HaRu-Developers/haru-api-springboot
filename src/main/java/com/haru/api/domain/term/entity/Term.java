package com.haru.api.domain.term.entity;

import com.haru.api.domain.term.entity.enums.TermType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Term {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private TermType type;

    @Column(length = 30)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
}
