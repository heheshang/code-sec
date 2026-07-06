package com.codesec.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "repo_branch")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepoBranchEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repo_id", nullable = false)
    private Long repoId;

    @Column(nullable = false, length = 256)
    private String name;

    @Column(name = "last_commit_sha", length = 64)
    private String lastCommitSha;

    @Column(name = "last_scanned_at")
    private LocalDateTime lastScannedAt;

    @Column(nullable = false, length = 20)
    private String status = "active";
}
