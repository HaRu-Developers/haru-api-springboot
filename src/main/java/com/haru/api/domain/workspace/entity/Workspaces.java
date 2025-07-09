package com.haru.api.domain.workspace.entity;

import com.haru.api.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Getter
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Workspaces extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @Builder
    public Workspaces(String title, String imageUrl) {
        this.title = title;
        this.imageUrl = imageUrl;
    }
}
