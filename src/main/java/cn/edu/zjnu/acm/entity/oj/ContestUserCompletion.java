package cn.edu.zjnu.acm.entity.oj;

import cn.edu.zjnu.acm.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name = "contest_user_completion", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"contest_id", "user_id"})
})
public class ContestUserCompletion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "contest_id", nullable = false)
    @JsonIgnore
    private Contest contest;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private Instant completedAt;

    public ContestUserCompletion() {
        this.completedAt = Instant.now();
    }

    public ContestUserCompletion(Contest contest, User user) {
        this.contest = contest;
        this.user = user;
        this.completedAt = Instant.now();
    }
}
