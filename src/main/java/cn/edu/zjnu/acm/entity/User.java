package cn.edu.zjnu.acm.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.time.Instant;

@Data
@Entity
@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User implements Cloneable, Comparable {
    @Column(nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    Instant createtime = Instant.now();
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, updatable = false, length = 30)
    @NotEmpty
    @Size(min = 6, max = 35)
    private String username;
    @Column(nullable = false, length = 60)
    @NotEmpty
    @Size(min = 6, max = 60)
    private String password;
    @Column(nullable = false, columnDefinition = "VARCHAR(250) default ''")
    @NotEmpty(message = "姓名不能为空")
    @Size(min = 1, max = 250, message = "姓名长度必须在1-250个字符之间")
    private String name;
    @Column(nullable = false, columnDefinition = "VARCHAR(250) default ''")
    @Size(min = 4, max = 250, message = "邮箱长度必须在4-250个字符之间")
    @Email(message = "邮箱格式不正确")
    private String email;
    @Column(nullable = false, columnDefinition = "VARCHAR(250) default ''")
    @Size(max = 250, message = "个人简介长度不能超过250个字符")
    private String intro;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "user")
    private UserProfile userProfile;

    public User() {
        this.username = "";
        this.password = "";
        this.email = "";
        this.name = "";
        this.intro = "";
    }

    public User(@NotEmpty @Size(min = 6, max = 30) String username, @NotEmpty @Size(min = 6, max = 30) String password, @NotEmpty @Size(min = 1, max = 30) String name, @Size(min = 4, max = 200) String email, @Size(max = 250) String intro) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.email = email;
        this.intro = intro;
    }

    public User clone() throws CloneNotSupportedException {
        return (User) super.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        return getId().equals(user.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public int compareTo(Object o) {
        return getId().compareTo(((User) o).getId());
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", intro='" + intro + '\'' +
                '}';
    }

    public User hideInfo() {
        setPassword(null);
        setEmail(null);
        setIntro(null);
        setCreatetime(null);
        return this;
    }
}
